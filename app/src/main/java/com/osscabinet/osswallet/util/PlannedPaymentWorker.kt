package com.osscabinet.ossWallet.util

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.osscabinet.ossWallet.OssWalletApp
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.domain.model.dto.TransactionRequest
import com.osscabinet.ossWallet.domain.service.management.PlannedPaymentService
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class PlannedPaymentWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as OssWalletApp).container
        val repo = container.plannedPaymentRepository
        val settingsRepo = container.settingsRepository
        val profileId = settingsRepo.currentProfileId.first()
        val now = System.currentTimeMillis()

        val allCategories = container.categoryRepository.getAllCategoriesSync()
        val due = repo.getDue(profileId, now)
        
        due.filter { it.status == PlannedPaymentStatus.ACTIVE }.forEach { payment ->
            if (payment.autoConfirm) {
                val typeStr = when (payment.transactionType) {
                    1 -> "income"
                    2 -> "transfer"
                    else -> "expense"
                }
                val amount = if (typeStr == "income" || typeStr == "transfer") {
                    payment.amount.abs()
                } else {
                    payment.amount.abs().negate()
                }

                val request = TransactionRequest(
                    name = allCategories.find { it.id == payment.categoryId }?.name ?: "Planned Payment",
                    amount = amount,
                    transactionType = typeStr,
                    accountId = payment.accountId ?: "",
                    currency = payment.currency,
                    categoryId = payment.categoryId,
                    tags = payment.tags,
                    paymentMethod = payment.paymentMethod ?: PaymentMethod.CASH,
                    timestamp = payment.schedule.nextDueDate,
                    payee = payment.payee ?: "",
                    note = payment.note ?: "",
                    latitude = payment.latitude,
                    longitude = payment.longitude,
                    warrantyExpiration = payment.warrantyExpiration,
                    status = payment.transactionStatus,
                    sourcePlannedPaymentId = payment.id,
                    isTransfer = payment.transactionType == 2,
                    targetAccountId = payment.targetAccountId,
                    targetCurrency = payment.targetCurrency,
                    transferRate = payment.transferRate
                )

                val txId = container.transactionManager.handleSaveTransaction(null, request, profileId)

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

            val nextDate = PlannedPaymentService.calculateNextDueDate(payment, payment.schedule.nextDueDate)
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
