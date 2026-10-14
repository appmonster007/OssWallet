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
import java.math.BigDecimal

object AccountsWidgetKeys {
    val IS_LOADED = stringPreferencesKey("accounts_is_loaded")
    /** Number of rows, so the header can tell an empty list from a loaded one. */
    val COUNT = stringPreferencesKey("accounts_count")
    val TOTAL_FORMATTED = stringPreferencesKey("accounts_total_formatted")
    /** Name of a [WidgetTheme] constant — see NetWorthWidgetKeys.THEME. */
    val THEME = stringPreferencesKey("accounts_theme")
    val GLASS = stringPreferencesKey("accounts_glass")
}

@Serializable
data class AccountWidgetRow(
    val accountId: String,
    val name: String,
    val colorHex: String,
    /** Account icon name, resolved with getAccountIconByName like the Accounts card. */
    val iconName: String,
    val balanceFormatted: String,
    val balanceNegative: Boolean,
    /** Net flow over the widget's period, e.g. "+$1,200.00 · This Month". */
    val netCaption: String,
    val netNegative: Boolean
)

/** Everything the Accounts widget shows: every active account in order, and the header total. */
data class AccountsSnapshot(
    val rows: List<AccountWidgetRow>,
    val totalFormatted: String,
    val theme: WidgetTheme,
    val glass: Boolean
)

/** Drives the widget off the same [AccountsCardComputer] the dashboard card uses. */
class AccountsWidgetFetcher : WidgetDataFetcher {
    override suspend fun updateState(
        context: Context,
        widgetId: Int,
        common: CommonData,
        prefs: MutablePreferences
    ) {
        val snapshot = load(context, widgetId, common)
        prefs[AccountsWidgetKeys.IS_LOADED] = "true"
        prefs[AccountsWidgetKeys.COUNT] = snapshot.rows.size.toString()
        prefs[AccountsWidgetKeys.THEME] = snapshot.theme.name
        prefs[AccountsWidgetKeys.GLASS] = snapshot.glass.toString()
        prefs[AccountsWidgetKeys.TOTAL_FORMATTED] = snapshot.totalFormatted
    }

    companion object {
        suspend fun load(context: Context, widgetId: Int, common: CommonData): AccountsSnapshot {
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

            val rows = ordered.map { account ->
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

            // The header total covers the same accounts the list shows.
            val total = ordered
                .mapNotNull { renderData?.accountSummaries?.get(it.id)?.balanceInBase }
                .fold(BigDecimal.ZERO) { acc, v -> acc.add(v) }

            return AccountsSnapshot(
                rows = rows,
                totalFormatted = formatCurrency(total, common.baseCurrency),
                theme = config.theme,
                glass = config.glass
            )
        }
    }
}
