package com.osscabinet.ossWallet.domain.repository

import com.osscabinet.ossWallet.domain.model.settings.CardConfig
import com.osscabinet.ossWallet.domain.model.settings.DashboardCardsConfig
import kotlinx.coroutines.flow.Flow

interface CardRepository {
    val dashboardCardsConfig: Flow<DashboardCardsConfig>
    val accountDetailCardsConfig: Flow<DashboardCardsConfig>
    val accountsViewCardsConfig: Flow<DashboardCardsConfig>
    
    suspend fun updateDashboardCard(cardId: String, update: (CardConfig) -> CardConfig)
    suspend fun updateAccountDetailCard(cardId: String, update: (CardConfig) -> CardConfig)
    suspend fun updateAccountsViewCard(cardId: String, update: (CardConfig) -> CardConfig)
    
    suspend fun addDashboardCard(card: CardConfig)
    suspend fun removeDashboardCard(cardId: String)
    suspend fun reorderDashboardCards(cards: List<CardConfig>)

    suspend fun addAccountDetailCard(card: CardConfig)
    suspend fun removeAccountDetailCard(cardId: String)
    suspend fun reorderAccountDetailCards(cards: List<CardConfig>)

    suspend fun addAccountsViewCard(card: CardConfig)
    suspend fun removeAccountsViewCard(cardId: String)
    suspend fun reorderAccountsViewCards(cards: List<CardConfig>)
}
