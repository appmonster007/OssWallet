package com.osscabinet.ossWallet.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.osscabinet.ossWallet.data.local.entity.LoanEntity
import com.osscabinet.ossWallet.data.local.entity.LoanWithFilter
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanDao {
    @Transaction
    @Query("SELECT * FROM loans WHERE profileId = :profileId AND status = 'ACTIVE' ORDER BY startDate DESC")
    fun getActiveLoans(profileId: Int): Flow<List<LoanWithFilter>>

    @Transaction
    @Query("SELECT * FROM loans WHERE profileId = :profileId ORDER BY startDate DESC")
    fun getAllLoans(profileId: Int): Flow<List<LoanWithFilter>>

    @Transaction
    @Query("SELECT * FROM loans WHERE profileId = :profileId AND status = 'ACTIVE' ORDER BY startDate DESC")
    suspend fun getActiveLoansSync(profileId: Int): List<LoanWithFilter>

    @Transaction
    @Query("SELECT * FROM loans")
    suspend fun getAllLoansSync(): List<LoanWithFilter>

    @Query("DELETE FROM loans")
    suspend fun clearAll()

    @Transaction
    @Query("SELECT * FROM loans WHERE id = :id")
    suspend fun getLoanById(id: Long): LoanWithFilter?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(loan: LoanEntity): Long

    @Update
    suspend fun update(loan: LoanEntity)

    @Query("DELETE FROM loans WHERE id = :id")
    suspend fun deleteById(id: Long)
}
