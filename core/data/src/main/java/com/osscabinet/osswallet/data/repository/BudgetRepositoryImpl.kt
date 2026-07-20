package com.osscabinet.ossWallet.data.repository

import com.osscabinet.ossWallet.data.local.dao.BudgetDao
import com.osscabinet.ossWallet.data.mapper.toDomain
import com.osscabinet.ossWallet.data.mapper.toEntity
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.repository.BudgetRepository
import com.osscabinet.ossWallet.domain.repository.FilterRepository
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val dao: BudgetDao,
    private val filterRepository: FilterRepository
) : BudgetRepository {

    override fun getActiveBudgets(profileId: Int): Flow<List<Budget>> =
        dao.getActiveBudgets(profileId).map { it.map { e -> e.toDomain() } }

    override fun getAllBudgets(profileId: Int): Flow<List<Budget>> =
        dao.getAllBudgets(profileId).map { it.map { e -> e.toDomain() } }

    override suspend fun getActiveBudgetsSync(profileId: Int): List<Budget> =
        dao.getActiveBudgetsSync(profileId).map { it.toDomain() }

    override suspend fun getById(id: Long, profileId: Int): Budget? =
        dao.getById(id, profileId)?.toDomain()

    override suspend fun insert(budget: Budget): Long {
        val filterId =
            budget.filterId ?: throw IllegalArgumentException("FilterId must be provided")
        return dao.insert(budget.toEntity(filterId))
    }

    override suspend fun update(budget: Budget) {
        val filterId =
            budget.filterId ?: throw IllegalArgumentException("FilterId must be provided")
        dao.update(budget.toEntity(filterId))
    }

    override suspend fun delete(id: Long) {
        val filterId = dao.getByIdInternal(id)?.budget?.filterId
        dao.deleteById(id)
        filterId?.let {
            filterRepository.getFilterById(it)?.let { f -> filterRepository.deleteFilter(f) }
        }
    }
}
