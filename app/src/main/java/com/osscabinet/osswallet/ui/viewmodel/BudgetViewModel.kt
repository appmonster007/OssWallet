package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.usecase.SyncBudgetSpentUseCase
import com.osscabinet.ossWallet.domain.usecase.UpsertBudgetUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BudgetViewModel(
    private val syncBudgetSpentUseCase: SyncBudgetSpentUseCase,
    private val upsertBudgetUseCase: UpsertBudgetUseCase
) : ViewModel() {

    private val _budgets = MutableStateFlow<List<Budget>>(emptyList())
    val budgets: StateFlow<List<Budget>> = _budgets.asStateFlow()

    init {
        viewModelScope.launch {
            syncBudgetSpentUseCase().collect {
                _budgets.value = it
            }
        }
    }

    fun upsertBudget(category: String, limit: Double) {
        viewModelScope.launch {
            val existing = _budgets.value.find { it.category == category }
            val newBudget = Budget(category, existing?.spent ?: 0.0, limit)
            upsertBudgetUseCase(newBudget)
        }
    }
}

class BudgetViewModelFactory(
    private val syncBudgetSpentUseCase: SyncBudgetSpentUseCase,
    private val upsertBudgetUseCase: UpsertBudgetUseCase
) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return BudgetViewModel(syncBudgetSpentUseCase, upsertBudgetUseCase) as T
    }
}
