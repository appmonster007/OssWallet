package com.osscabinet.ossWallet.ui.screens.budgets

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.model.BudgetProgress
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.repository.BudgetRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.ComputationRepository
import com.osscabinet.ossWallet.domain.repository.PlannedPaymentRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.domain.service.management.BudgetService
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
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val computationRepository: ComputationRepository,
    private val plannedPaymentRepository: PlannedPaymentRepository,
    settingsRepository: SettingsRepository
) : BaseViewModel(settingsRepository) {

    private val _selectedOffsets = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val selectedOffsets: StateFlow<Map<Long, Int>> = _selectedOffsets.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentProfileId: StateFlow<Int> = settingsRepository.currentProfileId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    @OptIn(ExperimentalCoroutinesApi::class)
    val budgets: StateFlow<List<Budget>> = settingsRepository.currentProfileId
        .flatMapLatest { profileId ->
            budgetRepository.getAllBudgets(profileId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val transactions: StateFlow<List<Transaction>> = settingsRepository.currentProfileId
        .flatMapLatest { profileId -> transactionRepository.getAllTransactions(profileId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val categories: StateFlow<List<Category>> = settingsRepository.currentProfileId
        .flatMapLatest { profileId -> categoryRepository.getAllCategories(profileId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val plannedPayments: StateFlow<List<PlannedPayment>> = settingsRepository.currentProfileId
        .flatMapLatest { profileId -> plannedPaymentRepository.getActivePlannedPayments(profileId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableTags: StateFlow<List<String>> = transactions
        .map { txList -> txList.flatMap { it.tags }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private data class BudgetBaseSnapshot(
        val budgets: List<Budget>,
        val transactions: List<Transaction>,
        val rates: ExchangeRateSnapshot,
        val categories: List<Category>,
        val offsets: Map<Long, Int>
    )

    private data class BudgetSnapshot(
        val base: BudgetBaseSnapshot,
        val plannedPayments: List<PlannedPayment>
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val budgetProgress: StateFlow<Map<Long, BudgetProgress>> = combine(
        combine(
            budgets,
            transactions,
            computationRepository.exchangeRatesSnapshot,
            categories,
            selectedOffsets
        ) { budgetsList, transactionsList, snapshot, categoriesList, offsets ->
            BudgetBaseSnapshot(budgetsList, transactionsList, snapshot, categoriesList, offsets)
        },
        plannedPayments
    ) { base, plannedPaymentsList ->
        BudgetSnapshot(base, plannedPaymentsList)
    }.distinctUntilChanged()
        .map { snapshot ->
            val base = snapshot.base
            val baseCurrency = base.rates.anchor
            val now = System.currentTimeMillis()
            base.budgets.associate { budget ->
                val offset = base.offsets[budget.id] ?: 0
                val shiftedRange = if (offset != 0) {
                    com.osscabinet.ossWallet.domain.service.timeline.TimelineService.shiftRange(budget.timelineRange, offset)
                } else budget.timelineRange

                val (start, end) = com.osscabinet.ossWallet.domain.service.timeline.TimelineService.getDateRange(shiftedRange, referenceTime = now)
                val evalBudget = if (start != null && end != null) {
                    budget.copy(timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.CustomRange(start, end))
                } else budget.copy(timelineRange = shiftedRange)

                val rollover = BudgetService.calculateRollover(
                    evalBudget,
                    base.transactions,
                    base.rates,
                    baseCurrency,
                    now,
                    base.categories
                )
                val spent = BudgetService.calculateSpent(
                    evalBudget,
                    base.transactions,
                    base.rates,
                    baseCurrency,
                    now,
                    base.categories
                )
                val planned = BudgetService.calculatePlannedAmount(
                    evalBudget,
                    base.transactions,
                    snapshot.plannedPayments,
                    base.rates,
                    baseCurrency,
                    now,
                    base.categories
                )
                val elapsed = BudgetService.elapsedFraction(evalBudget, now)
                budget.id to BudgetService.buildProgress(evalBudget, spent, rollover, elapsed, planned)
            }
        }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    @OptIn(ExperimentalCoroutinesApi::class)
    val periodHistory: StateFlow<Map<Long, List<BudgetProgress>>> = combine(
        combine(
            budgets,
            transactions,
            computationRepository.exchangeRatesSnapshot,
            categories,
            selectedOffsets
        ) { budgetsList, transactionsList, snapshot, categoriesList, offsets ->
            BudgetBaseSnapshot(budgetsList, transactionsList, snapshot, categoriesList, offsets)
        },
        plannedPayments
    ) { base, plannedPaymentsList ->
        val baseCurrency = base.rates.anchor
        val now = System.currentTimeMillis()
        base.budgets.associate { budget ->
            val currentOffset = base.offsets[budget.id] ?: 0
            budget.id to BudgetService.getPeriodHistory(
                budget = budget,
                transactions = base.transactions,
                exchangeRates = base.rates,
                baseCurrency = baseCurrency,
                now = now,
                allCategories = base.categories,
                offset = currentOffset,
                count = 5,
                plannedPayments = plannedPaymentsList
            )
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun setPeriodOffset(budgetId: Long, offset: Int) {
        _selectedOffsets.value = _selectedOffsets.value + (budgetId to offset)
    }

    fun delete(budgetId: Long) {
        viewModelScope.launch {
            budgetRepository.delete(budgetId)
        }
    }

    fun archive(budget: Budget) {
        viewModelScope.launch {
            budgetRepository.update(budget.copy(isArchived = true))
        }
    }
}
