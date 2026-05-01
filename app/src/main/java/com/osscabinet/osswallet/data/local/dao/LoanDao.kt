package com.osscabinet.ossWallet.data.local.dao

import androidx.room.*
import com.osscabinet.ossWallet.data.local.entity.LoanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanDao {
    @Query("SELECT * FROM loans WHERE profileId = :profileId AND status = 'ACTIVE' ORDER BY startDate DESC")
    fun getActiveLoans(profileId: Int): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE profileId = :profileId ORDER BY startDate DESC")
    fun getAllLoans(profileId: Int): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE profileId = :profileId AND status = 'ACTIVE' ORDER BY startDate DESC")
    suspend fun getActiveLoansSync(profileId: Int): List<LoanEntity>

    @Query("SELECT * FROM loans WHERE id = :id")
    suspend fun getLoanById(id: Long): LoanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(loan: LoanEntity): Long

    @Update
    suspend fun update(loan: LoanEntity)

    @Query("DELETE FROM loans WHERE id = :id")
    suspend fun deleteById(id: Long)
}
