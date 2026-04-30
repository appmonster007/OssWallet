package com.osscabinet.ossWallet.domain.card

import com.osscabinet.ossWallet.domain.card.computers.*
import com.osscabinet.ossWallet.domain.model.settings.CardConfig

/**
 * Registry that orchestrates card computations.
 * Maps CardType to its respective CardDataComputer strategy.
 */
class CardComputerRegistry(computers: List<CardDataComputer<*, *>>) {
    private val map = computers.associateBy { it.supportedType }
    private val cache = mutableMapOf<Int, CardRenderData>()

    @Suppress("UNCHECKED_CAST")
    fun <C : CardConfig> compute(type: CardType, input: CardInput, config: C): CardRenderData {
        val cacheKey = (type.hashCode() * 31 + input.hashCode()) * 31 + config.hashCode()
        cache[cacheKey]?.let { return it }

        val computer = map[type] ?: error("No computer registered for $type")
        val result = (computer as CardDataComputer<C, CardRenderData>).compute(input, config)
        
        cache[cacheKey] = result
        return result
    }

    fun clearCache() {
        cache.clear()
    }

    companion object {
        /**
         * Provides a default instance with all standard computers.
         * In a real app, this would be provided via Dependency Injection.
         */
        val DEFAULT = CardComputerRegistry(
            listOf(
                NetWorthCardComputer(),
                CashFlowCardComputer(),
                CurrencyBalancesCardComputer(),
                AccountsCardComputer(),
                CategoryBreakdownCardComputer(),
                CategoryTreemapCardComputer(),
                AccountHeroCardComputer()
            )
        )
    }
}
