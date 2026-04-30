package com.osscabinet.ossWallet.ui.components.cards

import com.osscabinet.ossWallet.domain.card.CardRenderData

/**
 * Sealed class hierarchy that pairs specific card render data with its corresponding callbacks.
 * This ensures type safety at the compiler level, eliminating the need for unsafe casts
 * in the renderer.
 */
sealed class CardWithCallbacks {
    
    data class NetWorth(
        val renderData: CardRenderData.NetWorth,
        val callbacks: CardCallbacks.NetWorth
    ) : CardWithCallbacks()

    data class CurrencyBalances(
        val renderData: CardRenderData.CurrencyBalances,
        val callbacks: CardCallbacks.CurrencyBalances
    ) : CardWithCallbacks()

    data class CashFlow(
        val renderData: CardRenderData.CashFlow,
        val callbacks: CardCallbacks.CashFlow
    ) : CardWithCallbacks()

    data class Accounts(
        val renderData: CardRenderData.Accounts,
        val callbacks: CardCallbacks.Accounts
    ) : CardWithCallbacks()

    data class CategoryBreakdown(
        val renderData: CardRenderData.CategoryBreakdown,
        val callbacks: CardCallbacks.Analytics
    ) : CardWithCallbacks()

    data class CategoryTreemap(
        val renderData: CardRenderData.CategoryTreemap,
        val callbacks: CardCallbacks.Analytics
    ) : CardWithCallbacks()

    data class AccountHero(
        val renderData: CardRenderData.AccountHero,
        val callbacks: CardCallbacks.AccountHero
    ) : CardWithCallbacks()
}
