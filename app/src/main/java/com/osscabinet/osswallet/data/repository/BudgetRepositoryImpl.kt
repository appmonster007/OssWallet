package com.osscabinet.ossWallet.data.repository

import com.osscabinet.ossWallet.data.local.dao.BudgetDao
import com.osscabinet.ossWallet.data.mapper.toDomain
import com.osscabinet.ossWallet.data.mapper.toEntity
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.repository.BudgetRepository
import com.osscabinet.ossWallet.domain.repository.FilterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BudgetRepositoryImpl(
    private val dao: BudgetDao,
    private val filterRepository: FilterRepository
) : BudgetRepository {

    override fun getActiveBudgets(profileId: Int): Flow<List<Budget>> =
        dao.getActiveBudgets(profileId).map { it.map { e -> e.toDomain() } }

    override fun getAllBudgets(profileId: Int): Flow<List<Budget>> =
        dao.getAllBudgets(profileId).map { it.map { e -> e.toDomain() } }

    override suspend fun getActiveBudgetsSync(profileId: Int): List<Budget> =
        dao.getActiveBudgetsSync(profileId).map { it.toDomain() }

    override suspend fun getById(id: Long): Budget? =
        dao.getById(id)?.toDomain()

    override suspend fun insert(budget: Budget): Long {
        val filterId = filterRepository.insertFilter(
            Filter(
                name = "Budget Filter: ${budget.name}",
                filter = TransactionFilter(
                    categoryIds = budget.categoryIds,
                    accountIds = budget.accountIds,
                    tags = budget.tags,
                    paymentMethods = budget.paymentMethods,
                    statuses = budget.statuses,
                    linkedTransactionIds = budget.linkedTransactionIds,
                    transactionType = if (budget.trackTypes.size == 1) budget.trackTypes.first() else null
                )
            )
        )
        return dao.insert(budget.toEntity(filterId))
    }

    override suspend fun update(budget: Budget) {
        val existing = dao.getById(budget.id)
        if (existing != null) {
            filterRepository.updateFilter(
                Filter(
                    id = existing.budget.filterId,
                    name = "Budget Filter: ${budget.name}",
                    filter = TransactionFilter(
                        categoryIds = budget.categoryIds,
                        accountIds = budget.accountIds,
                        tags = budget.tags,
                        paymentMethods = budget.paymentMethods,
                        statuses = budget.statuses,
                        linkedTransactionIds = budget.linkedTransactionIds,
                        transactionType = if (budget.trackTypes.size == 1) budget.trackTypes.first() else null
                    )
                )
            )
            dao.update(budget.toEntity(existing.budget.filterId))
        }
    }

    override suspend fun delete(id: Long) {
        val existing = dao.getById(id)
        if (existing != null) {
            dao.deleteById(id)
            filterRepository.deleteFilter(existing.filter.toDomain())
        }
    }
}
