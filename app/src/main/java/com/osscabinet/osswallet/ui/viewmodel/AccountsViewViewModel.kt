package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.util.CardDataCache
import com.osscabinet.ossWallet.domain.util.CachedCardData
import com.osscabinet.ossWallet.domain.util.splitByComma
import com.osscabinet.ossWallet.domain.util.joinByComma
import com.osscabinet.ossWallet.domain.util.toEnum
import com.osscabinet.ossWallet.domain.model.analytics.ComputationContext
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.CardRepository
import com.osscabinet.ossWallet.domain.repository.ComputationRepository
import com.osscabinet.ossWallet.domain.usecase.*
import com.osscabinet.ossWallet.domain.model.analytics.*
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.model.analytics.CurrencyAggregationItem
import com.osscabinet.ossWallet.domain.model.analytics.AccountSummaryItem
import com.osscabinet.ossWallet.domain.model.analytics.CashFlowResult
import com.osscabinet.ossWallet.ui.navigation.AccountsFilterContext
import com.osscabinet.ossWallet.ui.components.cards.computeNetWorth
import com.osscabinet.ossWallet.ui.components.cards.computeCurrencyTotals
import com.osscabinet.ossWallet.ui.components.cards.computeCashFlow
import com.osscabinet.ossWallet.ui.components.cards.computeAccountStats
import com.osscabinet.ossWallet.domain.service.CardComputationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal

enum class AccountsViewSection {
    NET_WORTH,
    CURRENCY_BALANCES,
    CASHFLOW,
    ACCOUNTS_LIST
}

@OptIn(ExperimentalCoroutinesApi::class)
class AccountsViewViewModel(
    private val accountRepository: AccountRepository,
    private val transactionUseCases: TransactionUseCases,
    private val settingsRepository: SettingsRepository,
    private val cardRepository: CardRepository,
    private val computationRepository: ComputationRepository,
    private val cardComputationService: CardComputationService
) : ViewModel() {

    private val cardDataCache = CardDataCache()

    suspend fun computeNetWorthCard(
        cardConfig: com.osscabinet.ossWallet.domain.model.settings.NetWorthCardConfig,
        accounts: List<Account>,
        transactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): Pair<BigDecimal, CashFlowResult> {
        val cacheKey = "networth_${cardConfig.cardId}_${cardConfig.timelineRange}"
        return cardDataCache.getOrCompute(cacheKey) {
            cardConfig.computeNetWorth(
                accounts = accounts,
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
        return cardDataCache.getOrCompute(cacheKey) {
            cardConfig.computeCurrencyTotals(
                accounts = accounts,
                transactions = transactions,
                engine = cardComputationService,
                context = ComputationContext(baseCurrency, exchangeRates, precision)
            )
        }
    }

    suspend fun computeCashFlowCard(
        cardConfig: com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig,
        transactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): CashFlowResult {
        val cacheKey = "cashflow_${cardConfig.cardId}_${cardConfig.timelineRange}"
        return cardDataCache.getOrCompute(cacheKey) {
            cardConfig.computeCashFlow(
                transactions = transactions,
                engine = cardComputationService,
                context = ComputationContext(baseCurrency, exchangeRates, precision)
            )
        }
    }

    suspend fun computeAccountsCard(
        cardConfig: com.osscabinet.ossWallet.domain.model.settings.AccountsCardConfig,
        accounts: List<Account>,
        transactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): Pair<List<Account>, Map<String, AccountSummaryItem>> {
        val cacheKey = "accounts_${cardConfig.cardId}_${cardConfig.timelineRange}"
        return cardDataCache.getOrCompute(cacheKey) {
            cardConfig.computeAccountStats(
                accounts = accounts,
                transactions = transactions,
                engine = cardComputationService,
                context = ComputationContext(baseCurrency, exchangeRates, precision)
            )
        }
    }

    private val _sectionOrder = MutableStateFlow(listOf(AccountsViewSection.NET_WORTH, AccountsViewSection.CURRENCY_BALANCES, AccountsViewSection.CASHFLOW, AccountsViewSection.ACCOUNTS_LIST))
    val sectionOrder: StateFlow<List<AccountsViewSection>> = _sectionOrder.asStateFlow()

    // Filter state for accounts
    private val _initialFilterContext = MutableStateFlow<AccountsFilterContext>(AccountsFilterContext.None)
    val initialFilterContext: StateFlow<AccountsFilterContext> = _initialFilterContext.asStateFlow()

    data class AccountFilter(
        val accountTypes: List<String> = emptyList(),
        val currencies: List<String> = emptyList(),
        val minBalance: BigDecimal? = null,
        val maxBalance: BigDecimal? = null,
        val includeHidden: Boolean = true
    )

    private val _accountFilter = MutableStateFlow(AccountFilter())
    val accountFilter: StateFlow<AccountFilter> = _accountFilter.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.accountsViewSettings.collect { settings ->
                val savedOrder = settings.layout.sectionOrder
                if (savedOrder != null) {
                    try {
                        val sections = savedOrder.splitByComma().map { it.toEnum<AccountsViewSection>() }
                        _sectionOrder.value = sections
                    } catch (e: Exception) {
                        // Keep default order on error
                    }
                }
            }
        }
    }

    fun updateSectionOrder(newOrder: List<AccountsViewSection>) {
        _sectionOrder.value = newOrder
        viewModelScope.launch {
            settingsRepository.updateAccountsViewSettings { it.copy(layout = it.layout.copy(sectionOrder = newOrder.joinByComma { it.name })) }
        }
    }

    fun updateGridColumns(columns: Int) {
        viewModelScope.launch {
            settingsRepository.updateAccountsViewSettings { it.copy(accountsSection = it.accountsSection.copy(gridColumns = columns)) }
        }
    }

    fun updateCurrencyBalancesLayout(layout: String) {
        viewModelScope.launch {
            settingsRepository.updateAccountsViewSettings { it.copy(currencyBalancesSection = it.currencyBalancesSection.copy(layout = layout)) }
        }
    }

    fun updateShowBaseConversion(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAccountsViewSettings { it.copy(currencyBalancesSection = it.currencyBalancesSection.copy(showBaseConversion = show)) }
        }
    }

    fun updateCurrencyBalancesTimelineRange(range: TimelineRange) {
        viewModelScope.launch {
            settingsRepository.updateAccountsViewSettings { it.copy(currencyBalancesSection = it.currencyBalancesSection.copy(timelineRange = range)) }
        }
    }

    fun updateNetWorthFontSize(fontSize: String) {
        viewModelScope.launch {
            settingsRepository.updateAccountsViewSettings { it.copy(netWorthSection = it.netWorthSection.copy(fontSize = fontSize)) }
        }
    }

    fun updateNetWorthTimelineRange(range: TimelineRange) {
        viewModelScope.launch {
            settingsRepository.updateAccountsViewSettings { it.copy(netWorthSection = it.netWorthSection.copy(timelineRange = range)) }
        }
    }

    fun updateCashFlowShowBaseConversion(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAccountsViewSettings { it.copy(cashFlowSection = it.cashFlowSection.copy(showBaseConversion = show)) }
        }
    }

    fun initializeAccountsFilter(context: AccountsFilterContext) {
        _initialFilterContext.value = context
        val filter = when (context) {
            is AccountsFilterContext.None -> AccountFilter()
            is AccountsFilterContext.ByType -> AccountFilter(accountTypes = listOf(context.accountType))
            is AccountsFilterContext.ByCurrency -> AccountFilter(currencies = listOf(context.currency))
            is AccountsFilterContext.Custom -> AccountFilter(
                accountTypes = context.accountTypes,
                currencies = context.currencies,
                minBalance = context.minBalance?.let { BigDecimal(it) },
                maxBalance = context.maxBalance?.let { BigDecimal(it) },
                includeHidden = context.includeHidden
            )
        }
        _accountFilter.value = filter
    }

    fun updateAccountsFilter(filter: AccountFilter) {
        _accountFilter.value = filter
    }

    fun resetToInitialFilter() {
        initializeAccountsFilter(_initialFilterContext.value)
    }

    fun hasModifiedFilter(): Boolean {
        val initialFilter = when (val context = _initialFilterContext.value) {
            is AccountsFilterContext.None -> AccountFilter()
            is AccountsFilterContext.ByType -> AccountFilter(accountTypes = listOf(context.accountType))
            is AccountsFilterContext.ByCurrency -> AccountFilter(currencies = listOf(context.currency))
            is AccountsFilterContext.Custom -> {
                AccountFilter(
                    accountTypes = context.accountTypes,
                    currencies = context.currencies,
                    minBalance = context.minBalance?.let { BigDecimal(it) },
                    maxBalance = context.maxBalance?.let { BigDecimal(it) },
                    includeHidden = context.includeHidden
                )
            }
        }
        return _accountFilter.value != initialFilter
    }

    fun clearAccountsFilter() {
        _accountFilter.value = AccountFilter()
        _initialFilterContext.value = AccountsFilterContext.None
    }

    fun updateCashFlowTimelineRange(range: TimelineRange) {
        viewModelScope.launch {
            settingsRepository.updateAccountsViewSettings { it.copy(cashFlowSection = it.cashFlowSection.copy(timelineRange = range)) }
        }
    }

    fun updateAccountsLayout(layout: String) {
        viewModelScope.launch {
            settingsRepository.updateAccountsViewSettings { it.copy(accountsSection = it.accountsSection.copy(layout = layout)) }
        }
    }

    fun updateShowAddAccountCard(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAccountsViewSettings { it.copy(accountsSection = it.accountsSection.copy(showAddAccountCard = show)) }
        }
    }

    fun updateAccountsTimelineRange(range: TimelineRange) {
        viewModelScope.launch {
            settingsRepository.updateAccountsViewSettings { it.copy(accountsSection = it.accountsSection.copy(timelineRange = range)) }
        }
    }

    val uiState: StateFlow<AccountsViewUiState> = settingsRepository.currentProfileId
        .flatMapLatest { profileId ->
            combine(
                accountRepository.getAllAccounts(profileId),
                transactionUseCases.getTransactions(profileId),
                computationRepository.exchangeRates,
                computationRepository.selectedCurrency,
                computationRepository.hasLoadedExchangeRates,
                settingsRepository.decimalPrecision,
                settingsRepository.accountsViewSettings
            ) { args: Array<*> ->
                @Suppress("UNCHECKED_CAST")
                val accounts = args[0] as List<Account>
                @Suppress("UNCHECKED_CAST")
                val allTransactions = args[1] as List<Transaction>
                @Suppress("UNCHECKED_CAST")
                val rates = args[2] as Map<String, BigDecimal>
                val baseCurrency = args[3] as String
                val hasLoadedRates = args[4] as Boolean
                val precision = args[5] as Int
                val accountsViewSettings = args[6] as com.osscabinet.ossWallet.data.local.AccountsViewSettings

                // Invalidate cache when transactions change
                cardDataCache.invalidateAll()
                
                if (!hasLoadedRates) {
                    return@combine AccountsViewUiState.Loading
                }

                val gridColumns = accountsViewSettings.accountsSection.gridColumns
                val accountsLayout = accountsViewSettings.accountsSection.layout
                val showAddAccountCard = accountsViewSettings.accountsSection.showAddAccountCard
                val accountsTimelineRange = accountsViewSettings.accountsSection.timelineRange
                val currencyBalancesLayout = accountsViewSettings.currencyBalancesSection.layout
                val showBaseConversion = accountsViewSettings.currencyBalancesSection.showBaseConversion
                val currencyBalancesTimelineRange = accountsViewSettings.currencyBalancesSection.timelineRange
                val cashFlowShowBaseConversion = accountsViewSettings.cashFlowSection.showBaseConversion
                val cashFlowTimelineRange = accountsViewSettings.cashFlowSection.timelineRange
                val netWorthFontSize = accountsViewSettings.netWorthSection.fontSize
                val netWorthTimelineRange = accountsViewSettings.netWorthSection.timelineRange

                val computationContext = ComputationContext(baseCurrency, rates, precision)

                val globalCurrencyTotals = cardComputationService.computeCurrencyTotals(
                    config = com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig(
                        cardId = "accounts_view_currency_balances",
                        filter = com.osscabinet.ossWallet.domain.model.TransactionFilter(timelineRange = currencyBalancesTimelineRange)
                    ),
                    accounts = accounts,
                    allTransactions = allTransactions,
                    context = computationContext
                )

                val (updatedAccounts, accountStats) = cardComputationService.computeAccountStats(
                    config = com.osscabinet.ossWallet.domain.model.settings.AccountsCardConfig(
                        cardId = "accounts_view_stats",
                        timelineRange = accountsTimelineRange
                    ),
                    accounts = accounts,
                    allTransactions = allTransactions,
                    context = computationContext
                )

                val (nwValue, nwFlow) = cardComputationService.computeNetWorth(
                    config = com.osscabinet.ossWallet.domain.model.settings.NetWorthCardConfig(
                        cardId = "accounts_view_net_worth",
                        timelineRange = netWorthTimelineRange
                    ),
                    accounts = accounts,
                    allTransactions = allTransactions,
                    context = computationContext
                )

                val cashFlowResult = cardComputationService.computeCashFlow(
                    config = com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig(
                        cardId = "accounts_view_cash_flow",
                        filter = com.osscabinet.ossWallet.domain.model.TransactionFilter(timelineRange = cashFlowTimelineRange)
                    ),
                    allTransactions = allTransactions,
                    context = computationContext
                )

                val nonArchivedAccounts = updatedAccounts.filter { !it.isArchived }

                AccountsViewUiState.Success(
                    accounts = nonArchivedAccounts,
                    allTransactions = allTransactions,
                    baseCurrency = baseCurrency,
                    exchangeRates = rates,
                    currencyTotals = globalCurrencyTotals,
                    decimalPrecision = precision,
                    accountStats = accountStats,
                    gridColumns = gridColumns,
                    accountsLayout = accountsLayout,
                    showAddAccountCard = showAddAccountCard,
                    accountsTimelineRange = accountsTimelineRange,
                    currencyBalancesLayout = currencyBalancesLayout,
                    showBaseConversion = showBaseConversion,
                    currencyBalancesTimelineRange = currencyBalancesTimelineRange,
                    cashFlowShowBaseConversion = cashFlowShowBaseConversion,
                    cashFlowTimelineRange = cashFlowTimelineRange,
                    netWorth = nwValue,
                    netIncome = nwFlow.totalIncomeInBase,
                    netExpense = nwFlow.totalExpensesInBase,
                    netWorthFontSize = netWorthFontSize,
                    netWorthTimelineRange = netWorthTimelineRange,
                    cashFlowResult = cashFlowResult
                )
            }.flowOn(Dispatchers.Default)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AccountsViewUiState.Loading
        )

    fun updateCardNetWorthFontSize(cardId: String, fontSize: String) {
        viewModelScope.launch {
            cardRepository.updateAccountsViewCard(cardId) { cardConfig ->
                if (cardConfig is com.osscabinet.ossWallet.domain.model.settings.NetWorthCardConfig) {
                    cardConfig.copy(fontSize = fontSize)
                } else cardConfig
            }
            cardDataCache.invalidateCard(cardId)
        }
    }

    fun updateCardNetWorthTimelineRange(cardId: String, range: TimelineRange) {
        viewModelScope.launch {
            cardRepository.updateAccountsViewCard(cardId) { cardConfig ->
                if (cardConfig is com.osscabinet.ossWallet.domain.model.settings.NetWorthCardConfig) {
                    cardConfig.copy(timelineRange = range)
                } else cardConfig
            }
            cardDataCache.invalidateCard(cardId)
        }
    }

    fun updateCardCurrencyBalancesLayout(cardId: String, layout: String) {
        viewModelScope.launch {
            cardRepository.updateAccountsViewCard(cardId) { cardConfig ->
                if (cardConfig is com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig) {
                    cardConfig.copy(layout = layout)
                } else cardConfig
            }
            cardDataCache.invalidateCard(cardId)
        }
    }

    fun updateCardCurrencyBalancesShowBaseConversion(cardId: String, show: Boolean) {
        viewModelScope.launch {
            cardRepository.updateAccountsViewCard(cardId) { cardConfig ->
                if (cardConfig is com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig) {
                    cardConfig.copy(showBaseConversion = show)
                } else cardConfig
            }
            cardDataCache.invalidateCard(cardId)
        }
    }

    fun updateCardCurrencyBalancesTimelineRange(cardId: String, range: TimelineRange) {
        viewModelScope.launch {
            cardRepository.updateAccountsViewCard(cardId) { cardConfig ->
                if (cardConfig is com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig) {
                    cardConfig.copy(timelineRange = range)
                } else cardConfig
            }
            cardDataCache.invalidateCard(cardId)
        }
    }

    fun updateCardCashFlowShowBaseConversion(cardId: String, show: Boolean) {
        viewModelScope.launch {
            cardRepository.updateAccountsViewCard(cardId) { cardConfig ->
                if (cardConfig is com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig) {
                    cardConfig.copy(showBaseConversion = show)
                } else cardConfig
            }
            cardDataCache.invalidateCard(cardId)
        }
    }

    fun updateCardCashFlowTimelineRange(cardId: String, range: TimelineRange) {
        viewModelScope.launch {
            cardRepository.updateAccountsViewCard(cardId) { cardConfig ->
                if (cardConfig is com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig) {
                    cardConfig.copy(timelineRange = range)
                } else cardConfig
            }
            cardDataCache.invalidateCard(cardId)
        }
    }

    fun updateCardAccountsLayout(cardId: String, layout: String) {
        viewModelScope.launch {
            cardRepository.updateAccountsViewCard(cardId) { cardConfig ->
                if (cardConfig is com.osscabinet.ossWallet.domain.model.settings.AccountsCardConfig) {
                    cardConfig.copy(layout = layout)
                } else cardConfig
            }
            cardDataCache.invalidateCard(cardId)
        }
    }

    fun updateCardAccountsGridColumns(cardId: String, columns: Int) {
        viewModelScope.launch {
            cardRepository.updateAccountsViewCard(cardId) { cardConfig ->
                if (cardConfig is com.osscabinet.ossWallet.domain.model.settings.AccountsCardConfig) {
                    cardConfig.copy(gridColumns = columns)
                } else cardConfig
            }
            cardDataCache.invalidateCard(cardId)
        }
    }

    fun updateCardAccountsShowAddAccountCard(cardId: String, show: Boolean) {
        viewModelScope.launch {
            cardRepository.updateAccountsViewCard(cardId) { cardConfig ->
                if (cardConfig is com.osscabinet.ossWallet.domain.model.settings.AccountsCardConfig) {
                    cardConfig.copy(showAddAccountCard = show)
                } else cardConfig
            }
            cardDataCache.invalidateCard(cardId)
        }
    }

    fun updateCardAccountsTimelineRange(cardId: String, range: TimelineRange) {
        viewModelScope.launch {
            cardRepository.updateAccountsViewCard(cardId) { cardConfig ->
                if (cardConfig is com.osscabinet.ossWallet.domain.model.settings.AccountsCardConfig) {
                    cardConfig.copy(timelineRange = range)
                } else cardConfig
            }
            cardDataCache.invalidateCard(cardId)
        }
    }
}

sealed class AccountsViewUiState {
    object Loading : AccountsViewUiState()
    data class Success(
        val accounts: List<Account>,
        val allTransactions: List<Transaction> = emptyList(),
        val baseCurrency: String,
        val exchangeRates: Map<String, BigDecimal>,
        val currencyTotals: List<CurrencyAggregationItem> = emptyList(),
        val decimalPrecision: Int = 2,
        val accountStats: Map<String, AccountSummaryItem> = emptyMap(),
        val gridColumns: Int = 1,
        val accountsLayout: String = "grid",
        val showAddAccountCard: Boolean = true,
        val accountsTimelineRange: TimelineRange = TimelineRange.AllTime,
        val currencyBalancesLayout: String = "scroll",
        val showBaseConversion: Boolean = true,
        val currencyBalancesTimelineRange: TimelineRange = TimelineRange.AllTime,
        val cashFlowShowBaseConversion: Boolean = true,
        val cashFlowTimelineRange: TimelineRange = TimelineRange.AllTime,
        val netWorth: BigDecimal = BigDecimal.ZERO,
        val netIncome: BigDecimal = BigDecimal.ZERO,
        val netExpense: BigDecimal = BigDecimal.ZERO,
        val netWorthFontSize: String = "Medium",
        val netWorthTimelineRange: TimelineRange = TimelineRange.AllTime,
        val cashFlowResult: CashFlowResult? = null
    ) : AccountsViewUiState()
    data class Error(val message: String) : AccountsViewUiState()
}
