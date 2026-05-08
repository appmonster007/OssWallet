package com.osscabinet.ossWallet.domain.card

import com.osscabinet.ossWallet.domain.model.settings.CardConfig

/**
 * Registry that orchestrates card computations.
 * Maps CardType to its respective CardDataComputer strategy.
 * Computers with DB access have DatabaseQueryService injected at construction time.
 */
class CardComputerRegistry(computers: List<CardDataComputer<*, *>>) {
    private val map = computers.associateBy { it.supportedType }

    @Suppress("UNCHECKED_CAST")
    suspend fun <C : CardConfig> compute(
        type: CardType,
        input: CardInput,
        config: C
    ): CardRenderData {
        val computer = map[type] ?: error("No computer registered for $type")
        return (computer as CardDataComputer<C, CardRenderData>).compute(input, config)
    }

    /**
     * No-op for backward compatibility.
     */
    fun clearCache() {}
}
