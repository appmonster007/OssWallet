package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.model.TransactionFilter
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
                    filter = TransactionFilter(
                        timelineRange = config.timelineRange.withExcludeFuture(true)
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

        val formatter = CurrencyReprUi.CurrencyNumberFormatter()
        prefs[NetWorthGlanceWidget.Keys.IS_LOADED] = "true"
        prefs[NetWorthGlanceWidget.Keys.NET_WORTH] =
            formatter.format(netWorth, common.baseCurrency)
        prefs[NetWorthGlanceWidget.Keys.INCOME] =
            formatter.format(income, common.baseCurrency)
        prefs[NetWorthGlanceWidget.Keys.EXPENSE] =
            formatter.format(expense, common.baseCurrency)
        prefs[NetWorthGlanceWidget.Keys.NET_WORTH_COMPARISON] =
            buildComparisonText(netWorth, prev?.netWorth, config.showComparison) ?: ""
        prefs[NetWorthGlanceWidget.Keys.INCOME_COMPARISON] =
            buildComparisonText(income, prev?.cashFlow?.totalIncomeInBase, config.showComparison) ?: ""
        prefs[NetWorthGlanceWidget.Keys.EXPENSE_COMPARISON] =
            buildComparisonText(expense, prev?.cashFlow?.totalExpensesInBase, config.showComparison) ?: ""
        prefs[NetWorthGlanceWidget.Keys.PERIOD_LABEL] = config.timelineLabel
        prefs[NetWorthGlanceWidget.Keys.FONT_SIZE] = config.fontSize
    }
}
