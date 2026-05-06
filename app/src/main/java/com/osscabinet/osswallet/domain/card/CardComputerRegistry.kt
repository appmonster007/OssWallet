package com.osscabinet.ossWallet.domain.card

import com.osscabinet.ossWallet.domain.card.computers.*
import com.osscabinet.ossWallet.domain.card.computers.PlannedPaymentsCardComputer
import com.osscabinet.ossWallet.domain.card.computers.BudgetsCardComputer
import com.osscabinet.ossWallet.domain.card.computers.LoansCardComputer
import com.osscabinet.ossWallet.domain.card.computers.RecentActivitiesCardComputer
import com.osscabinet.ossWallet.domain.model.settings.CardConfig

/**
 * Registry that orchestrates card computations.
 * Maps CardType to its respective CardDataComputer strategy.
 * 
 * Note: Caching has been removed to ensure all cards refresh 
 * immediately on any filter or data change.
 */
class CardComputerRegistry(computers: List<CardDataComputer<*, *>>) {
    private val map = computers.associateBy { it.supportedType }

    @Suppress("UNCHECKED_CAST")
    fun <C : CardConfig> compute(type: CardType, input: CardInput, config: C): CardRenderData {
        val computer = map[type] ?: error("No computer registered for $type")
        return (computer as CardDataComputer<C, CardRenderData>).compute(input, config)
    }

    /**
     * No-op for backward compatibility. 
     * Cache is no longer used.
     */
    fun clearCache() {
        // No-op
    }

    companion object {
        /**
         * Provides a default instance with all standard computers.
         */
        val DEFAULT = CardComputerRegistry(
            listOf(
                NetWorthCardComputer(),
                CashFlowCardComputer(),
                CurrencyBalancesCardComputer(),
                AccountsCardComputer(),
                CategoryPieChartCardComputer(),
                CategoryTreemapCardComputer(),
                AccountHeroCardComputer(),
                PlannedPaymentsCardComputer(),
                BudgetsCardComputer(),
                LoansCardComputer(),
                RecentActivitiesCardComputer()
            )
        )
    }
}
