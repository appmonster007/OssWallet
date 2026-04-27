package com.osscabinet.ossWallet.domain.usecase

import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.domain.repository.BudgetRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BudgetUseCasesTest {

    private lateinit var budgetRepository: BudgetRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var budgetUseCases: BudgetUseCases

    @Before
    fun setup() {
        budgetRepository = object : BudgetRepository {
            override fun getBudgets() = flowOf(listOf(
                Budget(
                    id = "1",
                    name = "Food",
                    limit = BigDecimal("1000"),
                    spent = BigDecimal.ZERO,
                    period = BudgetPeriod.MONTHLY,
                    filter = TransactionFilter(categoryIds = listOf(1L))
                )
            ))
            override suspend fun getBudgetById(id: String): Budget? = null
            override suspend fun upsertBudget(budget: Budget) {}
            override suspend fun deleteBudget(id: String) {}
            override suspend fun evaluateBudgets() {}
        }

        transactionRepository = object : TransactionRepository {
            override fun getAllTransactions(profileId: Int) = flowOf(listOf(
                Transaction(
                    id = 1L,
                    title = "Lunch",
                    amount = BigDecimal("50"),
                    category = "Food",
                    categoryId = 1L,
                    timestamp = System.currentTimeMillis(),
                    currency = "USD",
                    transactionType = "expense",
                    accountId = "acc1"
                ),
                Transaction(
                    id = 2L,
                    title = "Subscription",
                    amount = BigDecimal("20"),
                    category = "Entertainment",
                    categoryId = 2L,
                    timestamp = System.currentTimeMillis(),
                    currency = "USD",
                    transactionType = "expense",
                    accountId = "acc1",
                    recurring = "monthly"
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

        budgetUseCases = BudgetUseCases(budgetRepository, transactionRepository)
    }

    @Test
    fun `getBudgetsWithAnalysis calculates spent correctly`() = runTest {
        val analysis = budgetUseCases.getBudgetsWithAnalysis(1).first()
        assertEquals(1, analysis.size)
        val foodBudget = analysis[0]
        assertEquals(BigDecimal("50"), foodBudget.budget.spent)
        assertEquals(BigDecimal("50"), foodBudget.variableSpent)
        assertEquals(BigDecimal.ZERO, foodBudget.fixedSpent)
    }

    @Test
    fun `getBudgetsWithAnalysis identifies fixed spending`() = runTest {
        val budgetRepositoryWithFixed = object : BudgetRepository {
            override fun getBudgets() = flowOf(listOf(
                Budget(
                    id = "2",
                    name = "All",
                    limit = BigDecimal("1000"),
                    spent = BigDecimal.ZERO,
                    period = BudgetPeriod.MONTHLY,
                    filter = TransactionFilter()
                )
            ))
            override suspend fun getBudgetById(id: String): Budget? = null
            override suspend fun upsertBudget(budget: Budget) {}
            override suspend fun deleteBudget(id: String) {}
            override suspend fun evaluateBudgets() {}
        }
        
        val useCases = BudgetUseCases(budgetRepositoryWithFixed, transactionRepository)
        val analysis = useCases.getBudgetsWithAnalysis(1).first()
        val allBudget = analysis[0]
        
        assertEquals(BigDecimal("70"), allBudget.budget.spent)
        assertEquals(BigDecimal("20"), allBudget.fixedSpent)
        assertEquals(BigDecimal("50"), allBudget.variableSpent)
    }
}
