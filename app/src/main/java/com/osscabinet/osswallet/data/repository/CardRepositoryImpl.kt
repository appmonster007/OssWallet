package com.osscabinet.ossWallet.data.repository

import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.domain.model.settings.CardConfig
import com.osscabinet.ossWallet.domain.model.settings.DashboardCardsConfig
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
        val updated = DashboardCardsConfig(cards = cards)
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
        val updated = DashboardCardsConfig(cards = cards)
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
        val updated = DashboardCardsConfig(cards = cards)
        settingsDataStore.saveAccountsViewCardsConfig(json.encodeToString(updated))
    }
}
