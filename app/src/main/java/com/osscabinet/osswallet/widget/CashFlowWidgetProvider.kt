package com.osscabinet.ossWallet.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * GlanceAppWidgetReceiver for the Cash Flow widget.
 *
 * Refresh path (same as NetWorthWidgetProvider):
 *   TransactionManager -> WidgetRefreshTrigger -> WorkManagerWidgetRefreshTrigger
 *   -> WidgetRefreshWorker.enqueueNow() -> updateAppWidgetState() per GlanceId
 *   -> CashFlowGlanceWidget().updateAll()
 *
 * Periodic system APPWIDGET_UPDATE broadcasts (configured in cash_flow_widget_info.xml)
 * are forwarded to WorkManager so refresh runs off the main thread with retry semantics.
 *
 * No config activity — the widget uses a fixed default period (current month).
 */
class CashFlowWidgetProvider : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = CashFlowGlanceWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        // Forward periodic system update broadcasts through WorkManager so we get
        // expedited scheduling and proper retry-on-failure, not a main-thread refresh.
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            WidgetRefreshWorker.enqueueNow(context)
        }
    }
}
