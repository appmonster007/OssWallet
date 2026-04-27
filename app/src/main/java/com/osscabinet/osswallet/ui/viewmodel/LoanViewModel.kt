package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Loan
import com.osscabinet.ossWallet.domain.usecase.LoanAnalysis
import com.osscabinet.ossWallet.domain.usecase.LoanUseCases
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LoanState(
    val loans: List<LoanAnalysis> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class LoanIntent {
    object LoadLoans : LoanIntent()
    data class UpsertLoan(val loan: Loan) : LoanIntent()
    data class DeleteLoan(val accountId: String) : LoanIntent()
}

class LoanViewModel(
    private val loanUseCases: LoanUseCases
) : ViewModel() {

    private val _state = MutableStateFlow(LoanState())
    val state: StateFlow<LoanState> = _state.asStateFlow()

    // Backwards compatibility
    val loans: StateFlow<List<Loan>> = _state.map { it.loans.map { it.loan } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        handleIntent(LoanIntent.LoadLoans)
    }

    fun handleIntent(intent: LoanIntent) {
        when (intent) {
            LoanIntent.LoadLoans -> loadLoans()
            is LoanIntent.UpsertLoan -> upsertLoan(intent.loan)
            is LoanIntent.DeleteLoan -> deleteLoan(intent.accountId)
        }
    }

    private fun loadLoans() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            loanUseCases.getLoansWithAnalysis()
                .catch { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
                .collect { analysis ->
                    _state.update { it.copy(loans = analysis, isLoading = false) }
                }
        }
    }

    private fun upsertLoan(loan: Loan) {
        viewModelScope.launch {
            loanUseCases.upsertLoan(loan)
        }
    }

    private fun deleteLoan(accountId: String) {
        viewModelScope.launch {
            loanUseCases.deleteLoan(accountId)
        }
    }
}
