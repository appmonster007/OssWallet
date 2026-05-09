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

/**
 * GlanceAppWidgetReceiver for the NetWorth widget.
 *
 * GlanceAppWidgetReceiver handles APPWIDGET_UPDATE, APPWIDGET_DELETED, and APPWIDGET_ENABLED
 * broadcasts automatically. On each APPWIDGET_UPDATE event Glance calls
 * [GlanceAppWidget.provideGlance] for each registered instance, which fetches fresh data and
 * re-renders.
 *
 * Data-change refresh path:
 *   TransactionManager -> WidgetRefreshTrigger -> WorkManagerWidgetRefreshTrigger
 *   -> WidgetRefreshWorker.enqueueNow() -> updateAppWidgetState() per GlanceId
 *   -> NetWorthGlanceWidget().updateAll() (triggers recompose of running session)
 *
 * When a widget is first placed on the homescreen the system sends APPWIDGET_UPDATE. Glance's
 * base class handles this and calls provideGlance() automatically — no manual work-enqueue is
 * needed in that path.
 *
 * Periodic system-initiated APPWIDGET_UPDATE broadcasts (every 30 min per the XML config) are
 * also forwarded to WorkManager so the refresh runs in a background worker with proper backoff
 * and retry, not on the main thread inside the broadcast receiver.
 *
 * Widget deletion cleans up per-instance DataStore entries to prevent unbounded growth.
 */
class NetWorthWidgetProvider : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = NetWorthGlanceWidget()

    // Scope for fire-and-forget cleanup tasks (onDeleted). SupervisorJob ensures one failing
    // delete does not cancel the others.
    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        // For periodic system APPWIDGET_UPDATE broadcasts, also route through WorkManager so
        // the refresh gets proper expedited scheduling and retry-on-failure semantics.
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            WidgetRefreshWorker.enqueueNow(context)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val prefsStore = NetWorthWidgetPrefsStore(context)
        // Non-critical cleanup: remove stored config for deleted widget instances.
        // Failure is safe — stale entries are ignored via getOrElse on next load.
        appWidgetIds.forEach { widgetId ->
            cleanupScope.launch {
                runCatching { prefsStore.delete(widgetId) }
            }
        }
    }
}
