package com.osscabinet.ossWallet.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Generic background worker that refreshes all registered widgets.
 * Uses WidgetRegistry to find fetchers for each widget type.
 */
class WidgetRefreshWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            withContext(NonCancellable) {
                val context = applicationContext
                val widgetManager = GlanceAppWidgetManager(context)
                val common = withContext(Dispatchers.IO) {
                    WidgetDataLoader.loadCommon(context)
                }

                for (widgetClass in WidgetRegistry.getAllRegisteredWidgets()) {
                    val fetcher = WidgetRegistry.getFetcher(widgetClass) ?: continue
                    @Suppress("UNCHECKED_CAST")
                    val glanceIds = widgetManager.getGlanceIds(widgetClass as Class<out GlanceAppWidget>)
                    
                    if (glanceIds.isEmpty()) continue

                    withContext(Dispatchers.IO) {
                        for (glanceId in glanceIds) {
                            val widgetId = runCatching {
                                widgetManager.getAppWidgetId(glanceId)
                            }.getOrNull() ?: continue

                            updateAppWidgetState(
                                context,
                                PreferencesGlanceStateDefinition,
                                glanceId
                            ) { currentPrefs ->
                                val mutablePrefs = currentPrefs.toMutablePreferences()
                                fetcher.updateState(context, widgetId, common, mutablePrefs)
                                mutablePrefs
                            }
                        }
                    }
                    // Trigger recomposition
                    (widgetClass.newInstance() as GlanceAppWidget).updateAll(context)
                }
            }
            Result.success()
        } catch (t: Throwable) {
            Log.e(TAG, "Widget refresh failed", t)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "WidgetRefreshWorker"
        const val WORK_NAME = "net_worth_widget_refresh"

        fun enqueueNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
