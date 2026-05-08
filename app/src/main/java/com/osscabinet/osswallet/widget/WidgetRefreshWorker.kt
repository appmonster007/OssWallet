package com.osscabinet.ossWallet.widget

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.*
import java.util.concurrent.TimeUnit

class WidgetRefreshWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val manager = GlanceAppWidgetManager(applicationContext)

        val widgetClasses = listOf(
            NetWorthWidget(),
            CashFlowWidget(),
            AccountBalanceWidget(),
            UpcomingPaymentsWidget(),
            BudgetWidget()
        )

        for (widget in widgetClasses) {
            val ids = manager.getGlanceIds(widget::class.java)
            ids.forEach { id -> widget.update(applicationContext, id) }
        }

        return Result.success()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, "widget_update")
            .setContentTitle("Updating widgets")
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        return ForegroundInfo(1001, notification)
    }

    companion object {
        private const val WORK_NAME = "widget_refresh_worker"
        private const val IMMEDIATE_WORK_NAME = "widget_refresh_immediate"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(30, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().build())
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun scheduleImmediate(context: Context) {
            val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
