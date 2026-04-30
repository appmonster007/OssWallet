package com.osscabinet.ossWallet.domain.model.analytics

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.service.*
import java.math.BigDecimal

/**
 * Sealed class representing the output of a card computation.
 * This data is "ready-to-render" and decoupled from business logic.
 */
sealed class CardRenderData {
    data class NetWorth(
        val value: BigDecimal,
        val cashFlowResult: CashFlowResult
    ) : CardRenderData()

    data class CurrencyBalances(
        val items: List<CurrencyAggregationItem>
    ) : CardRenderData()

    data class CashFlow(
        val result: CashFlowResult
    ) : CardRenderData()

    data class Accounts(
        val updatedAccounts: List<Account>,
        val accountStats: Map<String, AccountSummaryItem>
    ) : CardRenderData()

    data class CategoryBreakdownResult(
        val items: List<CategoryTreeMetrics>
    ) : CardRenderData()

    data class CategoryTreemapResult(
        val items: List<CategoryTreeMetrics>
    ) : CardRenderData()
}
