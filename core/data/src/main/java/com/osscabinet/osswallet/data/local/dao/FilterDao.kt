package com.osscabinet.ossWallet.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.osscabinet.ossWallet.data.local.entity.FilterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FilterDao {
    @Query("SELECT * FROM transaction_filters ORDER BY name ASC")
    fun getAllFilters(): Flow<List<FilterEntity>>

    @Query("SELECT * FROM transaction_filters WHERE id = :id")
    suspend fun getFilterById(id: Long): FilterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFilter(filter: FilterEntity): Long

    @Query("SELECT * FROM transaction_filters")
    suspend fun getAllFiltersSync(): List<FilterEntity>

    @Query("DELETE FROM transaction_filters")
    suspend fun clearAll()

    @Update
    suspend fun updateFilter(filter: FilterEntity)

    @Delete
    suspend fun deleteFilter(filter: FilterEntity)

    @Query("SELECT (SELECT COUNT(*) FROM budgets WHERE filterId = :filterId) + (SELECT COUNT(*) FROM loans WHERE filterId = :filterId)")
    suspend fun getFilterUsageCount(filterId: Long): Int
}
