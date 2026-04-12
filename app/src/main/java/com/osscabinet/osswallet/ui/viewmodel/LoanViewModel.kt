package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Loan
import com.osscabinet.ossWallet.domain.model.LoanType
import com.osscabinet.ossWallet.domain.repository.LoanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoanViewModel(private val repository: LoanRepository) : ViewModel() {

    private val _loans = MutableStateFlow<List<Loan>>(emptyList())
    val loans: StateFlow<List<Loan>> = _loans.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllLoans().collect {
                _loans.value = it
            }
        }
    }

    fun addLoan(
        title: String,
        principal: Double,
        interestRate: Double,
        dueDate: Long,
        type: LoanType
    ) {
        viewModelScope.launch {
            val loan = Loan(
                title = title,
                principal = principal,
                interestRate = interestRate,
                dueDate = dueDate,
                type = type
            )
            repository.insertLoan(loan)
        }
    }

    fun updateRepayment(loan: Loan, amount: Double) {
        viewModelScope.launch {
            val updated = loan.copy(repayments = loan.repayments + amount)
            repository.updateLoan(updated)
        }
    }
    
    fun deleteLoan(loan: Loan) {
        viewModelScope.launch {
            repository.deleteLoan(loan)
        }
    }
}

class LoanViewModelFactory(private val repository: LoanRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return LoanViewModel(repository) as T
    }
}
