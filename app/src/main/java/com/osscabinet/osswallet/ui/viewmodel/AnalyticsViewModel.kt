package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.card.*
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.domain.model.analytics.*
import com.osscabinet.ossWallet.domain.model.settings.*
import com.osscabinet.ossWallet.domain.repository.*
import com.osscabinet.ossWallet.domain.service.*
import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
import com.osscabinet.ossWallet.domain.util.CardDataCache
import com.osscabinet.ossWallet.ui.analytics.components.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModel(
    private val transactionUseCases: TransactionUseCases,
    private val settingsRepository: SettingsRepository,
    private val computationRepository: ComputationRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository
) : ViewModel(), CardUpdateHandler {

    private val cardDataCache = CardDataCache()

    // Analytics cards config
    private val _analyticsCardsConfig = MutableStateFlow(defaultAnalyticsCardsConfig())
    val analyticsCardsConfig: StateFlow<AnalyticsCardsConfig> = _analyticsCardsConfig.asStateFlow()

    init {
        // Load analytics cards config from DataStore on initialization
        viewModelScope.launch {
            settingsRepository.analyticsCardsConfig.collect { configStr ->
                if (configStr.isEmpty()) {
                    _analyticsCardsConfig.value = defaultAnalyticsCardsConfig()
                } else {
                    try {
                        _analyticsCardsConfig.value = Json.decodeFromString<AnalyticsCardsConfig>(configStr)
                    } catch (e: Exception) {
                        _analyticsCardsConfig.value = defaultAnalyticsCardsConfig()
                    }
                }
            }
        }
    }

    val uiState: StateFlow<AnalyticsUiState> = settingsRepository.currentProfileId
        .flatMapLatest { profileId ->
            combine(
                transactionUseCases.getTransactions(profileId),
                accountRepository.getAllAccounts(profileId),
                categoryRepository.categories,
                computationRepository.exchangeRates,
                computationRepository.selectedCurrency,
                computationRepository.hasLoadedExchangeRates,
                settingsRepository.decimalPrecision
            ) { args: Array<Any> ->
                @Suppress("UNCHECKED_CAST")
                val transactions = args[0] as List<Transaction>
                @Suppress("UNCHECKED_CAST")
                val accounts = args[1] as List<Account>
                @Suppress("UNCHECKED_CAST")
                val categories = args[2] as List<Category>
                @Suppress("UNCHECKED_CAST")
                val rates = args[3] as Map<String, BigDecimal>
                val baseCurrency = args[4] as String
                val hasLoadedRates = args[5] as Boolean
                val precision = args[6] as Int

                if (!hasLoadedRates) {
                    return@combine AnalyticsUiState.Loading
                }

                // Invalidate cache when transactions change
                cardDataCache.invalidateAll()

                AnalyticsUiState.Success(
                    allTransactions = transactions,
                    accounts = accounts,
                    allCategories = categories,
                    exchangeRates = rates,
                    selectedCurrency = baseCurrency,
                    decimalPrecision = precision
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AnalyticsUiState.Loading
        )

    // Card management methods
    fun addCard(cardConfig: CardConfig) {
        viewModelScope.launch {
            _analyticsCardsConfig.value = _analyticsCardsConfig.value.copy(
                cards = _analyticsCardsConfig.value.cards + cardConfig
            )
            settingsRepository.saveAnalyticsCardsConfig(Json.encodeToString(_analyticsCardsConfig.value))
        }
    }

    fun removeCard(cardId: String) {
        viewModelScope.launch {
            _analyticsCardsConfig.value = _analyticsCardsConfig.value.copy(
                cards = _analyticsCardsConfig.value.cards.filter { it.cardId != cardId }
            )
            settingsRepository.saveAnalyticsCardsConfig(Json.encodeToString(_analyticsCardsConfig.value))
        }
    }

    private fun updateCardConfigById(cardId: String, update: (CardConfig) -> CardConfig) {
        viewModelScope.launch {
            _analyticsCardsConfig.value = _analyticsCardsConfig.value.copy(
                cards = _analyticsCardsConfig.value.cards.map { card ->
                    if (card.cardId == cardId) update(card) else card
                }
            )
            settingsRepository.saveAnalyticsCardsConfig(Json.encodeToString(_analyticsCardsConfig.value))
        }
    }

    override fun updateCardCategoryBreakdownConfig(cardId: String, config: CategoryBreakdownCardConfig) {
        updateCardConfigById(cardId) { config }
    }

    override fun updateCardCategoryExpenseTreemapConfig(cardId: String, config: CategoryExpenseTreemapCardConfig) {
        updateCardConfigById(cardId) { config }
    }

    override fun updateCardNetWorthFilter(cardId: String, filter: TransactionFilter) {
        updateCardConfigById(cardId) { config ->
            if (config is NetWorthCardConfig) config.copy(filter = filter) else config
        }
    }

    override fun updateCardCurrencyBalancesFilter(cardId: String, filter: TransactionFilter) {
        updateCardConfigById(cardId) { config ->
            if (config is CurrencyBalancesCardConfig) config.copy(filter = filter) else config
        }
    }

    override fun updateCardCashFlowFilter(cardId: String, filter: TransactionFilter) {
        updateCardConfigById(cardId) { config ->
            if (config is CashFlowCardConfig) config.copy(filter = filter) else config
        }
    }

    override fun updateCardAccountsFilter(cardId: String, filter: TransactionFilter) {
        updateCardConfigById(cardId) { config ->
            if (config is AccountsCardConfig) config.copy(filter = filter) else config
        }
    }

    fun reorderCards(newOrder: List<CardConfig>) {
        viewModelScope.launch {
            // Update order property to match new positions
            val reorderedWithUpdatedOrder = newOrder.mapIndexed { index, card ->
                when (card) {
                    is NetWorthCardConfig -> card.copy(order = index)
                    is CurrencyBalancesCardConfig -> card.copy(order = index)
                    is CashFlowCardConfig -> card.copy(order = index)
                    is AccountsCardConfig -> card.copy(order = index)
                    is CategoryBreakdownCardConfig -> card.copy(order = index)
                    is CategoryExpenseTreemapCardConfig -> card.copy(order = index)
                    is AccountHeroCardConfig -> card.copy(order = index)
                }
            }
            _analyticsCardsConfig.value = AnalyticsCardsConfig(reorderedWithUpdatedOrder)
            settingsRepository.saveAnalyticsCardsConfig(Json.encodeToString(_analyticsCardsConfig.value))
        }
    }
}

fun defaultAnalyticsCardsConfig(): AnalyticsCardsConfig {
    return AnalyticsCardsConfig(
        cards = listOf(
            CategoryBreakdownCardConfig(
                cardId = "analytics_category_breakdown",
                order = 0,
                title = "CATEGORY BREAKDOWN",
                filter = TransactionFilter(timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_30_DAYS)),
                showLabels = true,
                showLegend = true
            ),
            CategoryExpenseTreemapCardConfig(
                cardId = "analytics_category_expense_treemap",
                order = 1,
                title = "CATEGORY EXPENSES",
                filter = TransactionFilter(timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_30_DAYS)),
                showLabels = true
            )
        )
    )
}

sealed class AnalyticsUiState {
    object Loading : AnalyticsUiState()
    data class Success(
        val allTransactions: List<Transaction> = emptyList(),
        val accounts: List<Account> = emptyList(),
        val allCategories: List<Category> = emptyList(),
        val selectedCurrency: String = "USD",
        val exchangeRates: Map<String, BigDecimal> = emptyMap(),
        val decimalPrecision: Int = 2
    ) : AnalyticsUiState()
    data class Error(val message: String) : AnalyticsUiState()
}
