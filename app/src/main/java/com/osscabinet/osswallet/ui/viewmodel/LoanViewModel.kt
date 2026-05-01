package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Loan
import com.osscabinet.ossWallet.domain.model.LoanPayment
import com.osscabinet.ossWallet.domain.model.LoanStatus
import com.osscabinet.ossWallet.domain.model.LoanType
import com.osscabinet.ossWallet.domain.repository.LoanRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.service.LoanService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal

class LoanViewModel(
    private val loanRepository: LoanRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val loans: StateFlow<List<Loan>> = settingsRepository.currentProfileId
        .flatMapLatest { profileId -> loanRepository.getAllLoans(profileId) }
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

    fun settleLoad(loan: Loan) {
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
        profileId = profileId
    )
}
