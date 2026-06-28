package com.osscabinet.ossWallet.domain.card

/**
 * Per-card render state. Each slot transitions Loading → Success | Error independently,
 * eliminating the null-sentinel + isCardsLoading boolean pattern in AeonCardList.
 *
 * No @Immutable annotation: domain/ prohibits androidx.compose.runtime imports.
 * Kotlin 2.0+ Compose Compiler Plugin infers sealed class stability automatically when
 * all subtypes are stable (data object / data class with stable fields). If recomposition
 * issues are observed, add this class to composeCompiler { stabilityConfigurationFile }.
 */
sealed class CardComputeState {
    data object Loading : CardComputeState()
    data class Success(val data: CardRenderData) : CardComputeState()
    data class Error(val message: String) : CardComputeState()
}
