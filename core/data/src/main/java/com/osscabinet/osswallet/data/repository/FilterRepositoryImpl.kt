package com.osscabinet.ossWallet.data.repository

import com.osscabinet.ossWallet.data.local.dao.FilterDao
import com.osscabinet.ossWallet.data.mapper.toDomain
import com.osscabinet.ossWallet.data.mapper.toEntity
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.repository.FilterRepository
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class FilterRepositoryImpl @Inject constructor(
    private val filterDao: FilterDao
) : FilterRepository {
    override fun getAllFilters(): Flow<List<Filter>> =
        filterDao.getAllFilters().map { list -> list.map { it.toDomain() } }

    override suspend fun getFilterById(id: Long): Filter? =
        filterDao.getFilterById(id)?.toDomain()

    override suspend fun insertFilter(filter: Filter): Long =
        filterDao.insertFilter(filter.toEntity())

    override suspend fun updateFilter(filter: Filter) =
        filterDao.updateFilter(filter.toEntity())

    override suspend fun deleteFilter(filter: Filter) =
        filterDao.deleteFilter(filter.toEntity())

    override suspend fun getFilterUsageCount(filterId: Long): Int =
        filterDao.getFilterUsageCount(filterId)
}
