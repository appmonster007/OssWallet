package com.osscabinet.ossWallet.ui.screens.budgets

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.model.BudgetProgress
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.repository.BudgetRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.CurrencyRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import com.osscabinet.ossWallet.domain.service.management.BudgetService
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal

class BudgetViewModel(
    private val budgetRepository: BudgetRepository,
    settingsRepository: SettingsRepository,
    private val transactionRepository: TransactionRepository? = null,
    currencyRepository: CurrencyRepository? = null,
    categoryRepository: CategoryRepository? = null
) : BaseViewModel(settingsRepository) {

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

    val categories: StateFlow<List<Category>> = categoryRepository?.categories
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        ?: MutableStateFlow(emptyList())

    val availableTags: StateFlow<List<String>> = transactions
        .map { txList -> txList.flatMap { it.tags }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _exchangeRates = MutableStateFlow<Map<String, BigDecimal>>(emptyMap())

    @OptIn(ExperimentalCoroutinesApi::class)
    val budgetProgress: StateFlow<Map<Long, BudgetProgress>> = combine(
        budgets,
        transactions,
        settingsRepository.currency,
        _exchangeRates,
        categories
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        val budgetsList = arr[0] as List<Budget>

        @Suppress("UNCHECKED_CAST")
        val transactionsList = arr[1] as List<Transaction>
        val baseCurrency = arr[2] as String

        @Suppress("UNCHECKED_CAST")
        val rates = arr[3] as Map<String, BigDecimal>

        @Suppress("UNCHECKED_CAST")
        val categoriesList = arr[4] as List<Category>

        val now = System.currentTimeMillis()
        budgetsList.associate { budget ->
            val rollover = BudgetService.calculateRollover(
                budget,
                transactionsList,
                rates,
                baseCurrency,
                now,
                categoriesList
            )
            val spent = BudgetService.calculateSpent(
                budget,
                transactionsList,
                rates,
                baseCurrency,
                now,
                categoriesList
            )
            budget.id to BudgetService.buildProgress(budget, spent, rollover)
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
