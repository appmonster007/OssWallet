package com.osscabinet.ossWallet.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.osscabinet.ossWallet.di.PlannedPaymentWorkerEntryPoint
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.dto.TransactionRequest
import com.osscabinet.ossWallet.domain.service.management.TransactionEntryService
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The one PendingIntent target in this phase that mutates data in place (per the Planned
 * Payments target data model in docs/card-data-summary.md — "the clearest in-place-mutation
 * target"). Confirm/Skip taps on a Planned Payments widget row land here instead of opening the
 * app, call the SAME [com.osscabinet.ossWallet.domain.service.management.TransactionManager]
 * entry points the Compose dashboard's quick-confirm/quick-skip swipe actions use
 * ([com.osscabinet.ossWallet.ui.screens.dashboard.DashboardViewModel.quickConfirmById] /
 * `.quickSkipById`), then re-enqueue this widget's refresh.
 *
 * Registered `android:exported="false"` — only this app's own PendingIntents can invoke it.
 */
class PlannedPaymentsWidgetActionReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val paymentId = intent.getLongExtra(EXTRA_PAYMENT_ID, -1L)
        val scheduledDate = intent.getLongExtra(EXTRA_SCHEDULED_DATE, -1L)
        val action = intent.action
        if (paymentId <= 0L || scheduledDate <= 0L || action == null) return

        val appContext = context.applicationContext
        // onReceive must return quickly, but the actual mutation needs DB/coroutine work —
        // same tradeoff BaseWidgetProvider.onDeleted already makes with its own cleanupScope.
        scope.launch {
            runCatching {
                when (action) {
                    ACTION_CONFIRM -> confirm(appContext, paymentId, scheduledDate)
                    ACTION_SKIP -> skip(appContext, paymentId, scheduledDate)
                }
            }.onFailure { t ->
                Log.e(TAG, "Planned payment widget action failed", t)
            }
            WidgetRefreshWorker.enqueueNow(appContext)
        }
    }

    private suspend fun confirm(context: Context, paymentId: Long, scheduledDate: Long) {
        val ep = entryPoint(context)
        val payment = ep.plannedPaymentRepository().getById(paymentId) ?: return
        val accountId = payment.accountId?.takeIf { it.isNotBlank() } ?: return
        val profileId = ep.settingsRepository().currentProfileId.first()

        val request = TransactionRequest(
            name = payment.name,
            amount = TransactionEntryService.getMagnitude(payment.amount),
            entryMode = payment.entryMode,
            accountId = accountId,
            currency = payment.currency,
            categoryId = payment.categoryId,
            tags = payment.tags,
            paymentMethod = payment.paymentMethod ?: PaymentMethod.CASH,
            timestamp = System.currentTimeMillis(),
            payee = payment.payee ?: "",
            note = payment.note ?: "",
            targetAccountId = payment.targetAccountId,
            targetCurrency = payment.targetCurrency,
            transferRate = payment.transferRate,
            sourcePlannedPaymentId = payment.id,
            warrantyExpiration = payment.warrantyExpiration,
            gpsLocation = payment.gpsLocation
        )
        ep.transactionManager().handleSaveTransaction(
            existingTransaction = null,
            request = request,
            profileId = profileId,
            sourcePlannedPaymentId = payment.id,
            scheduledDate = scheduledDate
        )
    }

    private suspend fun skip(context: Context, paymentId: Long, scheduledDate: Long) {
        val ep = entryPoint(context)
        ep.transactionManager().skipPlannedPayment(paymentId = paymentId, scheduledDate = scheduledDate)
    }

    private fun entryPoint(context: Context): PlannedPaymentWorkerEntryPoint =
        EntryPointAccessors.fromApplication(context, PlannedPaymentWorkerEntryPoint::class.java)

    companion object {
        private const val TAG = "PlannedPaymentsWidgetAction"
        const val ACTION_CONFIRM = "com.osscabinet.ossWallet.widget.action.CONFIRM_PLANNED_PAYMENT"
        const val ACTION_SKIP = "com.osscabinet.ossWallet.widget.action.SKIP_PLANNED_PAYMENT"
        const val EXTRA_PAYMENT_ID = "extra_payment_id"
        const val EXTRA_SCHEDULED_DATE = "extra_scheduled_date"
    }
}
