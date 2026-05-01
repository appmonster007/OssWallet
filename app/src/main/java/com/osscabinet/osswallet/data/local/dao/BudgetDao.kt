package com.osscabinet.ossWallet.data.local.dao

import androidx.room.*
import com.osscabinet.ossWallet.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budgets WHERE profileId = :profileId AND isArchived = 0 ORDER BY name ASC")
    fun getActiveBudgets(profileId: Int): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE profileId = :profileId ORDER BY name ASC")
    fun getAllBudgets(profileId: Int): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE profileId = :profileId AND isArchived = 0 ORDER BY name ASC")
    suspend fun getActiveBudgetsSync(profileId: Int): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getById(id: Long): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BudgetEntity): Long

    @Update
    suspend fun update(entity: BudgetEntity)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteById(id: Long)
}
