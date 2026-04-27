package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.usecase.BudgetAnalysis
import com.osscabinet.ossWallet.domain.usecase.BudgetUseCases
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class BudgetState(
    val budgets: List<BudgetAnalysis> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class BudgetIntent {
    object LoadBudgets : BudgetIntent()
    data class UpsertBudget(val budget: Budget) : BudgetIntent()
    data class DeleteBudget(val id: String) : BudgetIntent()
    object Refresh : BudgetIntent()
}

class BudgetViewModel(
    private val budgetUseCases: BudgetUseCases
) : ViewModel() {

    private val _state = MutableStateFlow(BudgetState())
    val state: StateFlow<BudgetState> = _state.asStateFlow()

    // Backwards compatibility for existing screens
    val budgets: StateFlow<List<Budget>> = _state.map { it.budgets.map { it.budget } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        handleIntent(BudgetIntent.LoadBudgets)
    }

    fun handleIntent(intent: BudgetIntent) {
        when (intent) {
            BudgetIntent.LoadBudgets -> loadBudgets()
            is BudgetIntent.UpsertBudget -> upsertBudget(intent.budget)
            is BudgetIntent.DeleteBudget -> deleteBudget(intent.id)
            BudgetIntent.Refresh -> loadBudgets()
        }
    }

    private fun loadBudgets() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            budgetUseCases.getBudgetsWithAnalysis()
                .catch { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
                .collect { analysis ->
                    _state.update { it.copy(budgets = analysis, isLoading = false) }
                }
        }
    }

    private fun upsertBudget(budget: Budget) {
        viewModelScope.launch {
            budgetUseCases.upsertBudget(budget)
        }
    }

    private fun deleteBudget(id: String) {
        viewModelScope.launch {
            budgetUseCases.deleteBudget(id)
        }
    }

    // Backwards compatibility helper
    fun upsertBudget(category: String, limit: java.math.BigDecimal) {
        viewModelScope.launch {
            val existing = _state.value.budgets.find { it.budget.name == category }
            val newBudget = Budget(
                id = existing?.budget?.id ?: java.util.UUID.randomUUID().toString(),
                name = category,
                limit = limit,
                spent = existing?.budget?.spent ?: java.math.BigDecimal.ZERO,
                currency = existing?.budget?.currency ?: "USD"
            )
            upsertBudget(newBudget)
        }
    }
}
