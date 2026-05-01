package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.util.splitByComma
import com.osscabinet.ossWallet.domain.util.joinByComma
import com.osscabinet.ossWallet.domain.util.toEnum
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.CardRepository
import com.osscabinet.ossWallet.domain.repository.ComputationRepository
import com.osscabinet.ossWallet.domain.usecase.*
import com.osscabinet.ossWallet.domain.model.analytics.*
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.card.*
import com.osscabinet.ossWallet.ui.navigation.AccountsFilterContext
import com.osscabinet.ossWallet.domain.model.settings.*
import com.osscabinet.ossWallet.domain.model.TransactionFilter
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
    private val categoryRepository: CategoryRepository,
    private val computationRepository: ComputationRepository,
    private val registry: CardComputerRegistry
) : ViewModel(), CardUpdateHandler {

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
                categoryRepository.categories,
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
                val categories = args[2] as List<Category>
                @Suppress("UNCHECKED_CAST")
                val rates = args[3] as Map<String, BigDecimal>
                val baseCurrency = args[4] as String
                val hasLoadedRates = args[5] as Boolean
                val precision = args[6] as Int
                val accountsViewSettings = args[7] as com.osscabinet.ossWallet.data.local.AccountsViewSettings

                if (!hasLoadedRates) {
                    return@combine AccountsViewUiState.Loading
                }

                // Invalidate registry cache when data changes
                registry.clearCache()

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

                val currentContext = _initialFilterContext.value
                val baseInput = CardInput(
                    transactions = allTransactions,
                    accounts = accounts,
                    categories = categories,
                    exchangeRates = rates,
                    baseCurrency = baseCurrency,
                    precision = precision,
                    filter = TransactionFilter(
                        accountIds = if (currentContext is AccountsFilterContext.ByType || currentContext is AccountsFilterContext.ByCurrency) {
                            accounts.map { it.id }
                        } else emptyList()
                    )
                )

                val globalCurrencyTotals = (registry.compute(
                    CardType.CURRENCY_BALANCES,
                    baseInput,
                    CurrencyBalancesCardConfig(
                        cardId = "currency_balances_main",
                        filter = TransactionFilter(timelineRange = currencyBalancesTimelineRange)
                    )
                ) as CardRenderData.CurrencyBalances).items

                val accountsData = registry.compute(
                    CardType.ACCOUNTS,
                    baseInput,
                    AccountsCardConfig(
                        cardId = "accounts_main",
                        filter = TransactionFilter(timelineRange = accountsTimelineRange)
                    )
                ) as CardRenderData.Accounts
                val updatedAccounts = accountsData.updatedAccounts
                val accountStats = accountsData.accountStats

                val netWorthData = registry.compute(
                    CardType.NET_WORTH,
                    baseInput,
                    NetWorthCardConfig(
                        cardId = "net_worth_main",
                        filter = TransactionFilter(timelineRange = netWorthTimelineRange)
                    )
                ) as CardRenderData.NetWorth
                val nwValue = netWorthData.value
                val nwFlow = netWorthData.cashFlowResult

                val cashFlowResult = (registry.compute(
                    CardType.CASH_FLOW,
                    baseInput,
                    CashFlowCardConfig(
                        cardId = "cash_flow_main",
                        filter = TransactionFilter(timelineRange = cashFlowTimelineRange)
                    )
                ) as CardRenderData.CashFlow).result

                val nonArchivedAccounts = updatedAccounts.filter { !it.isArchived }

                AccountsViewUiState.Success(
                    accounts = nonArchivedAccounts,
                    allTransactions = allTransactions,
                    allCategories = categories,
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

    override fun updateCardNetWorthFontSize(cardId: String, fontSize: String) {
        viewModelScope.launch {
            cardRepository.updateAccountsViewCard(cardId) { cardConfig ->
                if (cardConfig is NetWorthCardConfig) {
                    cardConfig.copy(fontSize = fontSize)
                } else cardConfig
            }
        }
    }

    override fun updateCardNetWorthFilter(cardId: String, filter: TransactionFilter) {
        viewModelScope.launch {
            cardRepository.updateAccountsViewCard(cardId) { cardConfig ->
                if (cardConfig is NetWorthCardConfig) {
                    cardConfig.copy(filter = filter)
                } else cardConfig
            }
        }
    }

    override fun updateCardCurrencyBalancesLayout(cardId: String, layout: String) {
        viewModelScope.launch {
            cardRepository.updateAccountsViewCard(cardId) { cardConfig ->
                if (cardConfig is CurrencyBalancesCardConfig) {
                    cardConfig.copy(layout = layout)
                } else cardConfig
            }
        }
    }

    override fun updateCardCurrencyBalancesShowBaseConversion(cardId: String, show: Boolean) {
        viewModelScope.launch {
            cardRepository.updateAccountsViewCard(cardId) { cardConfig ->
                if (cardConfig is CurrencyBalancesCardConfig) {
                    cardConfig.copy(showBaseConversion = show)
                } else cardConfig
            }
        }
    }

    override fun updateCardCurrencyBalancesFilter(cardId: String, filter: TransactionFilter) {
        viewModelScope.launch {
            cardRepository.updateAccountsViewCard(cardId) { cardConfig ->
                if (cardConfig is CurrencyBalancesCardConfig) {
                    cardConfig.copy(filter = filter)
                } else cardConfig
            }
        }
    }

    override fun updateCardCashFlowShowBaseConversion(cardId: String, show: Boolean) {
        viewModelScope.launch {
            cardRepository.updateAccountsViewCard(cardId) { cardConfig ->
                if (cardConfig is CashFlowCardConfig) {
                    cardConfig.copy(showBaseConversion = show)
                } else cardConfig
            }
        }
    }

    override fun updateCardCashFlowFilter(cardId: String, filter: TransactionFilter) {
        viewModelScope.launch {
            cardRepository.updateAccountsViewCard(cardId) { cardConfig ->
                if (cardConfig is CashFlowCardConfig) {
                    cardConfig.copy(filter = filter)
                } else cardConfig
            }
        }
    }

    override fun updateCardAccountsLayout(cardId: String, layout: String) {
        viewModelScope.launch {
            cardRepository.updateAccountsViewCard(cardId) { cardConfig ->
                if (cardConfig is AccountsCardConfig) {
                    cardConfig.copy(layout = layout)
                } else cardConfig
            }
        }
    }

    override fun updateCardAccountsGridColumns(cardId: String, columns: Int) {
        viewModelScope.launch {
            cardRepository.updateAccountsViewCard(cardId) { cardConfig ->
                if (cardConfig is AccountsCardConfig) {
                    cardConfig.copy(gridColumns = columns)
                } else cardConfig
            }
        }
    }

    override fun updateCardAccountsShowAddAccountCard(cardId: String, show: Boolean) {
        viewModelScope.launch {
            cardRepository.updateAccountsViewCard(cardId) { cardConfig ->
                if (cardConfig is AccountsCardConfig) {
                    cardConfig.copy(showAddAccountCard = show)
                } else cardConfig
            }
        }
    }

    override fun updateCardAccountsFilter(cardId: String, filter: TransactionFilter) {
        viewModelScope.launch {
            cardRepository.updateAccountsViewCard(cardId) { cardConfig ->
                if (cardConfig is AccountsCardConfig) {
                    cardConfig.copy(filter = filter)
                } else cardConfig
            }
        }
    }

    override fun updateCardCategoryBreakdownConfig(cardId: String, config: CategoryBreakdownCardConfig) {
        viewModelScope.launch {
            cardRepository.updateAccountsViewCard(cardId) { config }
        }
    }

    override fun updateCardCategoryExpenseTreemapConfig(cardId: String, config: CategoryExpenseTreemapCardConfig) {
        viewModelScope.launch {
            cardRepository.updateAccountsViewCard(cardId) { config }
        }
    }
}

sealed class AccountsViewUiState {
    object Loading : AccountsViewUiState()
    data class Success(
        val accounts: List<Account>,
        val allTransactions: List<Transaction> = emptyList(),
        val allCategories: List<Category> = emptyList(),
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
