package com.osscabinet.ossWallet.data.local.dao

import androidx.room.*
import com.osscabinet.ossWallet.data.local.entity.LoanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanDao {
    @Query("SELECT * FROM loan_metadata")
    fun getAllLoans(): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loan_metadata WHERE accountId = :accountId")
    suspend fun getLoanByAccountId(accountId: String): LoanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLoan(loan: LoanEntity)

    @Delete
    suspend fun deleteLoan(loan: LoanEntity)
}
