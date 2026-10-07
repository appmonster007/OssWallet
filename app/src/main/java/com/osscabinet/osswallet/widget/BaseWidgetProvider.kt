package com.osscabinet.ossWallet.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Shared shape for the three plain (non-Glance) homescreen widget providers — raw RemoteViews,
 * built by each widget's own renderer.
 *
 * Refresh path: TransactionManager -> WidgetRefreshTrigger -> WorkManagerWidgetRefreshTrigger
 *   -> WidgetRefreshWorker.enqueueNow() -> that widget's fetcher updates an in-memory Preferences
 *   -> that widget's RemoteViews renderer -> AppWidgetManager.updateAppWidget().
 *
 * [onUpdate] and [onAppWidgetOptionsChanged] (fired on placement, periodic system updates, and
 * resize) both just enqueue the worker rather than rendering inline — broadcast receivers have a
 * tight execution-time budget and the fetch path hits the database.
 */
abstract class BaseWidgetProvider(
    private val createPrefsStore: (Context) -> WidgetPrefsStore<*>
) : AppWidgetProvider() {

    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        WidgetRefreshWorker.enqueueNow(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        WidgetRefreshWorker.enqueueNow(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val prefsStore = createPrefsStore(context)
        appWidgetIds.forEach { widgetId ->
            cleanupScope.launch {
                runCatching { prefsStore.delete(widgetId) }
            }
        }
    }
}
