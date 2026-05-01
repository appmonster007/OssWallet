package com.osscabinet.ossWallet.data.local.dao

import androidx.room.*
import com.osscabinet.ossWallet.data.local.entity.PlannedPaymentOccurrenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannedPaymentOccurrenceDao {

    @Query("SELECT * FROM planned_payment_occurrences WHERE plannedPaymentId = :plannedPaymentId ORDER BY scheduledDate ASC")
    fun getOccurrencesForPayment(plannedPaymentId: Long): Flow<List<PlannedPaymentOccurrenceEntity>>

    @Query("SELECT * FROM planned_payment_occurrences WHERE plannedPaymentId = :plannedPaymentId AND scheduledDate = :scheduledDate LIMIT 1")
    suspend fun getOccurrenceForDate(plannedPaymentId: Long, scheduledDate: Long): PlannedPaymentOccurrenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PlannedPaymentOccurrenceEntity): Long

    @Update
    suspend fun update(entity: PlannedPaymentOccurrenceEntity)

    @Query("UPDATE planned_payment_occurrences SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)
}
