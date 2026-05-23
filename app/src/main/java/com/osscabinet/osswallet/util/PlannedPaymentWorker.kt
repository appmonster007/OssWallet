package com.osscabinet.ossWallet.util

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.osscabinet.ossWallet.di.PlannedPaymentWorkerEntryPoint
import com.osscabinet.ossWallet.domain.model.OccurrenceStatus
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.PlannedPaymentOccurrence
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.dto.TransactionRequest
import com.osscabinet.ossWallet.domain.service.management.PlannedPaymentService
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class PlannedPaymentWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val ep = dagger.hilt.android.EntryPointAccessors.fromApplication(
            applicationContext,
            PlannedPaymentWorkerEntryPoint::class.java
        )
        val repo = ep.plannedPaymentRepository()
        val settingsRepo = ep.settingsRepository()
        val profileId = settingsRepo.currentProfileId.first()
        val now = System.currentTimeMillis()

        val allCategories = ep.categoryRepository().getAllCategoriesSync()
        val due = repo.getDue(profileId).filter { it.schedule.nextDueDate <= now }

        due.filter { it.status == PlannedPaymentStatus.ACTIVE }.forEach { payment ->
            if (payment.autoConfirm) {
                val amount =
                    if (payment.transactionType == TransactionType.INCOME || payment.transactionType == TransactionType.TRANSFER) {
                        payment.amount.abs()
                    } else {
                        payment.amount.abs().negate()
                    }

                val request = TransactionRequest(
                    name = allCategories.find { it.id == payment.categoryId }?.name
                        ?: "Planned Payment",
                    amount = amount,
                    transactionType = payment.transactionType,
                    accountId = payment.accountId ?: "",
                    currency = payment.currency,
                    categoryId = payment.categoryId,
                    tags = payment.tags,
                    paymentMethod = payment.paymentMethod ?: PaymentMethod.CASH,
                    timestamp = payment.schedule.nextDueDate,
                    payee = payment.payee ?: "",
                    note = payment.note ?: "",
                    gpsLocation = payment.gpsLocation,
                    warrantyExpiration = payment.warrantyExpiration,
                    status = payment.transactionStatus,
                    sourcePlannedPaymentId = payment.id,
                    isTransfer = payment.transactionType == TransactionType.TRANSFER,
                    targetAccountId = payment.targetAccountId,
                    targetCurrency = payment.targetCurrency,
                    transferRate = payment.transferRate
                )

                val txId = ep.transactionManager().handleSaveTransaction(null, request, profileId)

                // Mark occurrence as confirmed
                repo.upsertOccurrence(
                    PlannedPaymentOccurrence(
                        id = 0,
                        plannedPaymentId = payment.id,
                        scheduledDate = payment.schedule.nextDueDate,
                        status = OccurrenceStatus.CONFIRMED,
                        generatedTransactionId = txId
                    )
                )
            }

            val nextDate =
                PlannedPaymentService.calculateNextDueDate(payment, payment.schedule.nextDueDate)
            if (nextDate != null) {
                repo.update(
                    payment.copy(
                        schedule = payment.schedule.copy(
                            nextDueDate = nextDate,
                            lastTriggeredDate = payment.schedule.nextDueDate
                        )
                    )
                )
            } else {
                repo.update(payment.copy(status = PlannedPaymentStatus.COMPLETED))
            }
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "planned_payment_worker"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<PlannedPaymentWorker>(1, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
