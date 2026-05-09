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
import com.osscabinet.ossWallet.domain.model.settings.NetWorthCardConfig
import com.osscabinet.ossWallet.domain.service.aggregation.CardAggregationService
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
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
