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
    override val cardRepository: CardRepository,
    private val categoryRepository: CategoryRepository,
    private val computationRepository: ComputationRepository,
    private val registry: CardComputerRegistry
) : ViewModel(), CardUpdateHandler {

    override val cardUpdateScope = viewModelScope
    override val cardScreenContext = ScreenContext.ACCOUNTS_VIEW

    val cardsConfig: StateFlow<DashboardCardsConfig> = cardRepository.getCardsConfig(cardScreenContext)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardCardsConfig())

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
            settingsRepository.updateAccountsViewSettings { it.copy(layout = it.layout.copy(sectionOrder = newOrder.joinToString(",") { it.name })) }
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

    val uiState: StateFlow<AccountsViewUiState> = settingsRepository.currentProfileId
        .flatMapLatest { profileId ->
            combine(
                accountRepository.getAllAccounts(profileId),
                transactionUseCases.getTransactions(profileId),
                categoryRepository.categories,
                computationRepository.exchangeRates,
                computationRepository.selectedCurrency,
                computationRepository.hasLoadedExchangeRates,
                settingsRepository.decimalPrecision
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

                if (!hasLoadedRates) {
                    return@combine AccountsViewUiState.Loading
                }

                // Invalidate registry cache when data changes
                registry.clearCache()

                AccountsViewUiState.Success(
                    accounts = accounts.filter { !it.isArchived },
                    allTransactions = allTransactions,
                    allCategories = categories,
                    baseCurrency = baseCurrency,
                    exchangeRates = rates,
                    decimalPrecision = precision
                )
            }.flowOn(Dispatchers.Default)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AccountsViewUiState.Loading
        )
}

sealed class AccountsViewUiState {
    object Loading : AccountsViewUiState()
    data class Success(
        val accounts: List<Account>,
        val allTransactions: List<Transaction> = emptyList(),
        val allCategories: List<Category> = emptyList(),
        val baseCurrency: String,
        val exchangeRates: Map<String, BigDecimal>,
        val decimalPrecision: Int = 2
    ) : AccountsViewUiState()
    data class Error(val message: String) : AccountsViewUiState()
}
