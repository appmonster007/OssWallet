package com.osscabinet.ossWallet.domain.repository

import com.osscabinet.ossWallet.domain.model.OccurrenceStatus
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.PlannedPaymentOccurrence
import kotlinx.coroutines.flow.Flow

interface PlannedPaymentRepository {
    fun getActivePlannedPayments(profileId: Int): Flow<List<PlannedPayment>>
    fun getAllPlannedPayments(profileId: Int): Flow<List<PlannedPayment>>
    suspend fun getAllSync(profileId: Int): List<PlannedPayment>
    suspend fun getDue(profileId: Int, now: Long): List<PlannedPayment>
    suspend fun getById(id: Long): PlannedPayment?
    fun getPaymentById(id: Long): Flow<PlannedPayment?>
    suspend fun insert(payment: PlannedPayment): Long
    suspend fun update(payment: PlannedPayment)
    suspend fun delete(id: Long)
    fun getOccurrences(plannedPaymentId: Long): Flow<List<PlannedPaymentOccurrence>>
    suspend fun upsertOccurrence(occurrence: PlannedPaymentOccurrence): Long
    suspend fun updateOccurrenceStatus(occurrenceId: Long, status: OccurrenceStatus)
    suspend fun getOccurrenceForDate(
        plannedPaymentId: Long,
        scheduledDate: Long
    ): PlannedPaymentOccurrence?

    suspend fun isTemplateUsedInPlannedPayments(templateId: Long): Boolean
    suspend fun getCountByTemplateId(templateId: Long): Int
}
