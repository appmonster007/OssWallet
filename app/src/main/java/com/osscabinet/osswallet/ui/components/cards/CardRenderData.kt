package com.osscabinet.ossWallet.ui.components.cards

import com.osscabinet.ossWallet.ui.analytics.components.CategoryBreakdownData
import com.osscabinet.ossWallet.ui.analytics.components.CategoryExpenseTreemapData
import com.osscabinet.ossWallet.domain.model.settings.CategoryBreakdownCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CategoryExpenseTreemapCardConfig
import com.osscabinet.ossWallet.domain.model.settings.AccountHeroCardConfig
import com.osscabinet.ossWallet.domain.model.settings.AccountsCardConfig
import com.osscabinet.ossWallet.domain.model.settings.NetWorthCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig
import com.osscabinet.ossWallet.domain.model.analytics.AccountSummaryItem
import com.osscabinet.ossWallet.ui.viewmodel.NetWorthUiState
import com.osscabinet.ossWallet.domain.model.analytics.CurrencyAggregationItem
import com.osscabinet.ossWallet.domain.model.analytics.CashFlowResult
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.model.Account
import java.math.BigDecimal

/**
 * Sealed class hierarchy for unified card rendering data.
 * Each subtype represents the data required for a specific card type.
 */
sealed class CardRenderData {
    
    /**
     * Data for NetWorth card
     * Uses shared NetWorthCardConfig across all screens
     * Screens set accountId or selectedAccountIds to filter data for their context
     */
    data class NetWorth(
        val config: NetWorthCardConfig,
        val uiState: NetWorthUiState
    ) : CardRenderData()
    
    /**
     * Data for CurrencyBalances card
     * Uses shared CurrencyBalancesCardConfig across all screens
     * Screens set accountId to filter data for their context
     */
    data class CurrencyBalances(
        val config: CurrencyBalancesCardConfig,
        val currencyTotals: List<CurrencyAggregationItem>,
        val baseCurrency: String,
        val precision: Int
    ) : CardRenderData()
    
    /**
     * Data for CashFlow card
     * Uses shared CashFlowCardConfig across all screens
     * Screens set accountId to filter data for their context
     */
    data class CashFlow(
        val config: CashFlowCardConfig,
        val cashFlowResult: CashFlowResult,
        val baseCurrency: String,
        val precision: Int
    ) : CardRenderData()
    
    /**
     * Data for CategoryBreakdown card
     */
    data class CategoryBreakdown(
        val config: CategoryBreakdownCardConfig,
        val data: List<CategoryBreakdownData>
    ) : CardRenderData()
    
    /**
     * Data for CategoryExpenseTreemap card
     */
    data class CategoryExpenseTreemap(
        val config: CategoryExpenseTreemapCardConfig,
        val data: List<CategoryExpenseTreemapData>
    ) : CardRenderData()

    /**
     * Data for AccountHero card
     */
    data class AccountHero(
        val config: AccountHeroCardConfig,
        val account: Account,
        val balance: BigDecimal,
        val netBalance: BigDecimal,
        val baseCurrency: String,
        val exchangeRates: Map<String, BigDecimal>,
        val precision: Int
    ) : CardRenderData()
    
    /**
     * Data for Accounts card
     * Uses shared AccountsCardConfig across all screens
     */
    data class Accounts(
        val config: AccountsCardConfig,
        val accounts: List<Account>,
        val accountStats: Map<String, AccountSummaryItem>,
        val baseCurrency: String,
        val currentTheme: String,
        val precision: Int
    ) : CardRenderData()
    
}
