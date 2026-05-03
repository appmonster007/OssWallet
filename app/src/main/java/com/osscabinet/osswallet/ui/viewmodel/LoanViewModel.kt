package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Loan
import com.osscabinet.ossWallet.domain.model.LoanPayment
import com.osscabinet.ossWallet.domain.model.LoanStatus
import com.osscabinet.ossWallet.domain.model.LoanType
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.repository.LoanRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import com.osscabinet.ossWallet.domain.service.LoanService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal

class LoanViewModel(
    private val loanRepository: LoanRepository,
    private val settingsRepository: SettingsRepository,
    private val transactionRepository: TransactionRepository? = null
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentProfileId: StateFlow<Int> = settingsRepository.currentProfileId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    @OptIn(ExperimentalCoroutinesApi::class)
    val loans: StateFlow<List<Loan>> = settingsRepository.currentProfileId
        .flatMapLatest { profileId -> loanRepository.getAllLoans(profileId) }
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

    fun getPaymentsForLoan(loanId: Long): Flow<List<LoanPayment>> =
        loanRepository.getPaymentsForLoan(loanId)

    fun saveLoan(loan: Loan) {
        viewModelScope.launch {
            if (loan.id == 0L) loanRepository.insertLoan(loan)
            else loanRepository.updateLoan(loan)
        }
    }

    fun deleteLoan(loanId: Long) {
        viewModelScope.launch { loanRepository.deleteLoan(loanId) }
    }

    fun settleLoan(loan: Loan) {
        viewModelScope.launch { loanRepository.updateLoan(LoanService.settledCopy(loan)) }
    }

    fun addPayment(payment: LoanPayment) {
        viewModelScope.launch { loanRepository.insertPayment(payment) }
    }

    fun deletePayment(paymentId: Long) {
        viewModelScope.launch { loanRepository.deletePayment(paymentId) }
    }

    fun buildDefaultLoan(profileId: Int, accountId: String = ""): Loan = Loan(
        id = 0,
        name = "",
        principalAmount = BigDecimal.ZERO,
        currency = "USD",
        interestRate = null,
        startDate = System.currentTimeMillis(),
        lenderOrBorrowerName = "",
        loanType = LoanType.BORROWED,
        linkedAccountId = accountId,
        status = LoanStatus.ACTIVE,
        profileId = profileId,
        categoryIds = emptyList(),
        tags = emptyList()
    )

    /**
     * Get transactions related to this loan based on linked account, categories, and tags.
     */
    fun getLoanTransactions(loan: Loan): List<Transaction> {
        val allTransactions = transactions.value
        return allTransactions.filter { transaction ->
            // Filter by linked account if specified
            val accountMatch = loan.linkedAccountId.isBlank() || transaction.accountId == loan.linkedAccountId
            
            // Filter by categories if specified
            val categoryMatch = loan.categoryIds.isEmpty() || 
                transaction.categoryId in loan.categoryIds
            
            // Filter by tags if specified (transaction must have at least one matching tag)
            val tagMatch = loan.tags.isEmpty() || 
                transaction.tags.any { it in loan.tags }
            
            accountMatch && categoryMatch && tagMatch
        }.sortedByDescending { it.timestamp }
    }
}
