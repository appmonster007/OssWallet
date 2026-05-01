package com.osscabinet.ossWallet.widget

import android.content.Context
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

    companion object {
        private const val WORK_NAME = "widget_refresh_worker"

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
    }
}
