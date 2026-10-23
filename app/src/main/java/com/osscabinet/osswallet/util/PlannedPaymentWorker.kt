package com.osscabinet.ossWallet.util

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.osscabinet.ossWallet.di.PlannedPaymentWorkerEntryPoint
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
import com.osscabinet.ossWallet.domain.repository.PlannedPaymentRepository
import com.osscabinet.ossWallet.domain.service.management.PlannedPaymentConfirmer
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
        val profileId = ep.settingsRepository().currentProfileId.first()
        processDuePlannedPayments(
            repo = ep.plannedPaymentRepository(),
            confirmer = ep.plannedPaymentConfirmer(),
            profileId = profileId,
            now = System.currentTimeMillis()
        )
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

/** The worker's actual logic, pulled out of [PlannedPaymentWorker.doWork] so it's testable without a Context. */
internal suspend fun processDuePlannedPayments(
    repo: PlannedPaymentRepository,
    confirmer: PlannedPaymentConfirmer,
    profileId: Int,
    now: Long
) {
    val due = repo.getDue(profileId, now)

    due.filter { it.status == PlannedPaymentStatus.ACTIVE }.forEach { payment ->
        if (payment.autoConfirm) {
            // Delegate to PlannedPaymentConfirmer: the single place that builds the
            // transaction (honoring template overrides) and atomically advances the
            // occurrence/nextDueDate, so a WorkManager retry can't double-confirm.
            // Confirmed/AlreadyHandled already advanced nextDueDate; NeedsReview/NotFound
            // leave it due so the user resolves it from the app instead of it being
            // silently skipped here.
            confirmer.confirm(payment, payment.schedule.nextDueDate, profileId)
            return@forEach
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
}
