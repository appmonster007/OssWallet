package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.stringPreferencesKey
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

object AccountsWidgetKeys {
    val IS_LOADED = stringPreferencesKey("accounts_is_loaded")
    val TOTAL_FORMATTED = stringPreferencesKey("accounts_total_formatted")
    val ROWS_JSON = stringPreferencesKey("accounts_rows_json")
    /** Name of a [WidgetTheme] constant — see NetWorthWidgetKeys.THEME. */
    val THEME = stringPreferencesKey("accounts_theme")
}

@Serializable
data class AccountWidgetRow(
    val accountId: String,
    val name: String,
    val colorHex: String,
    /** Account icon name, resolved with getIconByName like every card does. */
    val iconName: String,
    val balanceFormatted: String,
    val balanceNegative: Boolean,
    /** Net flow over the widget's period, e.g. "+$1,200.00 · This Month". */
    val netCaption: String,
    val netNegative: Boolean
)

/** Drives the widget off the same [AccountsCardComputer] the dashboard card uses. */
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

        val ordered = renderData?.updatedAccounts
            .orEmpty()
            .filter { !it.isArchived }
            .sortedBy { it.displayOrder }

        val rows = ordered.take(AccountsWidgetConfig.MAX_ROWS).map { account ->
            val summary = renderData?.accountSummaries?.get(account.id)
            val balance = summary?.balance ?: BigDecimal.ZERO
            val net = summary?.currentPeriodMetrics?.netFlow ?: BigDecimal.ZERO
            val sign = if (net.signum() > 0) "+" else ""
            AccountWidgetRow(
                accountId = account.id,
                name = account.name,
                colorHex = account.colorHex,
                iconName = account.icon,
                balanceFormatted = formatCurrency(balance, account.currencyCode),
                balanceNegative = balance.signum() < 0,
                netCaption = "$sign${formatCurrency(net, account.currencyCode)} · ${config.periodLabel}",
                netNegative = net.signum() < 0
            )
        }

        // The header total covers every active account, not just the visible rows.
        val total = ordered
            .mapNotNull { renderData?.accountSummaries?.get(it.id)?.balanceInBase }
            .fold(BigDecimal.ZERO) { acc, v -> acc.add(v) }

        prefs[AccountsWidgetKeys.IS_LOADED] = "true"
        prefs[AccountsWidgetKeys.THEME] = config.theme.name
        prefs[AccountsWidgetKeys.TOTAL_FORMATTED] = formatCurrency(total, common.baseCurrency)
        prefs[AccountsWidgetKeys.ROWS_JSON] = Json.encodeToString(rows)
    }
}
