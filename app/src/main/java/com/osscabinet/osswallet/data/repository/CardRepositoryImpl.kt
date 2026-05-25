package com.osscabinet.ossWallet.data.repository

import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.settings.AccountsCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CardConfig
import com.osscabinet.ossWallet.domain.model.settings.CategoryPieChartCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CategoryTreemapCardConfig
import com.osscabinet.ossWallet.domain.model.settings.DashboardCardsConfig
import com.osscabinet.ossWallet.domain.model.settings.ScreenContext
import com.osscabinet.ossWallet.domain.model.settings.withOrder
import com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.repository.CardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardRepositoryImpl @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : CardRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private fun defaultDashboardConfig(): DashboardCardsConfig = DashboardCardsConfig(
        cards = listOf(
            AccountsCardConfig(
                cardId = "default_accounts_card",
                order = 0
            )
        )
    )

    private fun defaultAccountDetailConfig(): DashboardCardsConfig = DashboardCardsConfig(
        cards = listOf(
            com.osscabinet.ossWallet.domain.model.settings.AccountHeroCardConfig(
                cardId = "default_account_hero_card",
                order = 0
            )
        )
    )

    private fun defaultAnalyticsConfig(): DashboardCardsConfig = DashboardCardsConfig(
        cards = listOf(
            CategoryPieChartCardConfig(
                cardId = "analytics_category_breakdown",
                order = 0,
                title = "CATEGORY PIE CHART",
                filter = TransactionFilter(
                    timelineRange = TimelineRange.LastPeriod(LastPeriodType.LAST_30_DAYS),
                    transactionTypes = setOf(TransactionType.EXPENSE)
                ),
                showLabels = true,
                showLegend = true
            ),
            CategoryTreemapCardConfig(
                cardId = "analytics_category_expense_treemap",
                order = 1,
                title = "CATEGORY TREEMAP",
                filter = TransactionFilter(
                    timelineRange = TimelineRange.LastPeriod(LastPeriodType.LAST_30_DAYS),
                    transactionTypes = setOf(TransactionType.EXPENSE)
                ),
                showLabels = true
            )
        )
    )

    override fun getCardsConfig(context: ScreenContext): Flow<DashboardCardsConfig> =
        when (context) {
            ScreenContext.DASHBOARD -> dashboardCardsConfig
            ScreenContext.ACCOUNT_DETAIL -> accountDetailCardsConfig
            ScreenContext.ACCOUNTS_VIEW -> accountsViewCardsConfig
            ScreenContext.ANALYTICS -> analyticsCardsConfig
        }

    override suspend fun updateCard(
        context: ScreenContext,
        cardId: String,
        update: (CardConfig) -> CardConfig
    ) {
        val flow = getCardsConfig(context)
        val current = flow.first()
        val updated =
            current.copy(cards = current.cards.map { if (it.cardId == cardId) update(it) else it })
        saveConfig(context, updated)
    }

    override suspend fun addCard(context: ScreenContext, card: CardConfig) {
        val flow = getCardsConfig(context)
        val current = flow.first()

        // Find the maximum order and set new card's order to max + 1
        val maxOrder = current.cards.maxOfOrNull { it.order } ?: 0
        val cardWithOrder = card.withOrder(maxOrder + 1)

        val updated = current.copy(cards = current.cards + cardWithOrder)
        saveConfig(context, updated)
    }

    override suspend fun removeCard(context: ScreenContext, cardId: String) {
        val flow = getCardsConfig(context)
        val current = flow.first()
        val updated = current.copy(cards = current.cards.filter { it.cardId != cardId })
        saveConfig(context, updated)
    }

    override suspend fun reorderCards(context: ScreenContext, cards: List<CardConfig>) {
        val updated = DashboardCardsConfig(cards = withUpdatedOrder(cards))
        saveConfig(context, updated)
    }

    private suspend fun saveConfig(context: ScreenContext, config: DashboardCardsConfig) {
        val configToSave = config
        val encoded = json.encodeToString(configToSave)
        when (context) {
            ScreenContext.DASHBOARD -> settingsDataStore.saveDashboardCardsConfig(encoded)
            ScreenContext.ACCOUNT_DETAIL -> settingsDataStore.saveAccountDetailCardsConfig(encoded)
            ScreenContext.ACCOUNTS_VIEW -> settingsDataStore.saveAccountsViewCardsConfig(encoded)
            ScreenContext.ANALYTICS -> settingsDataStore.saveAnalyticsCardsConfig(encoded)
        }
    }

    // Legacy implementations mapping to new unified API
    override val dashboardCardsConfig: Flow<DashboardCardsConfig> =
        settingsDataStore.dashboardCardsConfig.map {
            if (it.isEmpty()) defaultDashboardConfig() else {
                try {
                    json.decodeFromString(it)
                } catch (e: Exception) {
                    defaultDashboardConfig()
                }
            }
        }

    override val accountDetailCardsConfig: Flow<DashboardCardsConfig> =
        settingsDataStore.accountDetailCardsConfig.map {
            if (it.isEmpty()) defaultAccountDetailConfig() else {
                try {
                    json.decodeFromString(it)
                } catch (e: Exception) {
                    defaultAccountDetailConfig()
                }
            }
        }

    override val accountsViewCardsConfig: Flow<DashboardCardsConfig> =
        settingsDataStore.accountsViewCardsConfig.map {
            if (it.isEmpty()) DashboardCardsConfig() else json.decodeFromString(it)
        }

    override val analyticsCardsConfig: Flow<DashboardCardsConfig> =
        settingsDataStore.analyticsCardsConfig.map {
            if (it.isEmpty()) defaultAnalyticsConfig() else {
                try {
                    json.decodeFromString(it)
                } catch (e: Exception) {
                    defaultAnalyticsConfig()
                }
            }
        }

    override suspend fun updateDashboardCard(cardId: String, update: (CardConfig) -> CardConfig) =
        updateCard(ScreenContext.DASHBOARD, cardId, update)

    override suspend fun updateAccountDetailCard(
        cardId: String,
        update: (CardConfig) -> CardConfig
    ) = updateCard(ScreenContext.ACCOUNT_DETAIL, cardId, update)

    override suspend fun updateAccountsViewCard(
        cardId: String,
        update: (CardConfig) -> CardConfig
    ) = updateCard(ScreenContext.ACCOUNTS_VIEW, cardId, update)

    override suspend fun updateAnalyticsCard(cardId: String, update: (CardConfig) -> CardConfig) =
        updateCard(ScreenContext.ANALYTICS, cardId, update)

    override suspend fun addDashboardCard(card: CardConfig) = addCard(ScreenContext.DASHBOARD, card)
    override suspend fun removeDashboardCard(cardId: String) =
        removeCard(ScreenContext.DASHBOARD, cardId)

    override suspend fun reorderDashboardCards(cards: List<CardConfig>) =
        reorderCards(ScreenContext.DASHBOARD, cards)

    override suspend fun addAccountDetailCard(card: CardConfig) =
        addCard(ScreenContext.ACCOUNT_DETAIL, card)

    override suspend fun removeAccountDetailCard(cardId: String) =
        removeCard(ScreenContext.ACCOUNT_DETAIL, cardId)

    override suspend fun reorderAccountDetailCards(cards: List<CardConfig>) =
        reorderCards(ScreenContext.ACCOUNT_DETAIL, cards)

    override suspend fun addAccountsViewCard(card: CardConfig) =
        addCard(ScreenContext.ACCOUNTS_VIEW, card)

    override suspend fun removeAccountsViewCard(cardId: String) =
        removeCard(ScreenContext.ACCOUNTS_VIEW, cardId)

    override suspend fun reorderAccountsViewCards(cards: List<CardConfig>) =
        reorderCards(ScreenContext.ACCOUNTS_VIEW, cards)

    override suspend fun addAnalyticsCard(card: CardConfig) = addCard(ScreenContext.ANALYTICS, card)
    override suspend fun removeAnalyticsCard(cardId: String) =
        removeCard(ScreenContext.ANALYTICS, cardId)

    override suspend fun reorderAnalyticsCards(cards: List<CardConfig>) =
        reorderCards(ScreenContext.ANALYTICS, cards)

    private fun withUpdatedOrder(cards: List<CardConfig>): List<CardConfig> =
        cards.mapIndexed { index, card ->
            card.withOrder(index + 1)
        }
}
