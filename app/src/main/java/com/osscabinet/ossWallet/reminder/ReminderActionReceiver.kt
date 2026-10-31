package com.osscabinet.ossWallet.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.service.management.ConfirmResult
import com.osscabinet.ossWallet.domain.service.management.PlannedPaymentConfirmer
import com.osscabinet.ossWallet.domain.service.reminder.ReminderEvent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Handles the Snooze and Mark paid buttons on reminder notifications. */
/** Hilt cannot inject a Kotlin BroadcastReceiver (its abstract onReceive can't be super-called), so look things up here. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReminderActionEntryPoint {
    fun confirmer(): PlannedPaymentConfirmer
    fun settingsRepository(): SettingsRepository
}

class ReminderActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val extras = intent.extras ?: return
        val event = ReminderEvent.fromMap(
            ReminderEventKeys.associateWith { extras.getString(it) }
        ) ?: return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ReminderNotifier.ACTION_SNOOZE -> {
                        SnoozedReminderWorker.schedule(context, event)
                        ReminderNotifier.cancel(context, event)
                    }
                    ReminderNotifier.ACTION_MARK_PAID -> markPaid(context, event)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun markPaid(context: Context, event: ReminderEvent) {
        val due = event.dueDate ?: return
        val ep = EntryPointAccessors.fromApplication(context, ReminderActionEntryPoint::class.java)
        val profileId = ep.settingsRepository().currentProfileId.first()
        val outcome = runCatching { ep.confirmer().confirm(event.targetId, due, profileId) }.getOrNull()
        // Whatever happened, a snoozed copy of this reminder has nothing left to say.
        SnoozedReminderWorker.cancel(context, event)
        when (outcome) {
            is ConfirmResult.Confirmed -> ReminderNotifier.postOutcome(context, event, "${outcome.paymentName} marked paid")
            is ConfirmResult.AlreadyHandled -> ReminderNotifier.postOutcome(context, event, "${outcome.paymentName} was already handled")
            is ConfirmResult.NeedsReview -> ReminderNotifier.postOutcome(context, event, "Open the app to confirm ${outcome.paymentName}")
            ConfirmResult.NotFound -> ReminderNotifier.cancel(context, event)
            null -> ReminderNotifier.postOutcome(context, event, "Couldn't mark it paid. Open the app to confirm.")
        }
    }

    private companion object {
        val ReminderEventKeys = listOf("key", "kind", "title", "body", "targetId", "dueDate")
    }
}
