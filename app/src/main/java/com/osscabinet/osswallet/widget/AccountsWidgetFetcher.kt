package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import com.osscabinet.ossWallet.di.WidgetDataLoaderEntryPoint
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.computers.AccountsCardComputer
import com.osscabinet.ossWallet.domain.model.settings.AccountsCardConfig
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.ui.util.formatCurrency
import com.osscabinet.ossWallet.widget.WidgetDataLoader.CommonData
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.math.BigDecimal

@Serializable
data class AccountWidgetRow(
    val accountId: String,
    val name: String,
    val colorHex: String,
    val balanceFormatted: String,
    val netCaption: String,
    val sparklinePoints: List<Double>
)

class AccountsWidgetFetcher : WidgetDataFetcher {
    override suspend fun updateState(
        context: Context,
        widgetId: Int,
        common: CommonData,
        prefs: MutablePreferences
    ) {
        val config = AccountsWidgetPrefsStore(context).loadSafe(widgetId)
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
        val cardConfig = AccountsCardConfig(
            cardId = "accounts_widget_$widgetId",
            filter = effectiveFilter
        )

        val renderData = runCatching {
            AccountsCardComputer(common.databaseQueryService).compute(input, cardConfig)
        }.getOrNull()

        val orderedAccounts = renderData?.updatedAccounts
            .orEmpty()
            .filter { !it.isArchived }
            .sortedBy { it.displayOrder }
            .take(3)

        val rows = orderedAccounts.map { account ->
            val summary = renderData?.accountSummaries?.get(account.id)
            val currencyBalances = summary?.currencyBalances.orEmpty()
            val net = summary?.currentPeriodMetrics?.netFlow ?: BigDecimal.ZERO

            val netCaption = if (currencyBalances.size > 1) {
                val currencies = currencyBalances.keys.joinToString(", ")
                "$currencies · ${formatCurrency(net, account.currencyCode)} net"
            } else {
                val sign = if (net.signum() >= 0) "+" else ""
                "$sign${formatCurrency(net, account.currencyCode)} net"
            }

            AccountWidgetRow(
                accountId = account.id,
                name = account.name,
                colorHex = account.colorHex,
                balanceFormatted = formatCurrency(summary?.balance ?: BigDecimal.ZERO, account.currencyCode),
                netCaption = netCaption,
                sparklinePoints = summary?.balanceHistory.orEmpty().map { it.toDouble() }
            )
        }

        val totalInBase = orderedAccounts
            .mapNotNull { renderData?.accountSummaries?.get(it.id)?.balanceInBase }
            .fold(BigDecimal.ZERO) { acc, v -> acc.add(v) }

        prefs[AccountsWidgetKeys.IS_LOADED] = "true"
        prefs[AccountsWidgetKeys.PERIOD_LABEL] = config.periodLabel
        prefs[AccountsWidgetKeys.TOTAL_FORMATTED] = formatCurrency(totalInBase, common.baseCurrency)
        prefs[AccountsWidgetKeys.ROWS_JSON] = Json.encodeToString(rows)
    }
}
