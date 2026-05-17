package com.osscabinet.ossWallet.ui.screens.loans

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Loan
import com.osscabinet.ossWallet.domain.model.LoanSummary
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.CurrencyRepository
import com.osscabinet.ossWallet.domain.repository.LoanRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import com.osscabinet.ossWallet.domain.service.management.LoanService
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal

class LoanViewModel(
    private val loanRepository: LoanRepository,
    settingsRepository: SettingsRepository,
    private val transactionRepository: TransactionRepository? = null,
    private val currencyRepository: CurrencyRepository? = null,
    private val categoryRepository: CategoryRepository? = null
) : BaseViewModel(settingsRepository) {

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentProfileId: StateFlow<Int> = settingsRepository.currentProfileId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    @OptIn(ExperimentalCoroutinesApi::class)
    val loans: StateFlow<List<Loan>> = settingsRepository.currentProfileId
        .flatMapLatest { profileId -> loanRepository.getAllLoans(profileId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val transactions: StateFlow<List<Transaction>> = if (transactionRepository != null) {
        settingsRepository.currentProfileId
            .flatMapLatest { profileId -> transactionRepository.getAllTransactions(profileId) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    } else {
        MutableStateFlow(emptyList())
    }

    private val categories: StateFlow<List<Category>> = categoryRepository?.categories
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        ?: MutableStateFlow(emptyList())

    val availableTags: StateFlow<List<String>> = transactions
        .map { txList -> txList.asSequence().flatMap { it.tags }.distinct().sorted().toList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _exchangeRates = MutableStateFlow<Map<String, BigDecimal>>(emptyMap())

    /** Precomputed summaries for every loan — mirrors BudgetViewModel.budgetProgress. */
    val loanSummaries: StateFlow<Map<Long, LoanSummary>> = combine(
        loans,
        transactions,
        _exchangeRates,
        categories
    ) { loansList, transactionsList, rates, categoriesList ->
        val now = System.currentTimeMillis()
        loansList.associate { loan ->
            loan.id to LoanService.buildSummaryFromTransactions(
                loan, transactionsList, rates, now, allCategories = categoriesList
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        if (currencyRepository != null) {
            viewModelScope.launch {
                settingsRepository.currency.collect { base ->
                    try {
                        val rates = currencyRepository.getExchangeRates(base)
                        _exchangeRates.value = rates.mapValues { (_, v) -> BigDecimal.valueOf(v) }
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    fun deleteLoan(loanId: Long) {
        viewModelScope.launch { loanRepository.deleteLoan(loanId) }
    }

    fun settleLoan(loan: Loan) {
        viewModelScope.launch { loanRepository.updateLoan(LoanService.settledCopy(loan)) }
    }
}
