package com.osscabinet.ossWallet.util

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.osscabinet.ossWallet.OssWalletApp
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
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

        val due = repo.getDue(profileId, now)
        due.filter { it.status == PlannedPaymentStatus.ACTIVE }.forEach { payment ->
            val nextDate = PlannedPaymentService.calculateNextDueDate(payment, now)
            if (nextDate != null) {
                repo.update(payment.copy(nextDueDate = nextDate, lastTriggeredDate = now))
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
