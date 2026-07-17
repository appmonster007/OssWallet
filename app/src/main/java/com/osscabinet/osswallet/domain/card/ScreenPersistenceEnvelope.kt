package com.osscabinet.ossWallet.domain.card

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Loan
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.settings.DashboardCardsConfig
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.ui.screens.transactions.model.TransactionListUiState
import kotlinx.serialization.Serializable

/**
 * Envelope for persistent screen state that is not a single card.
 * This is used to hydrate UI headers, metadata, and feeds instantly from cache.
 */
@Serializable
sealed interface ScreenPersistenceEnvelope {

    @Serializable
    data class Feed(val state: TransactionListUiState) : ScreenPersistenceEnvelope

    @Serializable
    data class Snapshot(
        val accounts: List<Account>,
        val categories: List<Category>,
        val tags: List<Tag>,
        val plannedPayments: List<PlannedPayment> = emptyList(),
        val budgets: List<Budget> = emptyList(),
        val loans: List<Loan> = emptyList(),
        val baseCurrency: String,
        val exchangeRates: ExchangeRateSnapshot,
        val cardsConfig: DashboardCardsConfig = DashboardCardsConfig()
    ) : ScreenPersistenceEnvelope
}
