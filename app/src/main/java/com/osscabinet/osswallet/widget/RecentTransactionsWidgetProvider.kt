package com.osscabinet.ossWallet.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RecentTransactionsWidgetProvider : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = RecentTransactionsGlanceWidget()

    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            WidgetRefreshWorker.enqueueNow(context)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val prefsStore = RecentTransactionsWidgetPrefsStore(context)
        appWidgetIds.forEach { widgetId ->
            cleanupScope.launch {
                runCatching { prefsStore.delete(widgetId) }
            }
        }
    }
}
