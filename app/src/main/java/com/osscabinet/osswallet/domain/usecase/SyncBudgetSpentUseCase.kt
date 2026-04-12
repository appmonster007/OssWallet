package com.osscabinet.ossWallet.domain.usecase

import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.repository.BudgetRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class SyncBudgetSpentUseCase(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(profileId: Int = 1): Flow<List<Budget>> {
        return combine(
            budgetRepository.getBudgets(),
            transactionRepository.getAllTransactions(profileId)
        ) { budgets, transactions ->
            budgets.map { budget ->
                val spent = transactions
                    .filter { !it.isIncome && it.category.equals(budget.category, ignoreCase = true) }
                    .sumOf { it.amount }
                budget.copy(spent = Math.abs(spent))
            }
        }
    }
}
