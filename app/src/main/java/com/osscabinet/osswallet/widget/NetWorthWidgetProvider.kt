package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class NetWorthWidgetProvider : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NetWorthGlanceWidget()

    companion object {
        suspend fun updateAllWidgets(context: Context) {
            WidgetUpdateCoordinator.updateAll(context)
        }
    }
}
