package com.osscabinet.ossWallet.data.repository

import com.osscabinet.ossWallet.data.local.dao.BudgetDao
import com.osscabinet.ossWallet.data.local.entity.BudgetEntity
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BudgetRepositoryImpl(private val budgetDao: BudgetDao) : BudgetRepository {
    override fun getBudgets(): Flow<List<Budget>> = budgetDao.getAllBudgets().map { entities ->
        entities.map { it.toDomain() }
    }

    override suspend fun upsertBudget(budget: Budget) {
        budgetDao.upsertBudget(budget.toEntity())
    }

    override suspend fun deleteBudget(category: String) {
        budgetDao.deleteBudget(category)
    }

    private fun BudgetEntity.toDomain() = Budget(category, spent, limit, currency)
    private fun Budget.toEntity() = BudgetEntity(category, spent, limit, currency)
}
