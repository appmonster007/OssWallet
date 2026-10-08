package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.osscabinet.ossWallet.di.WidgetDataLoaderEntryPoint
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.computers.AccountHeroCardComputer
import com.osscabinet.ossWallet.domain.model.forAccount
import com.osscabinet.ossWallet.domain.model.settings.AccountHeroCardConfig
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.ui.util.formatCurrency
import com.osscabinet.ossWallet.widget.WidgetDataLoader.CommonData
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object AccountHeroWidgetKeys {
    val IS_LOADED = stringPreferencesKey("ah_is_loaded")
    val ACCOUNT_FOUND = stringPreferencesKey("ah_account_found")
    val ACCOUNT_NAME = stringPreferencesKey("ah_account_name")
    val COLOR_HEX = stringPreferencesKey("ah_color_hex")
    val PERIOD_LABEL = stringPreferencesKey("ah_period_label")
    val BALANCE_FORMATTED = stringPreferencesKey("ah_balance_formatted")
    val COMPARISON = stringPreferencesKey("ah_comparison")
    val SPARKLINE_JSON = stringPreferencesKey("ah_sparkline_json")
}

class AccountHeroWidgetFetcher : WidgetDataFetcher {
    override suspend fun updateState(
        context: Context,
        widgetId: Int,
        common: CommonData,
        prefs: MutablePreferences
    ) {
        val config = AccountHeroWidgetPrefsStore(context).loadSafe(widgetId)
        val accountId = config.accountId
        if (accountId == null) {
            prefs[AccountHeroWidgetKeys.IS_LOADED] = "true"
            prefs[AccountHeroWidgetKeys.ACCOUNT_FOUND] = "false"
            return
        }

        val ep = EntryPointAccessors.fromApplication(context.applicationContext, WidgetDataLoaderEntryPoint::class.java)
        val accounts = ep.accountRepository().getAllAccounts(common.profileId).first()
        val account = accounts.find { it.id == accountId }
        if (account == null) {
            prefs[AccountHeroWidgetKeys.IS_LOADED] = "true"
            prefs[AccountHeroWidgetKeys.ACCOUNT_FOUND] = "false"
            return
        }

        val scopedFilter = config.filter.forAccount(accountId)
        val input = CardInput(
            profileId = common.profileId,
            targetCurrency = account.currencyCode,
            exchangeRates = ExchangeRateSnapshot(anchor = common.baseCurrency, rates = common.exchangeRates),
            filter = scopedFilter,
            accounts = accounts,
            categories = emptyList()
        )
        val cardConfig = AccountHeroCardConfig(
            cardId = "account_hero_widget_$widgetId",
            filter = scopedFilter
        )

        val renderData = runCatching {
            AccountHeroCardComputer(common.databaseQueryService).compute(input, cardConfig)
        }.getOrNull()

        val summary = renderData?.summary
        val comparison = buildComparisonText(
            current = summary?.currentPeriodMetrics?.netFlow ?: java.math.BigDecimal.ZERO,
            previous = summary?.previousPeriodMetrics?.netFlow,
            showComparison = cardConfig.showComparison
        )

        prefs[AccountHeroWidgetKeys.IS_LOADED] = "true"
        prefs[AccountHeroWidgetKeys.ACCOUNT_FOUND] = "true"
        prefs[AccountHeroWidgetKeys.ACCOUNT_NAME] = account.name
        prefs[AccountHeroWidgetKeys.COLOR_HEX] = account.colorHex
        prefs[AccountHeroWidgetKeys.PERIOD_LABEL] = config.periodLabel
        prefs[AccountHeroWidgetKeys.BALANCE_FORMATTED] = formatCurrency(summary?.balance, account.currencyCode)
        prefs[AccountHeroWidgetKeys.COMPARISON] = comparison ?: ""
        prefs[AccountHeroWidgetKeys.SPARKLINE_JSON] =
            Json.encodeToString(summary?.balanceHistory.orEmpty().map { it.toDouble() })
    }
}
