package com.osscabinet.ossWallet.data.local.dao

import androidx.room.*
import com.osscabinet.ossWallet.data.local.entity.LoanPaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanPaymentDao {
    @Query("SELECT * FROM loan_payments WHERE loanId = :loanId ORDER BY date DESC")
    fun getPaymentsForLoan(loanId: Long): Flow<List<LoanPaymentEntity>>

    @Query("SELECT * FROM loan_payments WHERE loanId = :loanId ORDER BY date DESC")
    suspend fun getPaymentsForLoanSync(loanId: Long): List<LoanPaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: LoanPaymentEntity): Long

    @Query("SELECT * FROM loan_payments")
    suspend fun getAllPaymentsSync(): List<LoanPaymentEntity>

    @Query("DELETE FROM loan_payments")
    suspend fun clearAll()

    @Update
    suspend fun update(payment: LoanPaymentEntity)

    @Query("DELETE FROM loan_payments WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM loan_payments WHERE loanId = :loanId")
    suspend fun deleteAllForLoan(loanId: Long)
}
