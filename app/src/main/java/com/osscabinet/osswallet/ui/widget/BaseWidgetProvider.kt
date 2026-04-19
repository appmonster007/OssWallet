package com.osscabinet.ossWallet.ui.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Base class for high-performance widgets.
 */
abstract class BaseWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        Log.d("BaseWidgetProvider", "onUpdate called for ${this.javaClass.simpleName}")
        WidgetUpdateManager.requestUpdate(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            Log.d("BaseWidgetProvider", "Manual refresh requested for ${this.javaClass.simpleName}")
            WidgetUpdateManager.requestUpdate(context)
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.osscabinet.ossWallet.action.WIDGET_REFRESH"
    }
}
