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
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.domain.service.management.LoanService
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class LoanViewModel @Inject constructor(
    private val loanRepository: LoanRepository,
    settingsRepository: SettingsRepository,
    private val transactionRepository: TransactionRepository,
    private val computationRepository: ComputationRepository,
    private val categoryRepository: CategoryRepository
) : BaseViewModel(settingsRepository) {

    private val _selectedOffsets = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val selectedOffsets: StateFlow<Map<Long, Int>> = _selectedOffsets.asStateFlow()

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

    @OptIn(ExperimentalCoroutinesApi::class)
    private val categories: StateFlow<List<Category>> = settingsRepository.currentProfileId
        .flatMapLatest { profileId -> categoryRepository.getAllCategories(profileId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableTags: StateFlow<List<String>> = transactions
        .map { txList -> txList.asSequence().flatMap { it.tags }.distinct().sorted().toList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private data class LoanSnapshot(
        val loans: List<Loan>,
        val transactions: List<Transaction>,
        val rates: ExchangeRateSnapshot,
        val categories: List<Category>,
        val offsets: Map<Long, Int>
    )

    /** Precomputed summaries for every loan — mirrors BudgetViewModel.budgetProgress. */
    val loanSummaries: StateFlow<Map<Long, LoanSummary>> = combine(
        loans,
        transactions,
        computationRepository.exchangeRatesSnapshot,
        categories,
        selectedOffsets
    ) { loansList, transactionsList, snapshot, categoriesList, offsetsMap ->
        LoanSnapshot(loansList, transactionsList, snapshot, categoriesList, offsetsMap)
    }.distinctUntilChanged()
        .map { snapshot ->
            val now = System.currentTimeMillis()
            snapshot.loans.associate { loan ->
                val offset = snapshot.offsets[loan.id] ?: 0
                val evalLoan = if (offset != 0) {
                    loan.copy(
                        timelineRange = com.osscabinet.ossWallet.domain.service.timeline.TimelineService.shiftRange(
                            loan.timelineRange,
                            offset
                        )
                    )
                } else loan

                loan.id to LoanService.buildSummaryFromTransactions(
                    evalLoan,
                    snapshot.transactions,
                    snapshot.rates,
                    now,
                    allCategories = snapshot.categories
                )
            }
        }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    @OptIn(ExperimentalCoroutinesApi::class)
    val periodHistory: StateFlow<Map<Long, List<LoanSummary>>> = combine(
        loans,
        transactions,
        computationRepository.exchangeRatesSnapshot,
        categories,
        selectedOffsets
    ) { loansList, transactionsList, snapshot, categoriesList, offsetsMap ->
        val now = System.currentTimeMillis()
        loansList.associate { loan ->
            val currentOffset = offsetsMap[loan.id] ?: 0
            loan.id to LoanService.getPeriodHistory(
                loan = loan,
                transactions = transactionsList,
                exchangeRates = snapshot,
                now = now,
                allCategories = categoriesList,
                offset = currentOffset,
                count = 5
            )
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun setPeriodOffset(loanId: Long, offset: Int) {
        _selectedOffsets.value = _selectedOffsets.value + (loanId to offset)
    }

    fun deleteLoan(loanId: Long) {
        viewModelScope.launch { loanRepository.deleteLoan(loanId) }
    }

    fun settleLoan(loan: Loan) {
        viewModelScope.launch { loanRepository.updateLoan(LoanService.settledCopy(loan)) }
    }
}
