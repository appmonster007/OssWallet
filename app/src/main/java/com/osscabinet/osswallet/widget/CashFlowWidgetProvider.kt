package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll

class CashFlowWidgetProvider : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CashFlowGlanceWidget()

    companion object {
        suspend fun updateAllWidgets(context: Context) {
            CashFlowGlanceWidget().updateAll(context)
        }
    }
}
