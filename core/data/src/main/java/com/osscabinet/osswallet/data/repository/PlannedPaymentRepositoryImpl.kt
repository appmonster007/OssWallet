package com.osscabinet.ossWallet.data.repository

import com.osscabinet.ossWallet.data.local.dao.PlannedPaymentDao
import com.osscabinet.ossWallet.data.local.dao.PlannedPaymentOccurrenceDao
import com.osscabinet.ossWallet.data.mapper.toDomain
import com.osscabinet.ossWallet.data.mapper.toEntity
import com.osscabinet.ossWallet.domain.model.OccurrenceStatus
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.PlannedPaymentOccurrence
import com.osscabinet.ossWallet.domain.repository.PlannedPaymentRepository
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class PlannedPaymentRepositoryImpl @Inject constructor(
    private val dao: PlannedPaymentDao,
    private val occurrenceDao: PlannedPaymentOccurrenceDao,
) : PlannedPaymentRepository {

    override fun getActivePlannedPayments(profileId: Int): Flow<List<PlannedPayment>> =
        dao.getActivePlannedPayments(profileId).map { list -> list.map { it.toDomain() } }

    override fun getAllPlannedPayments(profileId: Int): Flow<List<PlannedPayment>> =
        dao.getAllPlannedPayments(profileId).map { list -> list.map { it.toDomain() } }

    override suspend fun getAllSync(profileId: Int): List<PlannedPayment> =
        dao.getAllPlannedPaymentsSync(profileId).map { it.toDomain() }

    override suspend fun getDue(profileId: Int, now: Long): List<PlannedPayment> =
        dao.getDuePlannedPayments(profileId, now).map { it.toDomain() }

    override suspend fun getById(id: Long): PlannedPayment? =
        dao.getById(id)?.toDomain()

    override fun getPaymentById(id: Long): Flow<PlannedPayment?> =
        dao.getByIdFlow(id).map { it?.toDomain() }

    override suspend fun insert(payment: PlannedPayment): Long =
        dao.insert(payment.toEntity())

    override suspend fun update(payment: PlannedPayment) =
        dao.update(payment.toEntity())

    override suspend fun delete(id: Long) =
        dao.deleteById(id)

    override fun getOccurrences(plannedPaymentId: Long): Flow<List<PlannedPaymentOccurrence>> =
        occurrenceDao.getOccurrencesForPayment(plannedPaymentId)
            .map { list -> list.map { it.toDomain() } }

    override suspend fun upsertOccurrence(occurrence: PlannedPaymentOccurrence): Long =
        occurrenceDao.insert(occurrence.toEntity())

    override suspend fun updateOccurrenceStatus(occurrenceId: Long, status: OccurrenceStatus) =
        occurrenceDao.updateStatus(occurrenceId, status)

    override suspend fun getOccurrenceForDate(
        plannedPaymentId: Long,
        scheduledDate: Long
    ): PlannedPaymentOccurrence? =
        occurrenceDao.getOccurrenceForDate(plannedPaymentId, scheduledDate)?.toDomain()

    override suspend fun isTemplateUsedInPlannedPayments(templateId: Long): Boolean =
        dao.getCountByTemplateId(templateId) > 0

    override suspend fun getCountByTemplateId(templateId: Long): Int =
        dao.getCountByTemplateId(templateId)
}
