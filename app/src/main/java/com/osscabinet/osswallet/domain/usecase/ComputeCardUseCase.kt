package com.osscabinet.ossWallet.domain.usecase

import com.osscabinet.ossWallet.domain.card.CardComputerRegistry
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.model.settings.CardConfig
import com.osscabinet.ossWallet.domain.service.category.CategoryHierarchyCache
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class ComputeCardUseCase(private val registry: CardComputerRegistry) {

    suspend fun computeAll(
        configs: List<CardConfig>,
        input: CardInput
    ): Map<String, CardRenderData> = coroutineScope {
        // Build hierarchy cache once for the whole batch — shared across all card computes.
        val enrichedInput = input.copy(
            categoryHierarchy = CategoryHierarchyCache.build(input.categories)
        )
        configs.map { config ->
            async {
                try {
                    config.cardId to registry.compute(config.cardType, enrichedInput, config)
                } catch (e: Exception) {
                    null
                }
            }
        }.mapNotNull { it.await() }.toMap()
    }
}
