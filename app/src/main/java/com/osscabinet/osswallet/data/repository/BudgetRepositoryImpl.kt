package com.osscabinet.ossWallet.data.repository

import com.osscabinet.ossWallet.data.local.dao.BudgetDao
import com.osscabinet.ossWallet.data.mapper.toDomain
import com.osscabinet.ossWallet.data.mapper.toEntity
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BudgetRepositoryImpl(
    private val dao: BudgetDao
) : BudgetRepository {

    override fun getActiveBudgets(profileId: Int): Flow<List<Budget>> =
        dao.getActiveBudgets(profileId).map { it.map { e -> e.toDomain() } }

    override fun getAllBudgets(profileId: Int): Flow<List<Budget>> =
        dao.getAllBudgets(profileId).map { it.map { e -> e.toDomain() } }

    override suspend fun getActiveBudgetsSync(profileId: Int): List<Budget> =
        dao.getActiveBudgetsSync(profileId).map { it.toDomain() }

    override suspend fun getById(id: Long): Budget? =
        dao.getById(id)?.toDomain()

    override suspend fun insert(budget: Budget): Long =
        dao.insert(budget.toEntity())

    override suspend fun update(budget: Budget) =
        dao.update(budget.toEntity())

    override suspend fun delete(id: Long) =
        dao.deleteById(id)
}
