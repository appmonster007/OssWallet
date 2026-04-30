package com.osscabinet.ossWallet.domain.card

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.analytics.*
import com.osscabinet.ossWallet.domain.model.settings.*
import com.osscabinet.ossWallet.domain.service.*
import java.math.BigDecimal

/**
 * Sealed class hierarchy for unified card rendering data.
 * Every card computation returns a subtype of this class.
 */
sealed class CardRenderData {
    
    data class NetWorth(
        val config: NetWorthCardConfig,
        val value: BigDecimal,
        val cashFlowResult: CashFlowResult,
        val baseCurrency: String,
        val precision: Int
    ) : CardRenderData()

    data class CurrencyBalances(
        val config: CurrencyBalancesCardConfig,
        val items: List<CurrencyAggregationItem>,
        val baseCurrency: String,
        val precision: Int
    ) : CardRenderData()

    data class CashFlow(
        val config: CashFlowCardConfig,
        val result: CashFlowResult,
        val baseCurrency: String,
        val precision: Int
    ) : CardRenderData()

    data class Accounts(
        val config: AccountsCardConfig,
        val updatedAccounts: List<Account>,
        val accountStats: Map<String, AccountSummaryItem>,
        val baseCurrency: String,
        val currentTheme: String,
        val precision: Int
    ) : CardRenderData()

    data class CategoryBreakdown(
        val config: CategoryBreakdownCardConfig,
        val items: List<CategoryTreeMetrics>
    ) : CardRenderData()

    data class CategoryTreemap(
        val config: CategoryExpenseTreemapCardConfig,
        val items: List<CategoryTreeMetrics>
    ) : CardRenderData()

    data class AccountHero(
        val config: AccountHeroCardConfig,
        val account: Account,
        val balance: BigDecimal,
        val netBalance: BigDecimal,
        val baseCurrency: String,
        val exchangeRates: Map<String, BigDecimal>,
        val precision: Int
    ) : CardRenderData()
}
