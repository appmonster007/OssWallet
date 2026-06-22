package com.osscabinet.ossWallet.ui.screens.loans

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Loan
import com.osscabinet.ossWallet.domain.model.LoanSummary
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.ComputationRepository
import com.osscabinet.ossWallet.domain.repository.LoanRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import com.osscabinet.ossWallet.domain.service.management.LoanService
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoanViewModel @Inject constructor(
    private val loanRepository: LoanRepository,
    settingsRepository: SettingsRepository,
    private val transactionRepository: TransactionRepository,
    private val computationRepository: ComputationRepository,
    private val categoryRepository: CategoryRepository
) : BaseViewModel(settingsRepository) {

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentProfileId: StateFlow<Int> = settingsRepository.currentProfileId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    @OptIn(ExperimentalCoroutinesApi::class)
    val loans: StateFlow<List<Loan>> = settingsRepository.currentProfileId
        .flatMapLatest { profileId -> loanRepository.getAllLoans(profileId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val transactions: StateFlow<List<Transaction>> = settingsRepository.currentProfileId
        .flatMapLatest { profileId -> transactionRepository.getAllTransactions(profileId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val categories: StateFlow<List<Category>> = categoryRepository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableTags: StateFlow<List<String>> = transactions
        .map { txList -> txList.asSequence().flatMap { it.tags }.distinct().sorted().toList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Precomputed summaries for every loan — mirrors BudgetViewModel.budgetProgress. */
    val loanSummaries: StateFlow<Map<Long, LoanSummary>> = combine(
        loans,
        transactions,
        computationRepository.exchangeRatesSnapshot,
        categories
    ) { loansList, transactionsList, snapshot, categoriesList ->
        val now = System.currentTimeMillis()
        loansList.associate { loan ->
            loan.id to LoanService.buildSummaryFromTransactions(
                loan, transactionsList, snapshot, now, allCategories = categoriesList
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun deleteLoan(loanId: Long) {
        viewModelScope.launch { loanRepository.deleteLoan(loanId) }
    }

    fun settleLoan(loan: Loan) {
        viewModelScope.launch { loanRepository.updateLoan(LoanService.settledCopy(loan)) }
    }
}
