package com.osscabinet.ossWallet.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.osscabinet.ossWallet.data.local.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals WHERE profileId = :profileId ORDER BY completedAt IS NOT NULL, createdAt DESC")
    fun getAllGoals(profileId: Int): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE profileId = :profileId ORDER BY completedAt IS NOT NULL, createdAt DESC")
    suspend fun getAllGoalsSync(profileId: Int): List<GoalEntity>

    /** Cross-profile bulk read. ONLY for backup/export flows. */
    @Query("SELECT * FROM goals")
    suspend fun getAllGoalsForBackup(): List<GoalEntity>

    @Query("SELECT * FROM goals WHERE id = :id AND profileId = :profileId")
    suspend fun getById(id: Long, profileId: Int): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: GoalEntity): Long

    @Update
    suspend fun update(goal: GoalEntity)

    @Query("DELETE FROM goals WHERE id = :id AND profileId = :profileId")
    suspend fun deleteById(id: Long, profileId: Int)

    @Query("DELETE FROM goals")
    suspend fun clearAll()
}
