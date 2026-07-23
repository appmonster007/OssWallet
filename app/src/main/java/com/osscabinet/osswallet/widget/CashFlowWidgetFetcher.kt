package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.aggregation.CardAggregationService
import com.osscabinet.ossWallet.ui.util.formatCurrency
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import com.osscabinet.ossWallet.widget.WidgetDataLoader.CommonData
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal

class CashFlowWidgetFetcher : WidgetDataFetcher {
    override suspend fun updateState(
        context: Context,
        widgetId: Int,
        common: CommonData,
        prefs: MutablePreferences
    ) {
        val config = runCatching {
            CashFlowWidgetPrefsStore(context).load(widgetId)
        }.getOrElse { CashFlowWidgetConfig() }

        val renderData = runCatching {
            CardAggregationService.computeCashFlowCard(
                queryService = common.databaseQueryService,
                profileId = common.profileId,
                input = CardInput(
                    profileId = common.profileId,
                    targetCurrency = common.baseCurrency,
                    exchangeRates = com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot(
                        anchor = common.baseCurrency,
                        rates = common.exchangeRates
                    ),
                    filter = config.filter.copy(
                        timelineRange = config.filter.timelineRange.withExcludeFuture(true)
                    ),
                    accounts = emptyList(),
                    categories = emptyList()
                ),
                config = CashFlowCardConfig(
                    cardId = "cf_widget_$widgetId",
                    filter = config.filter.copy(
                        timelineRange = config.filter.timelineRange.withExcludeFuture(true)
                    )
                )
            )
        }.getOrNull()

        val result = renderData?.result
        val incomeInBase = result?.metricsInBase?.income ?: BigDecimal.ZERO
        val expenseInBase = result?.metricsInBase?.expense ?: BigDecimal.ZERO
        val netInBase = result?.metricsInBase?.netFlow ?: BigDecimal.ZERO

        val incomeFormatted = formatCurrency(incomeInBase, common.baseCurrency)
        val expenseFormatted = formatCurrency(expenseInBase, common.baseCurrency)
//        val transferFormatted = formatCurrency(transferInBase, common.baseCurrency)

        val prevNetInBase: BigDecimal? = if (config.showComparison) {
            val currentFilter = config.filter.copy(
                timelineRange = config.filter.timelineRange.withExcludeFuture(true)
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
                            targetCurrency = common.baseCurrency,
                            exchangeRates = com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot(
                                anchor = common.baseCurrency,
                                rates = common.exchangeRates
                            ),
                            filter = prevFilter,
                            accounts = emptyList(),
                            categories = emptyList()
                        ),
                        config = CashFlowCardConfig(cardId = "cf_widget_prev_$widgetId")
                    )
                }.getOrNull()?.result?.metricsInBase?.netFlow
            } else null
        } else null
        
        val comparisonText = buildComparisonText(
            netInBase, prevNetInBase, config.showComparison
        ) ?: ""

        val currencyJson = JSONArray().also { arr ->
            result?.breakdown?.forEach { item ->
                arr.put(
                    JSONObject().apply {
                        put("code", item.currency)
                        put("income", formatCurrency(item.metrics.income, item.currency))
                        put("incomeBase", formatCurrency(item.metricsInBase.income, common.baseCurrency))
                        put("expense", formatCurrency(item.metrics.expense, item.currency))
                        put("expenseBase", formatCurrency(item.metricsInBase.expense, common.baseCurrency))
                    }
                )
            }
        }.toString()

        prefs[CashFlowGlanceWidget.Keys.IS_LOADED] = "true"
        prefs[CashFlowGlanceWidget.Keys.INCOME_TOTAL] = incomeFormatted
        prefs[CashFlowGlanceWidget.Keys.EXPENSE_TOTAL] = expenseFormatted
//        prefs[CashFlowGlanceWidget.Keys.TRANSFER_TOTAL] = transferFormatted
        prefs[CashFlowGlanceWidget.Keys.PERIOD_LABEL] = config.timelineLabel
        prefs[CashFlowGlanceWidget.Keys.BASE_CURRENCY] = common.baseCurrency
        prefs[CashFlowGlanceWidget.Keys.SHOW_BASE_CONVERSION] = if (config.showBaseConversion) "true" else "false"
        prefs[CashFlowGlanceWidget.Keys.SHOW_TOTALS] = if (config.showTotals) "true" else "false"
        prefs[CashFlowGlanceWidget.Keys.CURRENCY_LIST] = currencyJson
        prefs[CashFlowGlanceWidget.Keys.COMPARISON] = comparisonText
    }
}
