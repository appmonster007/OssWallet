package com.osscabinet.ossWallet.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig
import com.osscabinet.ossWallet.domain.model.settings.NetWorthCardConfig
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.aggregation.CardAggregationService
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal

/**
 * One-shot expedited worker that fetches net worth data and writes it to each widget's
 * GlanceState (PreferencesGlanceStateDefinition). The running Glance session reacts to the
 * state change and recomposes immediately — no new SessionWorker is needed.
 *
 * Previously, provideGlance() performed async DB work before provideContent(). That held the
 * Glance SessionWorker open for the entire IO duration (~47s observed). While the session was
 * occupied, subsequent updateAll() calls targeting the same unique work name were dropped by
 * WorkManager's KEEP policy. Rapid transaction saves (e.g. -1, -4, -3) would skip intermediate
 * values because the session for the first update was still running when the next trigger fired.
 *
 * Now provideGlance() only calls provideContent { currentState() } — fully synchronous — so
 * the session is always ready to receive state updates. Every updateAppWidgetState() write in
 * this worker causes an immediate recomposition of the already-running session, regardless of
 * how many updates arrive in rapid succession.
 */
class WidgetRefreshWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            withContext(NonCancellable) {
                val context = applicationContext
                val widgetManager = GlanceAppWidgetManager(context)
                val glanceIds = widgetManager.getGlanceIds(NetWorthGlanceWidget::class.java)

                if (glanceIds.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        val common = WidgetDataLoader.loadCommon(context)
                        for (glanceId in glanceIds) {
                            val widgetId = runCatching {
                                widgetManager.getAppWidgetId(glanceId)
                            }.getOrNull() ?: continue

                            val config = runCatching {
                                NetWorthWidgetPrefsStore(context).load(widgetId)
                            }.getOrElse { NetWorthWidgetConfig() }

                            val renderData = runCatching {
                                CardAggregationService.computeNetWorthCard(
                                    queryService = common.databaseQueryService,
                                    profileId = common.profileId,
                                    input = CardInput(
                                        profileId = common.profileId,
                                        baseCurrency = common.baseCurrency,
                                        exchangeRates = common.exchangeRates,
                                        precision = common.precision,
                                        filter = TransactionFilter(
                                            timelineRange = config.timelineRange,
                                            excludeFuture = true
                                        ),
                                        accounts = emptyList(),
                                        categories = emptyList()
                                    ),
                                    config = NetWorthCardConfig(
                                        cardId = "nw_widget_$widgetId",
                                        fontSize = config.fontSize,
                                        showComparison = config.showComparison
                                    )
                                )
                            }.getOrNull()

                            val netWorth = renderData?.currentMetrics?.netWorth ?: BigDecimal.ZERO
                            val income = renderData?.currentMetrics?.cashFlow?.totalIncomeInBase ?: BigDecimal.ZERO
                            val expense = renderData?.currentMetrics?.cashFlow?.totalExpensesInBase ?: BigDecimal.ZERO
                            val prev = renderData?.previousMetrics

                            updateAppWidgetState(
                                context,
                                PreferencesGlanceStateDefinition,
                                glanceId
                            ) { currentPrefs ->
                                currentPrefs.toMutablePreferences().apply {
                                    this[NetWorthGlanceWidget.Keys.IS_LOADED] = "true"
                                    this[NetWorthGlanceWidget.Keys.NET_WORTH] =
                                        formatCurrencyRight(netWorth, common.baseCurrency, common.precision)
                                    this[NetWorthGlanceWidget.Keys.INCOME] =
                                        formatCurrencyRight(income, common.baseCurrency, common.precision)
                                    this[NetWorthGlanceWidget.Keys.EXPENSE] =
                                        formatCurrencyRight(expense, common.baseCurrency, common.precision)
                                    this[NetWorthGlanceWidget.Keys.NET_WORTH_COMPARISON] =
                                        buildComparisonText(netWorth, prev?.netWorth, config.showComparison) ?: ""
                                    this[NetWorthGlanceWidget.Keys.INCOME_COMPARISON] =
                                        buildComparisonText(income, prev?.cashFlow?.totalIncomeInBase, config.showComparison) ?: ""
                                    this[NetWorthGlanceWidget.Keys.EXPENSE_COMPARISON] =
                                        buildComparisonText(expense, prev?.cashFlow?.totalExpensesInBase, config.showComparison) ?: ""
                                    this[NetWorthGlanceWidget.Keys.PERIOD_LABEL] = config.timelineLabel
                                    this[NetWorthGlanceWidget.Keys.FONT_SIZE] = config.fontSize
                                }
                            }
                        }
                    }
                    // Trigger recomposition — provideGlance() now only reads currentState(),
                    // so the session completes in microseconds and is never occupied.
                    NetWorthGlanceWidget().updateAll(context)
                }

                // ---- Cash Flow widget refresh ----
                val cashFlowIds = widgetManager.getGlanceIds(CashFlowGlanceWidget::class.java)
                if (cashFlowIds.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        val common = WidgetDataLoader.loadCommon(context)
                        for (glanceId in cashFlowIds) {
                            val widgetId = runCatching {
                                widgetManager.getAppWidgetId(glanceId)
                            }.getOrNull() ?: continue

                            val config = runCatching {
                                CashFlowWidgetPrefsStore(context).load(widgetId)
                            }.getOrElse { CashFlowWidgetConfig() }

                            val renderData = runCatching {
                                CardAggregationService.computeCashFlowCard(
                                    queryService = common.databaseQueryService,
                                    profileId = common.profileId,
                                    input = CardInput(
                                        profileId = common.profileId,
                                        baseCurrency = common.baseCurrency,
                                        exchangeRates = common.exchangeRates,
                                        precision = common.precision,
                                        filter = TransactionFilter(
                                            timelineRange = config.timelineRange,
                                            excludeFuture = true
                                        ),
                                        accounts = emptyList(),
                                        categories = emptyList()
                                    ),
                                    config = CashFlowCardConfig(
                                        cardId = "cf_widget_$widgetId",
                                        filter = TransactionFilter(
                                            timelineRange = config.timelineRange,
                                            excludeFuture = true
                                        )
                                    )
                                )
                            }.getOrNull()

                            val result = renderData?.result
                            val incomeInBase = result?.totalIncomeInBase ?: BigDecimal.ZERO
                            val expenseInBase = result?.totalExpensesInBase ?: BigDecimal.ZERO
                            val netInBase = result?.netInBase ?: BigDecimal.ZERO

                            val incomeFormatted = formatCurrencyRight(incomeInBase, common.baseCurrency, common.precision)
                            val expenseFormatted = formatCurrencyRight(expenseInBase, common.baseCurrency, common.precision)

                            // Comparison: net flow vs previous period (computed on demand)
                            val prevNetInBase: BigDecimal? = if (config.showComparison) {
                                val currentFilter = TransactionFilter(
                                    timelineRange = config.timelineRange,
                                    excludeFuture = true
                                )
                                val (prevStart, prevEnd) = TimelineService.getPreviousRange(currentFilter.timelineRange)
                                if (prevStart != null && prevEnd != null) {
                                    val prevFilter = currentFilter.copy(
                                        timelineRange = TimelineRange.CustomRange(prevStart, prevEnd)
                                    )
                                    runCatching {
                                        CardAggregationService.computeCashFlowCard(
                                            queryService = common.databaseQueryService,
                                            profileId = common.profileId,
                                            input = CardInput(
                                                profileId = common.profileId,
                                                baseCurrency = common.baseCurrency,
                                                exchangeRates = common.exchangeRates,
                                                precision = common.precision,
                                                filter = prevFilter,
                                                accounts = emptyList(),
                                                categories = emptyList()
                                            ),
                                            config = CashFlowCardConfig(cardId = "cf_widget_prev_$widgetId")
                                        )
                                    }.getOrNull()?.result?.netInBase
                                } else null
                            } else null
                            val comparisonText = buildComparisonText(
                                netInBase, prevNetInBase, config.showComparison
                            ) ?: ""

                            // Per-currency breakdown: income + expense amounts per currency with base conversion
                            val currencyJson = JSONArray().also { arr ->
                                result?.breakdown?.forEach { item ->
                                    arr.put(
                                        JSONObject().apply {
                                            put("code", item.currency)
                                            put("income", formatCurrencyRight(item.income, item.currency, common.precision))
                                            put("incomeBase", formatCurrencyRight(item.incomeInBase, common.baseCurrency, common.precision))
                                            put("expense", formatCurrencyRight(item.expense, item.currency, common.precision))
                                            put("expenseBase", formatCurrencyRight(item.expenseInBase, common.baseCurrency, common.precision))
                                        }
                                    )
                                }
                            }.toString()

                            updateAppWidgetState(
                                context,
                                PreferencesGlanceStateDefinition,
                                glanceId
                            ) { currentPrefs ->
                                currentPrefs.toMutablePreferences().apply {
                                    this[CashFlowGlanceWidget.Keys.IS_LOADED] = "true"
                                    this[CashFlowGlanceWidget.Keys.INCOME_TOTAL] = incomeFormatted
                                    this[CashFlowGlanceWidget.Keys.EXPENSE_TOTAL] = expenseFormatted
                                    this[CashFlowGlanceWidget.Keys.PERIOD_LABEL] = config.timelineLabel
                                    this[CashFlowGlanceWidget.Keys.BASE_CURRENCY] = common.baseCurrency
                                    this[CashFlowGlanceWidget.Keys.SHOW_BASE_CONVERSION] = if (config.showBaseConversion) "true" else "false"
                                    this[CashFlowGlanceWidget.Keys.SHOW_TOTALS] = if (config.showTotals) "true" else "false"
                                    this[CashFlowGlanceWidget.Keys.CURRENCY_LIST] = currencyJson
                                    this[CashFlowGlanceWidget.Keys.COMPARISON] = comparisonText
                                }
                            }
                        }
                    }
                    CashFlowGlanceWidget().updateAll(context)
                }
            }
            Result.success()
        } catch (t: Throwable) {
            Log.e(TAG, "Widget refresh failed", t)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "WidgetRefreshWorker"
        const val WORK_NAME = "net_worth_widget_refresh"

        fun enqueueNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
