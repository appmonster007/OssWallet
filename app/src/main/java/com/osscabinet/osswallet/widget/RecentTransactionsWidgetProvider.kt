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
 * Plain (non-Glance) AppWidgetProvider for the Recent Transactions widget — raw RemoteViews,
 * built by [RecentTransactionsRemoteViewsRenderer]. See CashFlowWidgetProvider for the
 * refresh-path writeup; this follows the exact same shape.
 */
class RecentTransactionsWidgetProvider : AppWidgetProvider() {

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
        val prefsStore = RecentTransactionsWidgetPrefsStore(context)
        appWidgetIds.forEach { widgetId ->
            cleanupScope.launch {
                runCatching { prefsStore.delete(widgetId) }
            }
        }
    }
}
