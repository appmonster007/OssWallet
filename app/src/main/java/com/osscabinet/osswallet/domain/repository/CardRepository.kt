package com.osscabinet.ossWallet.domain.repository

import com.osscabinet.ossWallet.domain.model.settings.CardConfig
import com.osscabinet.ossWallet.domain.model.settings.DashboardCardsConfig
import com.osscabinet.ossWallet.domain.model.settings.ScreenContext
import kotlinx.coroutines.flow.Flow

interface CardRepository {
    // Legacy flows - will be deprecated after migration
    val dashboardCardsConfig: Flow<DashboardCardsConfig>
    val accountDetailCardsConfig: Flow<DashboardCardsConfig>
    val accountsViewCardsConfig: Flow<DashboardCardsConfig>
    val analyticsCardsConfig: Flow<DashboardCardsConfig>

    // New unified API
    fun getCardsConfig(context: ScreenContext): Flow<DashboardCardsConfig>
    suspend fun updateCard(context: ScreenContext, cardId: String, update: (CardConfig) -> CardConfig)
    suspend fun addCard(context: ScreenContext, card: CardConfig)
    suspend fun removeCard(context: ScreenContext, cardId: String)
    suspend fun reorderCards(context: ScreenContext, cards: List<CardConfig>)

    // Legacy update methods
    suspend fun updateDashboardCard(cardId: String, update: (CardConfig) -> CardConfig)
    suspend fun updateAccountDetailCard(cardId: String, update: (CardConfig) -> CardConfig)
    suspend fun updateAccountsViewCard(cardId: String, update: (CardConfig) -> CardConfig)
    suspend fun updateAnalyticsCard(cardId: String, update: (CardConfig) -> CardConfig)

    suspend fun addDashboardCard(card: CardConfig)
    suspend fun removeDashboardCard(cardId: String)
    suspend fun reorderDashboardCards(cards: List<CardConfig>)

    suspend fun addAccountDetailCard(card: CardConfig)
    suspend fun removeAccountDetailCard(cardId: String)
    suspend fun reorderAccountDetailCards(cards: List<CardConfig>)

    suspend fun addAccountsViewCard(card: CardConfig)
    suspend fun removeAccountsViewCard(cardId: String)
    suspend fun reorderAccountsViewCards(cards: List<CardConfig>)

    suspend fun addAnalyticsCard(card: CardConfig)
    suspend fun removeAnalyticsCard(cardId: String)
    suspend fun reorderAnalyticsCards(cards: List<CardConfig>)
}
