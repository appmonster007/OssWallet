package com.osscabinet.ossWallet.reminder

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.osscabinet.ossWallet.domain.repository.ReminderRepository
import com.osscabinet.ossWallet.domain.service.reminder.ReminderEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/** Posts a reminder again after the user tapped Snooze, unless it no longer applies by then. */
@HiltWorker
class SnoozedReminderWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted params: WorkerParameters,
    private val reminderRepository: ReminderRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val event = ReminderEvent.fromMap(inputData.keyValueMap.mapValues { it.value?.toString() }) ?: return Result.success()
        val config = reminderRepository.config.first()
        if (!config.enabled || !ReminderNotifier.canPost(appContext)) return Result.success()

        // Still due? Paid or settled in the meantime means the same event no longer comes up.
        // The time-of-day gate is dropped: the user asked for this one.
        val stillDue = planReminders(appContext, config.copy(hourOfDay = 0), System.currentTimeMillis(), emptySet())
            .any { it.key == event.key }
        if (stillDue) ReminderNotifier.post(appContext, event)
        return Result.success()
    }

    companion object {
        const val SNOOZE_HOURS = 3L

        private fun workName(event: ReminderEvent) = "reminder_snooze_${event.key}"

        fun schedule(context: Context, event: ReminderEvent) {
            val data = Data.Builder().apply { event.toMap().forEach { (k, v) -> putString(k, v) } }.build()
            val request = OneTimeWorkRequestBuilder<SnoozedReminderWorker>()
                .setInitialDelay(SNOOZE_HOURS, TimeUnit.HOURS)
                .setInputData(data)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(workName(event), ExistingWorkPolicy.REPLACE, request)
        }

        fun cancel(context: Context, event: ReminderEvent) {
            WorkManager.getInstance(context).cancelUniqueWork(workName(event))
        }
    }
}
