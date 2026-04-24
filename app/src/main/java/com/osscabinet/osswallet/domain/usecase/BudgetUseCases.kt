package com.osscabinet.ossWallet.domain.usecase

import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.repository.BudgetRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.math.BigDecimal

class BudgetUseCases(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository
) {
    fun getBudgets(): Flow<List<Budget>> = budgetRepository.getBudgets()

    suspend fun upsertBudget(budget: Budget) = budgetRepository.upsertBudget(budget)

    fun syncBudgetSpent(profileId: Int = 1): Flow<List<Budget>> {
        return combine(
            budgetRepository.getBudgets(),
            transactionRepository.getAllTransactions(profileId)
        ) { budgets, transactions ->
            budgets.map { budget ->
                val spent = transactions
                    .filter { it.amount.signum() < 0 && it.category.equals(budget.category, ignoreCase = true) }
                    .fold(BigDecimal.ZERO) { acc, t -> acc.add(t.amount.abs()) }
                budget.copy(spent = spent.abs())
            }
        }
    }
}
