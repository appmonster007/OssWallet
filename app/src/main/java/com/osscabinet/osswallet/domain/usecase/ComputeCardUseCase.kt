package com.osscabinet.ossWallet.domain.usecase

import com.osscabinet.ossWallet.domain.card.CardComputerRegistry
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.model.settings.CardConfig
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class ComputeCardUseCase(private val registry: CardComputerRegistry) {

    suspend fun computeAll(
        configs: List<CardConfig>,
        input: CardInput
    ): Map<String, CardRenderData> = coroutineScope {
        configs.map { config ->
            async {
                try {
                    config.cardId to registry.compute(config.cardType, input, config)
                } catch (e: Exception) {
                    null
                }
            }
        }.mapNotNull { it.await() }.toMap()
    }
}
