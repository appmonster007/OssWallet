package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.aggregation.CardAggregationService
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatCurrencyRight
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
                    baseCurrency = common.baseCurrency,
                    exchangeRates = common.exchangeRates,
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
        val transferInBase = result?.totalTransfersInBase ?: BigDecimal.ZERO
        val netInBase = result?.netInBase ?: BigDecimal.ZERO

        val incomeFormatted = formatCurrencyRight(incomeInBase, common.baseCurrency, shorten = true)
        val expenseFormatted = formatCurrencyRight(expenseInBase, common.baseCurrency, shorten = true)
//        val transferFormatted = formatCurrencyRight(transferInBase, common.baseCurrency, shorten = true)

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

        val currencyJson = JSONArray().also { arr ->
            result?.breakdown?.forEach { item ->
                arr.put(
                    JSONObject().apply {
                        put("code", item.currency)
                        put("income", formatCurrencyRight(item.income, item.currency, shorten = true))
                        put("incomeBase", formatCurrencyRight(item.incomeInBase, common.baseCurrency, shorten = true))
                        put("expense", formatCurrencyRight(item.expense, item.currency, shorten = true))
                        put("expenseBase", formatCurrencyRight(item.expenseInBase, common.baseCurrency, shorten = true))
//                        put("transfer", formatCurrencyRight(item.transfer, item.currency, shorten = true))
//                        put("transferBase", formatCurrencyRight(item.transferInBase, common.baseCurrency, shorten = true))
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
