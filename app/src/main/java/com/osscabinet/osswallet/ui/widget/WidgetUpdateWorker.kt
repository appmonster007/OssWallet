package com.osscabinet.ossWallet.ui.widget

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class WidgetUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Widget update worker started (Type: ${if (isPeriodic()) "Periodic" else "Manual"})")
        try {
            WidgetUpdateHelper.performUpdate(applicationContext)
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Worker failed", e)
            return if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private fun isPeriodic(): Boolean {
        return tags.contains("WidgetPeriodicUpdate")
    }

    companion object {
        private const val TAG = "WidgetUpdateWorker"
        private const val WORK_NAME = "WidgetPeriodicUpdate"

        fun enqueuePeriodicWork(context: Context) {
            val request = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
                15, TimeUnit.MINUTES // Minimum interval allowed by WorkManager
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
