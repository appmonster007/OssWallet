package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import com.osscabinet.ossWallet.di.WidgetDataLoaderEntryPoint
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.computers.CurrencyBalancesCardComputer
import com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.ui.util.formatCurrency
import com.osscabinet.ossWallet.widget.WidgetDataLoader.CommonData
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class CurrencyBalanceWidgetRow(
    val currency: String,
    val amountFormatted: String,
    val amountInBaseFormatted: String
)

class CurrencyBalancesWidgetFetcher : WidgetDataFetcher {
    override suspend fun updateState(
        context: Context,
        widgetId: Int,
        common: CommonData,
        prefs: MutablePreferences
    ) {
        val config = CurrencyBalancesWidgetPrefsStore(context).loadSafe(widgetId)
        val ep = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetDataLoaderEntryPoint::class.java
        )
        val accounts = ep.accountRepository().getAllAccounts(common.profileId).first()

        val effectiveFilter = config.filter.copy(
            timelineRange = config.filter.timelineRange.withExcludeFuture(true)
        )
        val input = CardInput(
            profileId = common.profileId,
            targetCurrency = common.baseCurrency,
            exchangeRates = ExchangeRateSnapshot(anchor = common.baseCurrency, rates = common.exchangeRates),
            filter = effectiveFilter,
            accounts = accounts,
            categories = emptyList()
        )
        val cardConfig = CurrencyBalancesCardConfig(
            cardId = "currency_balances_widget_$widgetId",
            filter = effectiveFilter,
            showBaseConversion = config.showBaseConversion
        )

        val renderData = runCatching {
            CurrencyBalancesCardComputer(common.databaseQueryService).compute(input, cardConfig)
        }.getOrNull()

        val rows = renderData?.items.orEmpty().take(4).map { item ->
            CurrencyBalanceWidgetRow(
                currency = item.currency,
                amountFormatted = formatCurrency(item.amount, item.currency),
                amountInBaseFormatted = formatCurrency(item.amountInBase, common.baseCurrency)
            )
        }
        val totalInBase = renderData?.items.orEmpty().sumOf { it.amountInBase }

        prefs[CurrencyBalancesWidgetKeys.IS_LOADED] = "true"
        prefs[CurrencyBalancesWidgetKeys.TOTAL_FORMATTED] = formatCurrency(totalInBase, common.baseCurrency)
        prefs[CurrencyBalancesWidgetKeys.BASE_CURRENCY] = common.baseCurrency
        prefs[CurrencyBalancesWidgetKeys.SHOW_BASE_CONVERSION] = if (config.showBaseConversion) "true" else "false"
        prefs[CurrencyBalancesWidgetKeys.ROWS_JSON] = Json.encodeToString(rows)
    }
}
