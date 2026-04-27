package com.osscabinet.ossWallet.data.local.dao

import androidx.room.*
import com.osscabinet.ossWallet.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getBudgetById(id: String): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudget(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudget(id: String)
}
