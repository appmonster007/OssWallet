package com.osscabinet.ossWallet.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.osscabinet.ossWallet.data.local.entity.BudgetEntity
import com.osscabinet.ossWallet.data.local.entity.BudgetWithFilter
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Transaction
    @Query("SELECT * FROM budgets WHERE profileId = :profileId AND isArchived = 0 ORDER BY name ASC")
    fun getActiveBudgets(profileId: Int): Flow<List<BudgetWithFilter>>

    @Transaction
    @Query("SELECT * FROM budgets WHERE profileId = :profileId ORDER BY name ASC")
    fun getAllBudgets(profileId: Int): Flow<List<BudgetWithFilter>>

    @Transaction
    @Query("SELECT * FROM budgets WHERE profileId = :profileId AND isArchived = 0 ORDER BY name ASC")
    suspend fun getActiveBudgetsSync(profileId: Int): List<BudgetWithFilter>

    /**
     * Cross-profile bulk read. ONLY for backup/export flows. Profile-scoped UI must
     * use [getActiveBudgetsSync] or [getAllBudgets] with a profileId.
     */
    @Transaction
    @Query("SELECT * FROM budgets")
    suspend fun getAllBudgetsSync(): List<BudgetWithFilter>

    @Query("DELETE FROM budgets")
    suspend fun clearAll()

    @Transaction
    @Query("SELECT * FROM budgets WHERE id = :id AND profileId = :profileId")
    suspend fun getById(id: Long, profileId: Int): BudgetWithFilter?

    /** For internal use by [delete] only — fetches filterId without profile check. */
    @Transaction
    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getByIdInternal(id: Long): BudgetWithFilter?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BudgetEntity): Long

    @Update
    suspend fun update(entity: BudgetEntity)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteById(id: Long)
}
