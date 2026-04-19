package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.math.BigDecimal
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.usecase.BudgetUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BudgetViewModel(
    private val budgetUseCases: BudgetUseCases
) : ViewModel() {

    private val _budgets = MutableStateFlow<List<Budget>>(emptyList())
    val budgets: StateFlow<List<Budget>> = _budgets.asStateFlow()

    init {
        viewModelScope.launch {
            budgetUseCases.syncBudgetSpent().collect {
                _budgets.value = it
            }
        }
    }

    fun upsertBudget(category: String, limit: BigDecimal) {
        viewModelScope.launch {
            val existing = _budgets.value.find { it.category == category }
            val newBudget = Budget(category, existing?.spent ?: BigDecimal.ZERO, limit)
            budgetUseCases.upsertBudget(newBudget)
        }
    }
}

class BudgetViewModelFactory(
    private val budgetUseCases: BudgetUseCases
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return BudgetViewModel(budgetUseCases) as T
    }
}
