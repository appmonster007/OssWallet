package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.usecase.*
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.components.cards.computeCashFlow
import com.osscabinet.ossWallet.ui.components.cards.computeCurrencyTotals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

/**
 * Cache entry for computed card data with timestamp for expiration
 */
internal data class CachedCardData<T>(
    val timestamp: Long,
    val data: T
) {
    fun isExpired(expirationMinutes: Long = 5): Boolean {
        val elapsedMinutes = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - timestamp)
        return elapsedMinutes >= expirationMinutes
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AccountDetailViewModel(
    private val accountId: String,
    private val accountRepository: AccountRepository,
    private val transactionUseCases: TransactionUseCases,
    val settingsDataStore: SettingsDataStore,
    private val analyticsViewModel: com.osscabinet.ossWallet.ui.viewmodel.AnalyticsViewModel,
    private val financialUseCases: FinancialUseCases,
    private val cardManagerViewModel: CardManagerViewModel,
    private val cardComputationService: com.osscabinet.ossWallet.domain.service.CardComputationService
) : ViewModel() {

    // Cache for computed card data to avoid recomputation
    private val cardDataCache = mutableMapOf<String, CachedCardData<*>>()
    private val cacheExpirationMinutes = 5L

    /**
     * Get cached data if available and not expired, otherwise compute and cache
     */
    private inline fun <T> getCachedOrCompute(
        cacheKey: String,
        compute: () -> T
    ): T {
        val cached = cardDataCache[cacheKey]
        @Suppress("UNCHECKED_CAST")
        if (cached != null && !cached.isExpired(cacheExpirationMinutes)) {
            return cached.data as T
        }

        val result = compute()
        cardDataCache[cacheKey] = CachedCardData(System.currentTimeMillis(), result)
        return result
    }

    /**
     * Invalidate cache for a specific card
     */
    fun invalidateCardCache(cardId: String) {
        cardDataCache.keys.removeIf { it.startsWith(cardId) }
    }

    /**
     * Invalidate all cache (called when data changes significantly)
     */
    fun invalidateAllCache() {
        cardDataCache.clear()
    }

    // Flow-based card data computation for CashFlowCard
    private val _cashFlowCardData = MutableStateFlow<com.osscabinet.ossWallet.domain.usecase.CashFlowResult?>(null)
    val cashFlowCardData: StateFlow<com.osscabinet.ossWallet.domain.usecase.CashFlowResult?> = _cashFlowCardData.asStateFlow()

    fun computeCashFlowCard(cardConfig: com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig, transactions: List<Transaction>, baseCurrency: String, exchangeRates: Map<String, BigDecimal>, precision: Int) {
        viewModelScope.launch {
            val cacheKey = "cashflow_${cardConfig.cardId}_${cardConfig.timelineRange}"
            val result = getCachedOrCompute(cacheKey) {
                cardConfig.computeCashFlow(
                    transactions = transactions,
                    engine = cardComputationService,
                    baseCurrency = baseCurrency,
                    exchangeRates = exchangeRates,
                    precision = precision
                )
            }
            _cashFlowCardData.value = result
        }
    }

    // Flow-based card data computation for CurrencyBalancesCard
    private val _currencyBalancesCardData = MutableStateFlow<List<com.osscabinet.ossWallet.domain.usecase.CurrencyAggregationItem>?>(null)
    val currencyBalancesCardData: StateFlow<List<com.osscabinet.ossWallet.domain.usecase.CurrencyAggregationItem>?> = _currencyBalancesCardData.asStateFlow()

    fun computeCurrencyBalancesCard(cardConfig: com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig, accounts: List<Account>, transactions: List<Transaction>, baseCurrency: String, exchangeRates: Map<String, BigDecimal>, precision: Int) {
        viewModelScope.launch {
            val cacheKey = "currency_${cardConfig.cardId}_${cardConfig.timelineRange}"
            val result = getCachedOrCompute(cacheKey) {
                cardConfig.computeCurrencyTotals(
                    accounts = accounts,
                    transactions = transactions,
                    engine = cardComputationService,
                    baseCurrency = baseCurrency,
                    exchangeRates = exchangeRates,
                    precision = precision
                )
            }
            _currencyBalancesCardData.value = result
        }
    }

    fun updateCurrencyBalancesLayout(layout: String) {
        viewModelScope.launch {
            settingsDataStore.updateAccountDetailSettings { it.copy(currencyBalancesSection = it.currencyBalancesSection.copy(layout = layout)) }
        }
    }

    fun updateShowBaseConversion(show: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateAccountDetailSettings { it.copy(currencyBalancesSection = it.currencyBalancesSection.copy(showBaseConversion = show)) }
        }
    }

    fun updateCurrencyBalancesTimelineRange(range: TimelineRange) {
        viewModelScope.launch {
            settingsDataStore.updateAccountDetailSettings { it.copy(currencyBalancesSection = it.currencyBalancesSection.copy(timelineRange = range)) }
        }
    }

    fun updateCashFlowShowBaseConversion(show: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateAccountDetailSettings { it.copy(cashFlowSection = it.cashFlowSection.copy(showBaseConversion = show)) }
        }
    }

    fun updateCashFlowTimelineRange(range: TimelineRange) {
        viewModelScope.launch {
            settingsDataStore.updateAccountDetailSettings { it.copy(cashFlowSection = it.cashFlowSection.copy(timelineRange = range)) }
        }
    }

    fun updateShowPlannedPayments(show: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateAccountDetailSettings { it.copy(transactionsSection = it.transactionsSection.copy(showPlannedPayments = show)) }
        }
    }

    fun updateUseNoteAsHeader(use: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateAccountDetailSettings { it.copy(transactionsSection = it.transactionsSection.copy(useNoteAsHeader = use)) }
        }
    }

    // Card-specific update methods
    fun updateCardHeroShowCurrencyBadges(cardId: String, show: Boolean) {
        viewModelScope.launch {
            cardManagerViewModel.updateAccountDetailCard(cardId) { config ->
                if (config is com.osscabinet.ossWallet.domain.model.settings.AccountHeroCardConfig) {
                    config.copy(showCurrencyBadges = show)
                } else config
            }
            invalidateCardCache(cardId)
        }
    }

    fun updateCardCashFlowShowBaseConversion(cardId: String, show: Boolean) {
        viewModelScope.launch {
            cardManagerViewModel.updateAccountDetailCard(cardId) { config ->
                if (config is com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig) {
                    config.copy(showBaseConversion = show)
                } else config
            }
            invalidateCardCache(cardId)
        }
    }

    fun updateCardCashFlowTimelineRange(cardId: String, range: TimelineRange) {
        viewModelScope.launch {
            cardManagerViewModel.updateAccountDetailCard(cardId) { config ->
                if (config is com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig) {
                    config.copy(timelineRange = range)
                } else config
            }
            invalidateCardCache(cardId)
        }
    }

    fun updateCardCurrencyBalancesLayout(cardId: String, layout: String) {
        viewModelScope.launch {
            cardManagerViewModel.updateAccountDetailCard(cardId) { config ->
                if (config is com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig) {
                    config.copy(layout = layout)
                } else config
            }
            invalidateCardCache(cardId)
        }
    }

    fun updateCardCurrencyBalancesShowBaseConversion(cardId: String, show: Boolean) {
        viewModelScope.launch {
            cardManagerViewModel.updateAccountDetailCard(cardId) { config ->
                if (config is com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig) {
                    config.copy(showBaseConversion = show)
                } else config
            }
            invalidateCardCache(cardId)
        }
    }

    fun updateCardCurrencyBalancesTimelineRange(cardId: String, range: TimelineRange) {
        viewModelScope.launch {
            cardManagerViewModel.updateAccountDetailCard(cardId) { config ->
                if (config is com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig) {
                    config.copy(timelineRange = range)
                } else config
            }
            invalidateCardCache(cardId)
        }
    }

    fun removeAccountDetailCard(cardId: String) {
        viewModelScope.launch {
            cardManagerViewModel.removeAccountDetailCard(cardId)
            invalidateCardCache(cardId)
        }
    }

    // Card-specific compute methods now use unified extension functions from CardCompute.kt

    val uiState: StateFlow<AccountDetailUiState> = settingsDataStore.currentProfileId
        .flatMapLatest { profileId ->
            combine(
                accountRepository.getAllAccounts(profileId),
                transactionUseCases.getTransactions(profileId),
                analyticsViewModel.uiState,
                settingsDataStore.decimalPrecision,
                settingsDataStore.accountDetailSettings
            ) { accounts, allTransactions, analyticsState, precision, settings ->
                // Invalidate cache when transactions change
                invalidateAllCache()
                
                val account = accounts.find { it.id == accountId } ?: return@combine AccountDetailUiState.Error("Account not found")

                val rates = analyticsState.exchangeRates
                val baseCurrency = analyticsState.selectedCurrency

                var accountTransactions = allTransactions.filter { it.accountId == accountId }
                
                if (!analyticsState.hasLoadedExchangeRates) {
                    return@combine AccountDetailUiState.Loading
                }

                if (!settings.transactionsSection.showPlannedPayments) {
                    val now = System.currentTimeMillis()
                    accountTransactions = accountTransactions.filter { it.timestamp <= now }
                }

                val currencyTotals = financialUseCases.getCurrencyAggregation(
                    accounts = listOf(account),
                    allTransactions = accountTransactions,
                    timelineRange = settings.currencyBalancesSection.timelineRange,
                    baseCurrency = baseCurrency,
                    exchangeRates = rates,
                    precision = precision
                )

                val cashFlowResult = financialUseCases.calculateCashFlow(
                    allTransactions = accountTransactions,
                    timelineRange = settings.cashFlowSection.timelineRange,
                    baseCurrency = baseCurrency,
                    exchangeRates = rates,
                    precision = precision
                )

                val updatedAccount = financialUseCases.getTotalBalances(
                    accounts = listOf(account),
                    allTransactions = accountTransactions,
                    exchangeRates = rates,
                    precision = precision,
                    timelineRange = settings.currencyBalancesSection.timelineRange
                ).first()

                AccountDetailUiState.Success(
                    account = updatedAccount,
                    allAccountTransactions = accountTransactions,
                    currencyTotals = currencyTotals,
                    baseCurrency = baseCurrency,
                    exchangeRates = rates,
                    decimalPrecision = precision,
                    currencyBalancesLayout = settings.currencyBalancesSection.layout,
                    showBaseConversion = settings.currencyBalancesSection.showBaseConversion,
                    currencyBalancesTimelineRange = settings.currencyBalancesSection.timelineRange,
                    cashFlowShowBaseConversion = settings.cashFlowSection.showBaseConversion,
                    cashFlowTimelineRange = settings.cashFlowSection.timelineRange,
                    showPlannedPayments = settings.transactionsSection.showPlannedPayments,
                    useNoteAsHeader = settings.transactionsSection.useNoteAsHeader,
                    cashFlowResult = cashFlowResult
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AccountDetailUiState.Loading
        )
}

sealed class AccountDetailUiState {
    object Loading : AccountDetailUiState()
    data class Success(
        val account: Account,
        val allAccountTransactions: List<Transaction>,
        val currencyTotals: List<CurrencyAggregationItem> = emptyList(),
        val baseCurrency: String,
        val exchangeRates: Map<String, BigDecimal>,
        val decimalPrecision: Int,
        val currencyBalancesLayout: String,
        val showBaseConversion: Boolean,
        val currencyBalancesTimelineRange: TimelineRange,
        val cashFlowShowBaseConversion: Boolean,
        val cashFlowTimelineRange: TimelineRange,
        val showPlannedPayments: Boolean = false,
        val useNoteAsHeader: Boolean = false,
        val cashFlowResult: CashFlowResult? = null
    ) : AccountDetailUiState()
    data class Error(val message: String) : AccountDetailUiState()
}
