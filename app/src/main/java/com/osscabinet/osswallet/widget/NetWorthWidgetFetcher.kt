package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.model.settings.NetWorthCardConfig
import com.osscabinet.ossWallet.domain.service.aggregation.CardAggregationService
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
        val config = runCatching {
            NetWorthWidgetPrefsStore(context).load(widgetId)
        }.getOrElse { NetWorthWidgetConfig() }

        val renderData = runCatching {
            CardAggregationService.computeNetWorthCard(
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

        val formatter = CurrencyReprUi.CurrencyNumberFormatter()
        prefs[NetWorthWidgetKeys.IS_LOADED]            = "true"
        prefs[NetWorthWidgetKeys.NET_WORTH]            =
            formatter.format(netWorth, common.baseCurrency)
        prefs[NetWorthWidgetKeys.INC]                  =
            formatter.format(cashFlow?.metricsInBase?.income ?: BigDecimal.ZERO, common.baseCurrency)
        prefs[NetWorthWidgetKeys.EXP]                  =
            formatter.format((cashFlow?.metricsInBase?.expense ?: BigDecimal.ZERO).abs(), common.baseCurrency)
        prefs[NetWorthWidgetKeys.NET_WORTH_COMPARISON] =
            buildComparisonText(netWorth, prev?.netWorth, config.showComparison) ?: ""
        prefs[NetWorthWidgetKeys.PERIOD_LABEL]         = config.timelineLabel
        prefs[NetWorthWidgetKeys.FONT_SIZE]            = config.fontSize
    }
}
