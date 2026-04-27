package com.osscabinet.ossWallet.domain.usecase

import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.domain.repository.LoanRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class LoanUseCasesTest {

    private lateinit var loanRepository: LoanRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var loanUseCases: LoanUseCases

    @Before
    fun setup() {
        loanRepository = object : LoanRepository {
            override fun getAllLoans() = flowOf(listOf(
                Loan(
                    account = Account(id = "loan1", name = "Car Loan", balance = BigDecimal("-8000"), currencyCode = "USD", structureType = "Loan", colorHex = "#FF0000", icon = "Car"),
                    principal = BigDecimal("10000"),
                    interestRate = BigDecimal("5"),
                    maturityDate = System.currentTimeMillis() + 1000000,
                    loanType = LoanType.CREDIT
                )
            ))
            override suspend fun getLoanByAccountId(accountId: String): Loan? = null
            override suspend fun upsertLoan(loan: Loan) {}
            override suspend fun deleteLoan(accountId: String) {}
        }

        transactionRepository = object : TransactionRepository {
            override fun getAllTransactions(profileId: Int) = flowOf(listOf(
                Transaction(
                    id = 10L,
                    title = "Monthly Pay",
                    amount = BigDecimal("200"),
                    category = "Loan Repayment",
                    timestamp = System.currentTimeMillis(),
                    currency = "USD",
                    transactionType = "income",
                    accountId = "loan1"
                ),
                Transaction(
                    id = 11L,
                    title = "Interest Charge",
                    amount = BigDecimal("30"),
                    category = "Interest",
                    timestamp = System.currentTimeMillis(),
                    currency = "USD",
                    transactionType = "expense",
                    accountId = "loan1"
                )
            ))
            override suspend fun getAllTransactionsSync(profileId: Int) = emptyList<Transaction>()
            override fun getFilteredTransactions(profileId: Int, filter: TransactionFilter) = flowOf(emptyList<Transaction>())
            override suspend fun getFilteredTransactionsSync(profileId: Int, filter: TransactionFilter) = emptyList<Transaction>()
            override suspend fun insertTransaction(transaction: Transaction, profileId: Int) = 0L
            override suspend fun saveTransfer(sourceTransaction: Transaction, targetTransaction: Transaction, profileId: Int) = 0L to 0L
            override suspend fun deleteTransaction(id: Long) {}
            override suspend fun updateTransaction(oldTransaction: Transaction, newTransaction: Transaction, profileId: Int) {}
            override suspend fun getTransactionsByAccount(accountId: String) = flowOf(emptyList<Transaction>())
            override suspend fun getTransactionById(id: Long): Transaction? = null
            override suspend fun addTransactionLink(transactionId: Long, linkedTransactionId: Long) {}
            override suspend fun removeTransactionLink(transactionId: Long, linkedTransactionId: Long) {}
            override suspend fun clearTransactionLinks(transactionId: Long) {}
            override fun getLinkedTransactionIds(transactionId: Long) = flowOf(emptyList<Long>())
            override fun getLinkedTransactions(transactionId: Long) = flowOf(emptyList<Transaction>())
            override suspend fun getCurrencySummaries(profileId: Int) = emptyList<com.osscabinet.ossWallet.data.local.dao.CurrencySummary>()
            override suspend fun getCurrencySummariesInRange(profileId: Int, start: Long, end: Long) = emptyList<com.osscabinet.ossWallet.data.local.dao.CurrencySummary>()
        }

        loanUseCases = LoanUseCases(loanRepository, transactionRepository)
    }

    @Test
    fun `getLoansWithAnalysis calculates repayments correctly`() = runTest {
        val analysis = loanUseCases.getLoansWithAnalysis(1).first()
        assertEquals(1, analysis.size)
        val carLoan = analysis[0]
        
        assertEquals(1, carLoan.repayments.size)
        assertEquals(BigDecimal("200"), carLoan.principalPaid)
    }
}
