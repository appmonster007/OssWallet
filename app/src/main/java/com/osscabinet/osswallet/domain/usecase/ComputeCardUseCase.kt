package com.osscabinet.ossWallet.domain.usecase

import android.util.Log
import com.osscabinet.ossWallet.domain.card.CardComputerRegistry
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.model.settings.CardConfig
import com.osscabinet.ossWallet.domain.service.category.CategoryHierarchyCache
import kotlinx.coroutines.CancellationException
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
                // ARC-41: surface compute failures as CardRenderData.Error instead of dropping
                // the card silently. A user staring at a missing card with no explanation can't
                // tell whether the data is unavailable or the app is broken; an explicit Error
                // state lets the renderer show a "failed to load" placeholder.
                val data = try {
                    registry.compute(config.cardType, enrichedInput, config)
                } catch (e: CancellationException) {
                    // Structured concurrency: cancellation must propagate, not become an Error.
                    throw e
                } catch (e: Throwable) {
                    Log.e(
                        "ComputeCardUseCase",
                        "Compute failed for ${config.cardType} (cardId=${config.cardId})",
                        e
                    )
                    CardRenderData.Error(e.message ?: e::class.simpleName)
                }
                config.cardId to data
            }
        }.map { it.await() }.toMap()
    }
}
