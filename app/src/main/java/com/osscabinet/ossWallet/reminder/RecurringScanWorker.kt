package com.osscabinet.ossWallet.reminder

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.osscabinet.ossWallet.MainActivity
import com.osscabinet.ossWallet.R
import com.osscabinet.ossWallet.domain.repository.ReminderRepository
import com.osscabinet.ossWallet.domain.service.discovery.DiscoveredPattern
import com.osscabinet.ossWallet.domain.service.discovery.RecurringSuggestions
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Looks for repeated payments worth scheduling and, when it finds new ones the user hasn't seen,
 * posts one quiet note. Runs daily but speaks only about something new, so it is rarely heard.
 */
@HiltWorker
class RecurringScanWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted params: WorkerParameters,
    private val reminderRepository: ReminderRepository,
    private val recurringSuggestions: RecurringSuggestions
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val config = reminderRepository.config.first()
        if (!config.enabled || !config.suggestions) return Result.success()
        if (!ReminderNotifier.canPost(appContext)) return Result.success()
        // Same quiet hours as the other reminders; the next daily run will catch it.
        val hour = Instant.ofEpochMilli(System.currentTimeMillis()).atZone(ZoneId.systemDefault()).hour
        if (hour < config.hourOfDay) return Result.success()

        val sent = reminderRepository.sentKeys()
        val fresh = recurringSuggestions.currentUnseen().filter { "$KEY_PREFIX${it.key}" !in sent }
        if (fresh.isEmpty()) return Result.success()

        post(appContext, fresh)
        reminderRepository.markSent(fresh.map { "$KEY_PREFIX${it.key}" }, System.currentTimeMillis())
        return Result.success()
    }

    private fun post(context: Context, found: List<DiscoveredPattern>) {
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_WIDGET_NAV, "smart_suggestions")
        }
        val pending = PendingIntent.getActivity(
            context, NOTIFICATION_ID, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val names = found.map { it.payee.ifBlank { it.name } }
        val body = when {
            names.size <= 2 -> names.joinToString(" and ")
            else -> names.take(2).joinToString(", ") + " and ${names.size - 2} more"
        }
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(if (found.size == 1) "1 repeated payment could be scheduled" else "${found.size} repeated payments could be scheduled")
            .setContentText(body)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification) }
    }

    companion object {
        private const val WORK_NAME = "recurring_scan_worker"
        private const val CHANNEL = "recurring_suggestions"
        private const val NOTIFICATION_ID = 7301
        private const val KEY_PREFIX = "sug:"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<RecurringScanWorker>(1, TimeUnit.DAYS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }
    }
}
