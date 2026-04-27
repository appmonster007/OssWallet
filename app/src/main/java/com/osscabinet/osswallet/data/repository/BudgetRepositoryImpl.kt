package com.osscabinet.ossWallet.data.repository

import com.osscabinet.ossWallet.data.local.dao.BudgetDao
import com.osscabinet.ossWallet.data.local.dao.TransactionDao
import com.osscabinet.ossWallet.data.mapper.toDomain
import com.osscabinet.ossWallet.data.mapper.toEntity
import com.osscabinet.ossWallet.data.mapper.toDomainList
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.repository.BudgetRepository
import com.osscabinet.ossWallet.domain.service.FilterService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

class BudgetRepositoryImpl(
    private val budgetDao: BudgetDao,
    private val transactionDao: TransactionDao
) : BudgetRepository {

    override fun getBudgets(): Flow<List<Budget>> = budgetDao.getAllBudgets().map { entities ->
        entities.map { it.toDomain() }
    }

    override suspend fun getBudgetById(id: String): Budget? {
        return budgetDao.getBudgetById(id)?.toDomain()
    }

    override suspend fun upsertBudget(budget: Budget) {
        budgetDao.upsertBudget(budget.toEntity())
    }

    override suspend fun deleteBudget(id: String) {
        budgetDao.deleteBudget(id)
    }

    override suspend fun evaluateBudgets() {
        val budgets = budgetDao.getAllBudgets().first().map { it.toDomain() }
        val transactions = transactionDao.getAllTransactions(1).first().toDomainList()
        
        budgets.forEach { budget ->
            val matchingTransactions = FilterService.filterTransactions(transactions, budget.filter)
            // Only count expenses for budget
            val totalSpent = matchingTransactions
                .filter { it.transactionType == "expense" }
                .sumOf { it.amount.abs() }
            
            upsertBudget(budget.copy(spent = totalSpent, lastEvaluated = System.currentTimeMillis()))
        }
    }
}
