package com.osscabinet.ossWallet.data.repository

import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.domain.model.settings.*
import com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionType
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
        val configToSave = when (context) {
            // For ACCOUNT_DETAIL: strip AccountHeroCardConfig (always-present, not user-managed).
            ScreenContext.ACCOUNT_DETAIL -> config.copy(
                cards = config.cards
                    .filter { it !is AccountHeroCardConfig }
            )
            else -> config
        }
        val encoded = json.encodeToString(configToSave)
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
        val decoded: DashboardCardsConfig = if (it.isEmpty()) DashboardCardsConfig() else json.decodeFromString(it)
        // AccountHeroCard is always-present; strip it from the managed list so legacy data is cleaned.
        decoded.copy(cards = decoded.cards.filter { card -> card !is AccountHeroCardConfig })
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
                is CategoryPieChartCardConfig -> card.copy(order = index)
                is CategoryTreemapCardConfig -> card.copy(order = index)
                is AccountHeroCardConfig -> card.copy(order = index)
                is PlannedPaymentsCardConfig -> card.copy(order = index)
                is BudgetsCardConfig -> card.copy(order = index)
                is LoansCardConfig -> card.copy(order = index)
                is RecentActivitiesCardConfig -> card.copy(order = index)
                is CashFlowComparisonCardConfig -> card.copy(order = index)
                is IncomeExpenseBarCardConfig -> card.copy(order = index)
            }
        }
}
