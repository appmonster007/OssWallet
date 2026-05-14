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
import java.math.BigDecimal
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
                val template = payment.template
                val typeStr = when (template.transactionType) {
                    1 -> "income"
                    2 -> "transfer"
                    else -> "expense"
                }
                val amount = if (typeStr == "income") template.amount.abs() else template.amount.abs().negate()

                val request = TransactionRequest(
                    name = allCategories.find { it.id == template.categoryId }?.name ?: "Planned Payment",
                    amount = amount,
                    transactionType = typeStr,
                    accountId = template.accountId ?: "",
                    currency = template.currency,
                    categoryId = template.categoryId,
                    tags = template.tags,
                    paymentMethod = template.paymentMethod ?: PaymentMethod.CASH,
                    timestamp = payment.schedule.nextDueDate,
                    payee = template.payee ?: "",
                    note = template.note ?: "",
                    latitude = template.latitude,
                    longitude = template.longitude,
                    warrantyExpiration = template.warrantyExpiration,
                    status = template.transactionStatus,
                    sourcePlannedPaymentId = payment.id,
                    isTransfer = template.transactionType == 2,
                    targetAccountId = template.targetAccountId,
                    targetCurrency = template.targetCurrency,
                    transferRate = template.transferRate
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
                            lastTriggeredDate = now
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
