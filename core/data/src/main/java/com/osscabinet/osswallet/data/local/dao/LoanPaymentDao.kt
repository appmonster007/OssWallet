package com.osscabinet.ossWallet.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.osscabinet.ossWallet.data.local.entity.LoanPaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanPaymentDao {
    @Query("SELECT * FROM loan_payments WHERE loanId = :loanId ORDER BY date DESC")
    fun getPaymentsForLoan(loanId: Long): Flow<List<LoanPaymentEntity>>

    @Query("SELECT * FROM loan_payments WHERE loanId = :loanId ORDER BY date DESC")
    suspend fun getPaymentsForLoanSync(loanId: Long): List<LoanPaymentEntity>

    /**
     * Cross-profile bulk read. ONLY for backup/export flows. Profile-scoped UI must
     * use [getAllPaymentsByProfile] with a profileId.
     */
    @Query("SELECT * FROM loan_payments ORDER BY date DESC")
    fun getAllPayments(): Flow<List<LoanPaymentEntity>>

    @Query("""
        SELECT lp.* FROM loan_payments lp
        INNER JOIN loans l ON lp.loanId = l.id
        WHERE l.profileId = :profileId
        ORDER BY lp.date DESC
    """)
    fun getAllPaymentsByProfile(profileId: Int): Flow<List<LoanPaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: LoanPaymentEntity): Long

    /**
     * Cross-profile bulk read. ONLY for backup/export flows.
     */
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
