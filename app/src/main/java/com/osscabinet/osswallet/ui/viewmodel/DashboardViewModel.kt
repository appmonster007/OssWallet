package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.usecase.GetTransactionsUseCase
import com.osscabinet.ossWallet.ui.util.calculateTotalInCurrency
import com.osscabinet.ossWallet.ui.util.convertAmount
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.math.BigDecimal
import java.math.RoundingMode

enum class DashboardSection {
    NET_WORTH,
    CURRENCY_BALANCES,
    CASHFLOW,
    ACCOUNTS,
    BUDGETS
}

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val accountRepository: AccountRepository,
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val settingsDataStore: SettingsDataStore,
    private val analyticsViewModel: AnalyticsViewModel
) : ViewModel() {

    private val _sectionOrder = MutableStateFlow(listOf(DashboardSection.NET_WORTH, DashboardSection.CURRENCY_BALANCES, DashboardSection.CASHFLOW, DashboardSection.ACCOUNTS, DashboardSection.BUDGETS))
    val sectionOrder: StateFlow<List<DashboardSection>> = _sectionOrder.asStateFlow()

    fun updateSectionOrder(newOrder: List<DashboardSection>) {
        _sectionOrder.value = newOrder
    }

    val uiState: StateFlow<DashboardUiState> = settingsDataStore.currentProfileId
        .flatMapLatest { profileId ->
            combine(
                accountRepository.getAllAccounts(profileId),
                getTransactionsUseCase(profileId),
                analyticsViewModel.uiState,
                settingsDataStore.decimalPrecision
            ) { accounts, allTransactions, analyticsState, precision ->
                val rates = analyticsState.exchangeRates
                val baseCurrency = analyticsState.selectedCurrency
                
                val transactionsByAccount = allTransactions.groupBy { it.accountId }
                
                // Calculate breakdown and totals per account
                val accountBreakdowns = accounts.associate { account ->
                    val accountTransactions = transactionsByAccount[account.id] ?: emptyList()
                    val breakdown = mutableMapOf<String, BigDecimal>()
                    breakdown[account.currencyCode] = account.initialBalance
                    accountTransactions.forEach { tx ->
                        val current = breakdown[tx.currency] ?: BigDecimal.ZERO
                        breakdown[tx.currency] = if (tx.isIncome) current.add(tx.amount) else current.subtract(tx.amount)
                    }
                    account.id to breakdown
                }

                // Update accounts with correct total balances in their primary currency
                val updatedAccounts = accounts.map { account ->
                    val breakdown = accountBreakdowns[account.id] ?: emptyMap()
                    
                    val totalInAccountCurrency = calculateTotalInCurrency(
                        currencyTotals = breakdown,
                        targetCurrency = account.currencyCode,
                        exchangeRates = rates,
                        precision = precision
                    )
                    account.copy(balance = totalInAccountCurrency)
                }

                // Global currency totals across all accounts
                val globalCurrencyTotals = mutableMapOf<String, BigDecimal>()
                accountBreakdowns.values.forEach { breakdown ->
                    breakdown.forEach { (curr, amount) ->
                        globalCurrencyTotals[curr] = (globalCurrencyTotals[curr] ?: BigDecimal.ZERO).add(amount)
                    }
                }

                val nonArchivedAccounts = updatedAccounts.filter { !it.isArchived }
                val nonArchivedAccountIds = nonArchivedAccounts.map { it.id }.toSet()
                
                val accountMapForTransactions = updatedAccounts.associateBy { it.id }

                // Calculate TransactionsWithBalance (recent activities) with correct balance progression in account currency
                val transactionsWithBalance = allTransactions.sortedBy { it.timestamp }
                    .groupBy { it.accountId }
                    .flatMap { (accId, accountTransactions) ->
                        val account = accountMapForTransactions[accId]
                        val accountCurrency = account?.currencyCode ?: "USD"

                        var runningBalance = account?.initialBalance ?: BigDecimal.ZERO
                        accountTransactions.map { transaction ->
                            val txAmountInAccountCurrency = convertAmount(
                                amount = transaction.amount,
                                fromCurrency = transaction.currency,
                                toCurrency = accountCurrency,
                                exchangeRates = rates,
                                precision = precision + 4
                            )
                            
                            runningBalance = if (transaction.isIncome) {
                                runningBalance.add(txAmountInAccountCurrency)
                            } else {
                                runningBalance.subtract(txAmountInAccountCurrency)
                            }
                            TransactionWithBalance(transaction, runningBalance.setScale(precision, RoundingMode.HALF_UP))
                        }
                    }
                    .filter { it.transaction.accountId in nonArchivedAccountIds }
                    .sortedByDescending { it.transaction.timestamp }
                    .take(20)

                DashboardUiState.Success(
                    accounts = nonArchivedAccounts,
                    recentTransactions = transactionsWithBalance,
                    baseCurrency = baseCurrency,
                    exchangeRates = rates,
                    currencyTotals = globalCurrencyTotals,
                    decimalPrecision = precision
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardUiState.Loading
        )
}

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Success(
        val accounts: List<Account>,
        val recentTransactions: List<TransactionWithBalance>,
        val baseCurrency: String,
        val exchangeRates: Map<String, BigDecimal>,
        val currencyTotals: Map<String, BigDecimal> = emptyMap(),
        val decimalPrecision: Int = 2
    ) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}
