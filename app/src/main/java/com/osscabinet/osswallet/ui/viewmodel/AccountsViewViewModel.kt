package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.usecase.GetTransactionsUseCase
import com.osscabinet.ossWallet.ui.util.calculateTotalInCurrency
import com.osscabinet.ossWallet.ui.util.convertAmount
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

enum class AccountsViewSection {
    NET_WORTH,
    CURRENCY_BALANCES,
    ACCOUNTS_LIST
}

@OptIn(ExperimentalCoroutinesApi::class)
class AccountsViewViewModel(
    private val accountRepository: AccountRepository,
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val settingsDataStore: SettingsDataStore,
    private val analyticsViewModel: AnalyticsViewModel
) : ViewModel() {

    private val _sectionOrder = MutableStateFlow(listOf(AccountsViewSection.NET_WORTH, AccountsViewSection.CURRENCY_BALANCES, AccountsViewSection.ACCOUNTS_LIST))
    val sectionOrder: StateFlow<List<AccountsViewSection>> = _sectionOrder.asStateFlow()

    init {
        viewModelScope.launch {
            settingsDataStore.accountsViewSectionOrder.collect { savedOrder ->
                if (savedOrder != null) {
                    try {
                        val sections = savedOrder.split(",").map { AccountsViewSection.valueOf(it) }
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
            settingsDataStore.saveAccountsViewSectionOrder(newOrder.joinToString(",") { it.name })
        }
    }

    fun updateGridColumns(columns: Int) {
        viewModelScope.launch {
            settingsDataStore.saveAccountsViewGridColumns(columns)
        }
    }

    val uiState: StateFlow<AccountsViewUiState> = settingsDataStore.currentProfileId
        .flatMapLatest { profileId ->
            combine(
                accountRepository.getAllAccounts(profileId),
                getTransactionsUseCase(profileId),
                analyticsViewModel.uiState,
                settingsDataStore.decimalPrecision,
                settingsDataStore.accountsViewGridColumns
            ) { accounts, allTransactions, analyticsState, precision, gridColumns ->
                val rates = analyticsState.exchangeRates
                val baseCurrency = analyticsState.selectedCurrency
                
                val transactionsByAccount = allTransactions.groupBy { it.accountId }
                
                val accountStats = accounts.associate { account ->
                    val accountTransactions = transactionsByAccount[account.id] ?: emptyList()
                    val breakdown = mutableMapOf<String, BigDecimal>()
                    
                    breakdown[account.currencyCode] = account.initialBalance
                    account.allowedCurrencies.forEach { curr ->
                        breakdown.getOrPut(curr) { BigDecimal.ZERO }
                    }
                    
                    val incomeBreakdown = mutableMapOf<String, BigDecimal>()
                    val expenseBreakdown = mutableMapOf<String, BigDecimal>()
                    
                    accountTransactions.forEach { tx ->
                        val current = breakdown[tx.currency] ?: BigDecimal.ZERO
                        breakdown[tx.currency] = if (tx.isIncome) current.add(tx.amount) else current.subtract(tx.amount)
                        
                        if (tx.isIncome) {
                            incomeBreakdown[tx.currency] = (incomeBreakdown[tx.currency] ?: BigDecimal.ZERO).add(tx.amount)
                        } else {
                            expenseBreakdown[tx.currency] = (expenseBreakdown[tx.currency] ?: BigDecimal.ZERO).add(tx.amount)
                        }
                    }
                    
                    val incomeInAccountCurrency = calculateTotalInCurrency(incomeBreakdown, account.currencyCode, rates, precision)
                    val expenseInAccountCurrency = calculateTotalInCurrency(expenseBreakdown, account.currencyCode, rates, precision)
                    
                    account.id to AccountStats(breakdown.toMap(), incomeInAccountCurrency, expenseInAccountCurrency)
                }

                val updatedAccounts = accounts.map { account ->
                    val stats = accountStats[account.id]
                    
                    val totalInAccountCurrency = calculateTotalInCurrency(
                        currencyTotals = stats?.currencyBalances ?: emptyMap(),
                        targetCurrency = account.currencyCode,
                        exchangeRates = rates,
                        precision = precision
                    )
                    account.copy(balance = totalInAccountCurrency)
                }

                val globalCurrencyTotals = mutableMapOf<String, BigDecimal>()
                accountStats.values.forEach { stats ->
                    stats.currencyBalances.forEach { (curr, amount) ->
                        globalCurrencyTotals[curr] = (globalCurrencyTotals[curr] ?: BigDecimal.ZERO).add(amount)
                    }
                }

                val nonArchivedAccounts = updatedAccounts.filter { !it.isArchived }

                AccountsViewUiState.Success(
                    accounts = nonArchivedAccounts,
                    baseCurrency = baseCurrency,
                    exchangeRates = rates,
                    currencyTotals = globalCurrencyTotals,
                    decimalPrecision = precision,
                    accountStats = accountStats,
                    gridColumns = gridColumns
                )
            }
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
        val baseCurrency: String,
        val exchangeRates: Map<String, BigDecimal>,
        val currencyTotals: Map<String, BigDecimal> = emptyMap(),
        val decimalPrecision: Int = 2,
        val accountStats: Map<String, AccountStats> = emptyMap(),
        val gridColumns: Int = 1
    ) : AccountsViewUiState()
    data class Error(val message: String) : AccountsViewUiState()
}
