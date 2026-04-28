package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.CardRepository
import com.osscabinet.ossWallet.domain.repository.ComputationRepository
import com.osscabinet.ossWallet.domain.usecase.*
import com.osscabinet.ossWallet.domain.model.analytics.*
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.components.cards.computeCashFlow
import com.osscabinet.ossWallet.ui.components.cards.computeCurrencyTotals
import com.osscabinet.ossWallet.domain.model.analytics.ComputationContext
import com.osscabinet.ossWallet.domain.util.CachedCardData
import com.osscabinet.ossWallet.domain.service.CardComputationService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class AccountDetailViewModel(
    private val accountId: String,
    private val accountRepository: AccountRepository,
    private val transactionUseCases: TransactionUseCases,
    private val settingsRepository: SettingsRepository,
    private val cardRepository: CardRepository,
    private val computationRepository: ComputationRepository,
    private val cardComputationService: CardComputationService
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

    suspend fun computeCashFlowCard(
        cardConfig: com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig,
        transactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): CashFlowResult {
        val cacheKey = "cashflow_${cardConfig.cardId}_${cardConfig.timelineRange}"
        return getCachedOrCompute(cacheKey) {
            cardConfig.computeCashFlow(
                transactions = transactions,
                engine = cardComputationService,
                context = ComputationContext(baseCurrency, exchangeRates, precision)
            )
        }
    }

    suspend fun computeCurrencyBalancesCard(
        cardConfig: com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig,
        accounts: List<Account>,
        transactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): List<CurrencyAggregationItem> {
        val cacheKey = "currency_${cardConfig.cardId}_${cardConfig.timelineRange}"
        return getCachedOrCompute(cacheKey) {
            cardConfig.computeCurrencyTotals(
                accounts = accounts,
                transactions = transactions,
                engine = cardComputationService,
                context = ComputationContext(baseCurrency, exchangeRates, precision)
            )
        }
    }

    fun updateCurrencyBalancesLayout(layout: String) {
        viewModelScope.launch {
            settingsRepository.updateAccountDetailSettings { it.copy(currencyBalancesSection = it.currencyBalancesSection.copy(layout = layout)) }
        }
    }

    fun updateShowBaseConversion(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAccountDetailSettings { it.copy(currencyBalancesSection = it.currencyBalancesSection.copy(showBaseConversion = show)) }
        }
    }

    fun updateCurrencyBalancesTimelineRange(range: TimelineRange) {
        viewModelScope.launch {
            settingsRepository.updateAccountDetailSettings { it.copy(currencyBalancesSection = it.currencyBalancesSection.copy(timelineRange = range)) }
        }
    }

    fun updateCashFlowShowBaseConversion(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAccountDetailSettings { it.copy(cashFlowSection = it.cashFlowSection.copy(showBaseConversion = show)) }
        }
    }

    fun updateCashFlowTimelineRange(range: TimelineRange) {
        viewModelScope.launch {
            settingsRepository.updateAccountDetailSettings { it.copy(cashFlowSection = it.cashFlowSection.copy(timelineRange = range)) }
        }
    }

    fun updateShowPlannedPayments(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAccountDetailSettings { it.copy(transactionsSection = it.transactionsSection.copy(showPlannedPayments = show)) }
        }
    }

    fun updateUseNoteAsHeader(use: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAccountDetailSettings { it.copy(transactionsSection = it.transactionsSection.copy(useNoteAsHeader = use)) }
        }
    }

    // Card-specific update methods
    fun updateCardHeroShowCurrencyBadges(cardId: String, show: Boolean) {
        viewModelScope.launch {
            cardRepository.updateAccountDetailCard(cardId) { config ->
                if (config is com.osscabinet.ossWallet.domain.model.settings.AccountHeroCardConfig) {
                    config.copy(showCurrencyBadges = show)
                } else config
            }
            invalidateCardCache(cardId)
        }
    }

    fun updateCardCashFlowShowBaseConversion(cardId: String, show: Boolean) {
        viewModelScope.launch {
            cardRepository.updateAccountDetailCard(cardId) { config ->
                if (config is com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig) {
                    config.copy(showBaseConversion = show)
                } else config
            }
            invalidateCardCache(cardId)
        }
    }

    fun updateCardCashFlowTimelineRange(cardId: String, range: TimelineRange) {
        viewModelScope.launch {
            cardRepository.updateAccountDetailCard(cardId) { config ->
                if (config is com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig) {
                    config.copy(timelineRange = range)
                } else config
            }
            invalidateCardCache(cardId)
        }
    }

    fun updateCardCurrencyBalancesLayout(cardId: String, layout: String) {
        viewModelScope.launch {
            cardRepository.updateAccountDetailCard(cardId) { config ->
                if (config is com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig) {
                    config.copy(layout = layout)
                } else config
            }
            invalidateCardCache(cardId)
        }
    }

    fun updateCardCurrencyBalancesShowBaseConversion(cardId: String, show: Boolean) {
        viewModelScope.launch {
            cardRepository.updateAccountDetailCard(cardId) { config ->
                if (config is com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig) {
                    config.copy(showBaseConversion = show)
                } else config
            }
            invalidateCardCache(cardId)
        }
    }

    fun updateCardCurrencyBalancesTimelineRange(cardId: String, range: TimelineRange) {
        viewModelScope.launch {
            cardRepository.updateAccountDetailCard(cardId) { config ->
                if (config is com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig) {
                    config.copy(timelineRange = range)
                } else config
            }
            invalidateCardCache(cardId)
        }
    }

    fun removeAccountDetailCard(cardId: String) {
        viewModelScope.launch {
            cardRepository.updateAccountDetailCard(cardId) { it } // Mock update to trigger re-flow if needed, but repository should have a remove method
            // cardRepository.removeAccountDetailCard(cardId) // Assuming this exists or using update to remove
            invalidateCardCache(cardId)
        }
    }

    val uiState: StateFlow<AccountDetailUiState> = settingsRepository.currentProfileId
        .flatMapLatest { profileId ->
            combine(
                accountRepository.getAllAccounts(profileId),
                transactionUseCases.getTransactions(profileId),
                computationRepository.exchangeRates,
                computationRepository.selectedCurrency,
                computationRepository.hasLoadedExchangeRates,
                settingsRepository.decimalPrecision,
                settingsRepository.accountDetailSettings
            ) { args: Array<*> ->
                @Suppress("UNCHECKED_CAST")
                val accounts = args[0] as List<Account>
                @Suppress("UNCHECKED_CAST")
                val allTransactions = args[1] as List<Transaction>
                @Suppress("UNCHECKED_CAST")
                val rates = args[2] as Map<String, BigDecimal>
                val baseCurrency = args[3] as String
                val hasLoadedExchangeRates = args[4] as Boolean
                val precision = args[5] as Int
                val settings = args[6] as com.osscabinet.ossWallet.data.local.AccountDetailSettings

                // Invalidate cache when transactions change
                invalidateAllCache()
                
                val account = accounts.find { it.id == accountId } ?: return@combine AccountDetailUiState.Error("Account not found")

                val computationContext = ComputationContext(baseCurrency, rates, precision)

                var accountTransactions = allTransactions.filter { it.accountId == accountId }
                
                if (!hasLoadedExchangeRates) {
                    return@combine AccountDetailUiState.Loading
                }

                if (!settings.transactionsSection.showPlannedPayments) {
                    val now = System.currentTimeMillis()
                    accountTransactions = accountTransactions.filter { it.timestamp <= now }
                }

                val currencyTotals = cardComputationService.computeCurrencyTotals(
                    config = com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig(
                        cardId = "account_detail_currency_balances",
                        filter = com.osscabinet.ossWallet.domain.model.TransactionFilter(timelineRange = settings.currencyBalancesSection.timelineRange)
                    ),
                    accounts = listOf(account),
                    allTransactions = accountTransactions,
                    context = computationContext
                )

                val cashFlowResult = cardComputationService.computeCashFlow(
                    config = com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig(
                        cardId = "account_detail_cash_flow",
                        filter = com.osscabinet.ossWallet.domain.model.TransactionFilter(timelineRange = settings.cashFlowSection.timelineRange)
                    ),
                    allTransactions = accountTransactions,
                    context = computationContext
                )

                val (updatedAccounts, _) = cardComputationService.computeAccountStats(
                    config = com.osscabinet.ossWallet.domain.model.settings.AccountsCardConfig(
                        cardId = "account_detail_stats",
                        timelineRange = settings.currencyBalancesSection.timelineRange
                    ),
                    accounts = listOf(account),
                    allTransactions = accountTransactions,
                    context = computationContext
                )
                val updatedAccount = updatedAccounts.first()

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
