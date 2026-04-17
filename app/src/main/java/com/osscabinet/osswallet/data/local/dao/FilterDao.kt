package com.osscabinet.ossWallet.data.local.dao

import androidx.room.*
import com.osscabinet.ossWallet.data.local.entity.FilterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FilterDao {
    @Query("SELECT * FROM transaction_filters ORDER BY name ASC")
    fun getAllFilters(): Flow<List<FilterEntity>>

    @Query("SELECT * FROM transaction_filters WHERE id = :id")
    suspend fun getFilterById(id: Long): FilterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFilter(filter: FilterEntity)

    @Update
    suspend fun updateFilter(filter: FilterEntity)

    @Delete
    suspend fun deleteFilter(filter: FilterEntity)
}
