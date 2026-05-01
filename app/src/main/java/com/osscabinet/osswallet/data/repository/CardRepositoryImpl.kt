package com.osscabinet.ossWallet.data.repository

import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.domain.model.settings.*
import com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.model.TransactionFilter
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

    private fun defaultAnalyticsConfig(): DashboardCardsConfig = DashboardCardsConfig(
        cards = listOf(
            CategoryBreakdownCardConfig(
                cardId = "analytics_category_breakdown",
                order = 0,
                title = "CATEGORY BREAKDOWN",
                filter = TransactionFilter(timelineRange = TimelineRange.LastPeriod(LastPeriodType.LAST_30_DAYS)),
                showLabels = true,
                showLegend = true
            ),
            CategoryExpenseTreemapCardConfig(
                cardId = "analytics_category_expense_treemap",
                order = 1,
                title = "CATEGORY EXPENSES",
                filter = TransactionFilter(timelineRange = TimelineRange.LastPeriod(LastPeriodType.LAST_30_DAYS)),
                showLabels = true
            )
        )
    )

    override fun getCardsConfig(context: ScreenContext): Flow<DashboardCardsConfig> = when (context) {
        ScreenContext.DASHBOARD -> dashboardCardsConfig
        ScreenContext.ACCOUNT_DETAIL -> accountDetailCardsConfig
        ScreenContext.ACCOUNTS_VIEW -> accountsViewCardsConfig
        ScreenContext.ANALYTICS -> analyticsCardsConfig
    }

    override suspend fun updateCard(context: ScreenContext, cardId: String, update: (CardConfig) -> CardConfig) {
        val flow = getCardsConfig(context)
        val current = flow.first()
        val updated = current.copy(cards = current.cards.map { if (it.cardId == cardId) update(it) else it })
        saveConfig(context, updated)
    }

    override suspend fun addCard(context: ScreenContext, card: CardConfig) {
        val flow = getCardsConfig(context)
        val current = flow.first()
        val updated = current.copy(cards = current.cards + card)
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
        val encoded = json.encodeToString(config)
        when (context) {
            ScreenContext.DASHBOARD -> settingsDataStore.saveDashboardCardsConfig(encoded)
            ScreenContext.ACCOUNT_DETAIL -> settingsDataStore.saveAccountDetailCardsConfig(encoded)
            ScreenContext.ACCOUNTS_VIEW -> settingsDataStore.saveAccountsViewCardsConfig(encoded)
            ScreenContext.ANALYTICS -> settingsDataStore.saveAnalyticsCardsConfig(encoded)
        }
    }

    // Legacy implementations mapping to new unified API
    override val dashboardCardsConfig: Flow<DashboardCardsConfig> = settingsDataStore.dashboardCardsConfig.map {
        if (it.isEmpty()) DashboardCardsConfig() else json.decodeFromString(it)
    }

    override val accountDetailCardsConfig: Flow<DashboardCardsConfig> = settingsDataStore.accountDetailCardsConfig.map {
        if (it.isEmpty()) DashboardCardsConfig() else json.decodeFromString(it)
    }

    override val accountsViewCardsConfig: Flow<DashboardCardsConfig> = settingsDataStore.accountsViewCardsConfig.map {
        if (it.isEmpty()) DashboardCardsConfig() else json.decodeFromString(it)
    }

    override val analyticsCardsConfig: Flow<DashboardCardsConfig> = settingsDataStore.analyticsCardsConfig.map {
        if (it.isEmpty()) defaultAnalyticsConfig() else {
            try { json.decodeFromString(it) } catch (e: Exception) { defaultAnalyticsConfig() }
        }
    }

    override suspend fun updateDashboardCard(cardId: String, update: (CardConfig) -> CardConfig) = updateCard(ScreenContext.DASHBOARD, cardId, update)
    override suspend fun updateAccountDetailCard(cardId: String, update: (CardConfig) -> CardConfig) = updateCard(ScreenContext.ACCOUNT_DETAIL, cardId, update)
    override suspend fun updateAccountsViewCard(cardId: String, update: (CardConfig) -> CardConfig) = updateCard(ScreenContext.ACCOUNTS_VIEW, cardId, update)
    override suspend fun updateAnalyticsCard(cardId: String, update: (CardConfig) -> CardConfig) = updateCard(ScreenContext.ANALYTICS, cardId, update)

    override suspend fun addDashboardCard(card: CardConfig) = addCard(ScreenContext.DASHBOARD, card)
    override suspend fun removeDashboardCard(cardId: String) = removeCard(ScreenContext.DASHBOARD, cardId)
    override suspend fun reorderDashboardCards(cards: List<CardConfig>) = reorderCards(ScreenContext.DASHBOARD, cards)

    override suspend fun addAccountDetailCard(card: CardConfig) = addCard(ScreenContext.ACCOUNT_DETAIL, card)
    override suspend fun removeAccountDetailCard(cardId: String) = removeCard(ScreenContext.ACCOUNT_DETAIL, cardId)
    override suspend fun reorderAccountDetailCards(cards: List<CardConfig>) = reorderCards(ScreenContext.ACCOUNT_DETAIL, cards)

    override suspend fun addAccountsViewCard(card: CardConfig) = addCard(ScreenContext.ACCOUNTS_VIEW, card)
    override suspend fun removeAccountsViewCard(cardId: String) = removeCard(ScreenContext.ACCOUNTS_VIEW, cardId)
    override suspend fun reorderAccountsViewCards(cards: List<CardConfig>) = reorderCards(ScreenContext.ACCOUNTS_VIEW, cards)

    override suspend fun addAnalyticsCard(card: CardConfig) = addCard(ScreenContext.ANALYTICS, card)
    override suspend fun removeAnalyticsCard(cardId: String) = removeCard(ScreenContext.ANALYTICS, cardId)
    override suspend fun reorderAnalyticsCards(cards: List<CardConfig>) = reorderCards(ScreenContext.ANALYTICS, cards)

    private fun withUpdatedOrder(cards: List<CardConfig>): List<CardConfig> =
        cards.mapIndexed { index, card ->
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
}
