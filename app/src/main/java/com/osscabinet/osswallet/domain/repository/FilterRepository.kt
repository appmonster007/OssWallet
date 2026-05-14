package com.osscabinet.ossWallet.domain.repository

import com.osscabinet.ossWallet.domain.model.Filter
import kotlinx.coroutines.flow.Flow

interface FilterRepository {
    fun getAllFilters(): Flow<List<Filter>>
    suspend fun getFilterById(id: Long): Filter?
    suspend fun insertFilter(filter: Filter): Long
    suspend fun updateFilter(filter: Filter)
    suspend fun deleteFilter(filter: Filter)
}
