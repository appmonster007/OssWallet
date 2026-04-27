package com.osscabinet.ossWallet.domain.usecase

import com.osscabinet.ossWallet.domain.model.Loan
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.repository.LoanRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.math.BigDecimal

data class LoanAnalysis(
    val loan: Loan,
    val repayments: List<Transaction>,
    val disbursements: List<Transaction>,
    val interestPaid: BigDecimal,
    val principalPaid: BigDecimal,
    val remainingMaturityDays: Int
)

class LoanUseCases(
    private val loanRepository: LoanRepository,
    private val transactionRepository: TransactionRepository
) {
    fun getAllLoans(): Flow<List<Loan>> = loanRepository.getAllLoans()

    suspend fun getLoanByAccountId(accountId: String) = loanRepository.getLoanByAccountId(accountId)

    suspend fun upsertLoan(loan: Loan) = loanRepository.upsertLoan(loan)

    suspend fun deleteLoan(accountId: String) = loanRepository.deleteLoan(accountId)

    fun getLoansWithAnalysis(profileId: Int = 1): Flow<List<LoanAnalysis>> {
        return combine(
            loanRepository.getAllLoans(),
            transactionRepository.getAllTransactions(profileId)
        ) { loans, transactions ->
            loans.map { loan ->
                val loanTransactions = transactions.filter { it.accountId == loan.account.id }
                
                // For a CREDIT loan (we owe money):
                // - Disbursements: transfers OUT of the loan account (or initial balance which we treat as disbursement)
                // - Repayments: transfers INTO the loan account (income to this account)
                val repayments = loanTransactions.filter { it.transactionType == "income" }
                val disbursements = loanTransactions.filter { it.transactionType == "expense" }
                
                // Calculate Interest vs Principal
                // Standard logic: if repayment category is "Interest", it's interest. 
                // For now, let's assume all repayments are principal unless categorized differently.
                val interestPaid = repayments.filter { it.category.contains("Interest", ignoreCase = true) }
                    .sumOf { it.amount.abs() }
                
                val principalPaid = repayments.filter { !it.category.contains("Interest", ignoreCase = true) }
                    .sumOf { it.amount.abs() }

                val now = System.currentTimeMillis()
                val remainingMillis = (loan.maturityDate - now).coerceAtLeast(0)
                val remainingDays = (remainingMillis / (24 * 60 * 60 * 1000L)).toInt()

                LoanAnalysis(
                    loan = loan,
                    repayments = repayments,
                    disbursements = disbursements,
                    interestPaid = interestPaid,
                    principalPaid = principalPaid,
                    remainingMaturityDays = remainingDays
                )
            }
        }
    }
}
