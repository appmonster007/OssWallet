package com.osscabinet.ossWallet.reminder

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.osscabinet.ossWallet.MainActivity
import com.osscabinet.ossWallet.R
import com.osscabinet.ossWallet.domain.service.reminder.ReminderEvent
import com.osscabinet.ossWallet.domain.service.reminder.ReminderKind

object ReminderNotifier {
    const val CHANNEL_PLANNED = "planned_payments"
    const val CHANNEL_BUDGETS = "budget_alerts"
    const val ACTION_SNOOZE = "com.osscabinet.ossWallet.reminder.SNOOZE"
    const val ACTION_MARK_PAID = "com.osscabinet.ossWallet.reminder.MARK_PAID"

    /** True when a notification posted now would actually be shown. */
    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun post(context: Context, event: ReminderEvent) {
        val isBudget = event.kind == ReminderKind.BUDGET_WARNING || event.kind == ReminderKind.BUDGET_EXCEEDED
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (isBudget) {
                putExtra(MainActivity.EXTRA_WIDGET_NAV, "budget_detail")
                putExtra(MainActivity.EXTRA_WIDGET_ENTITY_ID, event.targetId.toString())
            }
        }
        val id = event.key.hashCode()
        val pending = PendingIntent.getActivity(
            context, id, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(
            context, if (isBudget) CHANNEL_BUDGETS else CHANNEL_PLANNED
        )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(event.title)
            .setContentText(event.body)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .addAction(0, "Snooze", actionIntent(context, ACTION_SNOOZE, event))
        // Only a planned payment has something to mark as paid.
        if (event.kind == ReminderKind.PLANNED_PAYMENT && event.dueDate != null) {
            builder.addAction(0, "Mark paid", actionIntent(context, ACTION_MARK_PAID, event))
        }
        val notification = builder.build()
        // Guarded by canPost() at the call site; SecurityException would mean permission was revoked mid-run.
        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }

    /** A broadcast to [ReminderActionReceiver] carrying the whole event, so it can act without a lookup. */
    private fun actionIntent(context: Context, action: String, event: ReminderEvent): PendingIntent {
        val intent = Intent(context, ReminderActionReceiver::class.java).setAction(action)
        event.toMap().forEach { (k, v) -> intent.putExtra(k, v) }
        // Extras don't distinguish PendingIntents, so the request code has to be unique per action and event.
        return PendingIntent.getBroadcast(
            context, (event.key + action).hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun cancel(context: Context, event: ReminderEvent) {
        NotificationManagerCompat.from(context).cancel(event.key.hashCode())
    }

    /** Replaces the reminder with a short-lived outcome ("Rent marked paid"). */
    fun postOutcome(context: Context, event: ReminderEvent, text: String) {
        val notification = NotificationCompat.Builder(
            context, if (event.kind == ReminderKind.PLANNED_PAYMENT) CHANNEL_PLANNED else CHANNEL_BUDGETS
        )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(text)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setTimeoutAfter(5_000)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(event.key.hashCode(), notification) }
    }
}
