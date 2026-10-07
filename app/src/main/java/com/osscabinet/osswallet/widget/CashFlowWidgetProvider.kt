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
 * Plain (non-Glance) AppWidgetProvider for the Cash Flow widget — raw RemoteViews, built by
 * [CashFlowRemoteViewsRenderer].
 *
 * Refresh path (same shape as the Glance widgets, see NetWorthWidgetProvider):
 *   TransactionManager -> WidgetRefreshTrigger -> WorkManagerWidgetRefreshTrigger
 *   -> WidgetRefreshWorker.enqueueNow() -> CashFlowWidgetFetcher.updateState() into an in-memory
 *   Preferences -> CashFlowRemoteViewsRenderer.render() -> AppWidgetManager.updateAppWidget()
 *
 * [onUpdate] and [onAppWidgetOptionsChanged] (fired on placement, periodic system updates, and
 * resize) both just enqueue the worker rather than rendering inline — broadcast receivers have a
 * tight execution-time budget and the fetch path hits the database.
 */
class CashFlowWidgetProvider : AppWidgetProvider() {

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
        val prefsStore = CashFlowWidgetPrefsStore(context)
        appWidgetIds.forEach { widgetId ->
            cleanupScope.launch {
                runCatching { prefsStore.delete(widgetId) }
            }
        }
    }
}
