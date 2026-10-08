package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.model.settings.NetWorthCardConfig
import com.osscabinet.ossWallet.domain.service.aggregation.CardAggregationService
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.ui.util.CurrencyReprUi
import com.osscabinet.ossWallet.widget.WidgetDataLoader.CommonData
import java.math.BigDecimal

class NetWorthWidgetFetcher : WidgetDataFetcher {
    override suspend fun updateState(
        context: Context,
        widgetId: Int,
        common: CommonData,
        prefs: MutablePreferences
    ) {
        val config = NetWorthWidgetPrefsStore(context).loadSafe(widgetId)

        val renderData = runCatching {
            CardAggregationService.computeNetWorthCard(
                queryService = common.databaseQueryService,
                profileId = common.profileId,
                input = CardInput(
                    profileId = common.profileId,
                    targetCurrency = common.baseCurrency,
                    exchangeRates = ExchangeRateSnapshot(
                        anchor = common.baseCurrency,
                        rates = common.exchangeRates
                    ),
                    filter = config.filter.copy(
                        timelineRange = config.filter.timelineRange.withExcludeFuture(true)
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

        val netWorth = renderData?.currentPeriodMetrics?.netWorth ?: BigDecimal.ZERO
        val cashFlow = renderData?.currentPeriodMetrics?.cashFlow
        val prev     = renderData?.previousPeriodMetrics
        val prevMetrics = prev?.cashFlow?.metricsInBase

        val formatter = CurrencyReprUi.CurrencyNumberFormatter()
        fun fmt(v: BigDecimal?) = formatter.format(v ?: BigDecimal.ZERO, common.baseCurrency)

        val metricsInBase = cashFlow?.metricsInBase

        prefs[NetWorthWidgetKeys.IS_LOADED]            = "true"
        prefs[NetWorthWidgetKeys.NET_WORTH]            = fmt(netWorth)
        prefs[NetWorthWidgetKeys.INC]                  = fmt(metricsInBase?.income ?: BigDecimal.ZERO)
        prefs[NetWorthWidgetKeys.EXP]                  = fmt((metricsInBase?.expense ?: BigDecimal.ZERO).abs())
        prefs[NetWorthWidgetKeys.NET_WORTH_COMPARISON] =
            buildComparisonText(netWorth, prev?.netWorth, config.showComparison) ?: ""
        prefs[NetWorthWidgetKeys.PERIOD_LABEL]         = config.timelineLabel
        prefs[NetWorthWidgetKeys.FONT_SIZE]            = config.fontSize
        prefs[NetWorthWidgetKeys.BASE_CURRENCY]        = common.baseCurrency
        // Large-tier metrics[] — mirrors NetWorthCard's per-metric income/expense/transfer/
        // inflow/outflow/netFlow click targets (CardCallbacks.NetWorth), see docs/card-data-summary.md.
        prefs[NetWorthWidgetKeys.PERSPECTIVE_LABEL]    = "Cash Flow"
        prefs[NetWorthWidgetKeys.INCOME]               = fmt(metricsInBase?.income ?: BigDecimal.ZERO)
        prefs[NetWorthWidgetKeys.INCOME_CMP]           =
            buildComparisonText(metricsInBase?.income ?: BigDecimal.ZERO, prevMetrics?.income, config.showComparison) ?: ""
        prefs[NetWorthWidgetKeys.EXPENSE]              = fmt((metricsInBase?.expense ?: BigDecimal.ZERO).abs())
        prefs[NetWorthWidgetKeys.EXPENSE_CMP]          =
            buildComparisonText(metricsInBase?.expense ?: BigDecimal.ZERO, prevMetrics?.expense, config.showComparison) ?: ""
        prefs[NetWorthWidgetKeys.TRANSFER]             = fmt(metricsInBase?.transfer ?: BigDecimal.ZERO)
        prefs[NetWorthWidgetKeys.TRANSFER_CMP]         =
            buildComparisonText(metricsInBase?.transfer ?: BigDecimal.ZERO, prevMetrics?.transfer, config.showComparison) ?: ""
        prefs[NetWorthWidgetKeys.INFLOW]               = fmt(metricsInBase?.inflow ?: BigDecimal.ZERO)
        prefs[NetWorthWidgetKeys.INFLOW_CMP]           =
            buildComparisonText(metricsInBase?.inflow ?: BigDecimal.ZERO, prevMetrics?.inflow, config.showComparison) ?: ""
        prefs[NetWorthWidgetKeys.OUTFLOW]              = fmt(metricsInBase?.outflow ?: BigDecimal.ZERO)
        prefs[NetWorthWidgetKeys.OUTFLOW_CMP]          =
            buildComparisonText(metricsInBase?.outflow ?: BigDecimal.ZERO, prevMetrics?.outflow, config.showComparison) ?: ""
        prefs[NetWorthWidgetKeys.NET_FLOW]             = fmt(metricsInBase?.netFlow ?: BigDecimal.ZERO)
        prefs[NetWorthWidgetKeys.NET_FLOW_CMP]         =
            buildComparisonText(metricsInBase?.netFlow ?: BigDecimal.ZERO, prevMetrics?.netFlow, config.showComparison) ?: ""
    }
}
