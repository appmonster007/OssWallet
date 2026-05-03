package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.model.BudgetPeriod
import com.osscabinet.ossWallet.domain.model.BudgetProgress
import com.osscabinet.ossWallet.domain.model.BudgetTrackType
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.repository.BudgetRepository
import com.osscabinet.ossWallet.domain.repository.CurrencyRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import com.osscabinet.ossWallet.domain.service.BudgetService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal

class BudgetViewModel(
    private val budgetRepository: BudgetRepository,
    private val settingsRepository: SettingsRepository,
    private val transactionRepository: TransactionRepository? = null,
    private val currencyRepository: CurrencyRepository? = null
) : ViewModel() {

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
    val transactions: StateFlow<List<Transaction>> = if (transactionRepository != null) {
        settingsRepository.currentProfileId
            .flatMapLatest { profileId -> transactionRepository.getAllTransactions(profileId) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    } else {
        MutableStateFlow(emptyList())
    }

    val availableTags: StateFlow<List<String>> = transactions
        .map { txList -> txList.flatMap { it.tags }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _exchangeRates = MutableStateFlow<Map<String, BigDecimal>>(emptyMap())

    init {
        if (currencyRepository != null) {
            viewModelScope.launch {
                settingsRepository.currency.collect { base ->
                    try {
                        val rates = currencyRepository.getExchangeRates(base)
                        _exchangeRates.value = rates.mapValues { (_, v) -> BigDecimal.valueOf(v) }
                    } catch (_: Exception) { }
                }
            }
        }
    }

    fun save(budget: Budget) {
        viewModelScope.launch {
            if (budget.id == 0L) {
                budgetRepository.insert(budget)
            } else {
                budgetRepository.update(budget)
            }
        }
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

    fun getBudgetProgress(budget: Budget, baseCurrency: String, precision: Int): BudgetProgress {
        val now = System.currentTimeMillis()
        val allTx = transactions.value
        val rates = _exchangeRates.value
        val rollover = BudgetService.calculateRollover(budget, allTx, rates, baseCurrency, precision, now)
        val spent = BudgetService.calculateSpent(budget, allTx, rates, baseCurrency, precision, now)
        return BudgetService.buildProgress(budget, spent, rollover)
    }

    fun buildDefault(profileId: Int): Budget = Budget(
        id = 0L,
        name = "",
        currency = "USD",
        limitAmount = BigDecimal.ZERO,
        period = BudgetPeriod.MONTHLY,
        categoryIds = emptyList(),
        accountIds = emptyList(),
        alertThreshold = 0.8f,
        rollover = false,
        trackTypes = listOf(BudgetTrackType.EXPENSE),
        isArchived = false,
        profileId = profileId
    )
}
