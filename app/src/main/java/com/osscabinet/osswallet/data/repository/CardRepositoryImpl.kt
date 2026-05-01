package com.osscabinet.ossWallet.data.repository

import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.domain.model.settings.AccountHeroCardConfig
import com.osscabinet.ossWallet.domain.model.settings.AccountsCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CardConfig
import com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CategoryBreakdownCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CategoryExpenseTreemapCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig
import com.osscabinet.ossWallet.domain.model.settings.DashboardCardsConfig
import com.osscabinet.ossWallet.domain.model.settings.NetWorthCardConfig
import com.osscabinet.ossWallet.domain.model.settings.BudgetsCardConfig
import com.osscabinet.ossWallet.domain.model.settings.LoansCardConfig
import com.osscabinet.ossWallet.domain.model.settings.PlannedPaymentsCardConfig
import com.osscabinet.ossWallet.domain.repository.CardRepository
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class CardRepositoryImpl(
    private val settingsDataStore: SettingsDataStore
) : CardRepository {
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override val dashboardCardsConfig: Flow<DashboardCardsConfig> = settingsDataStore.dashboardCardsConfig.map { 
        if (it.isEmpty()) DashboardCardsConfig() else json.decodeFromString(it)
    }

    override val accountDetailCardsConfig: Flow<DashboardCardsConfig> = settingsDataStore.accountDetailCardsConfig.map { 
        if (it.isEmpty()) DashboardCardsConfig() else json.decodeFromString(it)
    }

    override val accountsViewCardsConfig: Flow<DashboardCardsConfig> = settingsDataStore.accountsViewCardsConfig.map { 
        if (it.isEmpty()) DashboardCardsConfig() else json.decodeFromString(it)
    }

    override suspend fun updateDashboardCard(cardId: String, update: (CardConfig) -> CardConfig) {
        val current = dashboardCardsConfig.first()
        val updated = current.copy(cards = current.cards.map { if (it.cardId == cardId) update(it) else it })
        settingsDataStore.saveDashboardCardsConfig(json.encodeToString(updated))
    }

    override suspend fun updateAccountDetailCard(cardId: String, update: (CardConfig) -> CardConfig) {
        val current = accountDetailCardsConfig.first()
        val updated = current.copy(cards = current.cards.map { if (it.cardId == cardId) update(it) else it })
        settingsDataStore.saveAccountDetailCardsConfig(json.encodeToString(updated))
    }

    override suspend fun updateAccountsViewCard(cardId: String, update: (CardConfig) -> CardConfig) {
        val current = accountsViewCardsConfig.first()
        val updated = current.copy(cards = current.cards.map { if (it.cardId == cardId) update(it) else it })
        settingsDataStore.saveAccountsViewCardsConfig(json.encodeToString(updated))
    }

    override suspend fun addDashboardCard(card: CardConfig) {
        val current = dashboardCardsConfig.first()
        val updated = current.copy(cards = current.cards + card)
        settingsDataStore.saveDashboardCardsConfig(json.encodeToString(updated))
    }

    override suspend fun removeDashboardCard(cardId: String) {
        val current = dashboardCardsConfig.first()
        val updated = current.copy(cards = current.cards.filter { it.cardId != cardId })
        settingsDataStore.saveDashboardCardsConfig(json.encodeToString(updated))
    }

    override suspend fun reorderDashboardCards(cards: List<CardConfig>) {
        // Update order property to match new positions for each card type
        val reorderedWithUpdatedOrder = cards.mapIndexed { index, card ->
            when (card) {
                is NetWorthCardConfig -> card.copy(order = index)
                is CurrencyBalancesCardConfig -> card.copy(order = index)
                is CashFlowCardConfig -> card.copy(order = index)
                is AccountsCardConfig -> card.copy(order = index)
                is CategoryBreakdownCardConfig -> card.copy(order = index)
                is CategoryExpenseTreemapCardConfig -> card.copy(order = index)
                is AccountHeroCardConfig -> card.copy(order = index)
                is PlannedPaymentsCardConfig -> card.copy(order = index)
                is BudgetsCardConfig -> card.copy(order = index)
                is LoansCardConfig -> card.copy(order = index)
            }
        }
        val updated = DashboardCardsConfig(cards = reorderedWithUpdatedOrder)
        settingsDataStore.saveDashboardCardsConfig(json.encodeToString(updated))
    }

    override suspend fun addAccountDetailCard(card: CardConfig) {
        val current = accountDetailCardsConfig.first()
        val updated = current.copy(cards = current.cards + card)
        settingsDataStore.saveAccountDetailCardsConfig(json.encodeToString(updated))
    }

    override suspend fun removeAccountDetailCard(cardId: String) {
        val current = accountDetailCardsConfig.first()
        val updated = current.copy(cards = current.cards.filter { it.cardId != cardId })
        settingsDataStore.saveAccountDetailCardsConfig(json.encodeToString(updated))
    }

    override suspend fun reorderAccountDetailCards(cards: List<CardConfig>) {
        // Update order property to match new positions for each card type
        val reorderedWithUpdatedOrder = cards.mapIndexed { index, card ->
            when (card) {
                is NetWorthCardConfig -> card.copy(order = index)
                is CurrencyBalancesCardConfig -> card.copy(order = index)
                is CashFlowCardConfig -> card.copy(order = index)
                is AccountsCardConfig -> card.copy(order = index)
                is CategoryBreakdownCardConfig -> card.copy(order = index)
                is CategoryExpenseTreemapCardConfig -> card.copy(order = index)
                is AccountHeroCardConfig -> card.copy(order = index)
                is PlannedPaymentsCardConfig -> card.copy(order = index)
                is BudgetsCardConfig -> card.copy(order = index)
                is LoansCardConfig -> card.copy(order = index)
            }
        }
        val updated = DashboardCardsConfig(cards = reorderedWithUpdatedOrder)
        settingsDataStore.saveAccountDetailCardsConfig(json.encodeToString(updated))
    }

    override suspend fun addAccountsViewCard(card: CardConfig) {
        val current = accountsViewCardsConfig.first()
        val updated = current.copy(cards = current.cards + card)
        settingsDataStore.saveAccountsViewCardsConfig(json.encodeToString(updated))
    }

    override suspend fun removeAccountsViewCard(cardId: String) {
        val current = accountsViewCardsConfig.first()
        val updated = current.copy(cards = current.cards.filter { it.cardId != cardId })
        settingsDataStore.saveAccountsViewCardsConfig(json.encodeToString(updated))
    }

    override suspend fun reorderAccountsViewCards(cards: List<CardConfig>) {
        // Update order property to match new positions for each card type
        val reorderedWithUpdatedOrder = cards.mapIndexed { index, card ->
            when (card) {
                is NetWorthCardConfig -> card.copy(order = index)
                is CurrencyBalancesCardConfig -> card.copy(order = index)
                is CashFlowCardConfig -> card.copy(order = index)
                is AccountsCardConfig -> card.copy(order = index)
                is CategoryBreakdownCardConfig -> card.copy(order = index)
                is CategoryExpenseTreemapCardConfig -> card.copy(order = index)
                is AccountHeroCardConfig -> card.copy(order = index)
                is PlannedPaymentsCardConfig -> card.copy(order = index)
                is BudgetsCardConfig -> card.copy(order = index)
                is LoansCardConfig -> card.copy(order = index)
            }
        }
        val updated = DashboardCardsConfig(cards = reorderedWithUpdatedOrder)
        settingsDataStore.saveAccountsViewCardsConfig(json.encodeToString(updated))
    }
}
