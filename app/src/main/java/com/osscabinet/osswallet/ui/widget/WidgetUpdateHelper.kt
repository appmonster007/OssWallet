package com.osscabinet.ossWallet.ui.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager

import androidx.glance.appwidget.updateAll

object WidgetUpdateHelper {
    private const val TAG = "WidgetUpdateHelper"

    suspend fun updateAllWidgets(context: Context) {
        Log.d(TAG, "Updating all widgets using updateAll()...")
        
        val widgets = listOf(
            NetWorthWidget(),
            BalanceWidget(),
            TransactionHistoryWidget(),
            BudgetWidget(),
            CashFlowWidget()
        )

        widgets.forEach { widget ->
            try {
                // Pre-check for GlanceId to avoid unnecessary SessionWorker overhead if widget not on home screen
                val manager = GlanceAppWidgetManager(context)
                val ids = manager.getGlanceIds(widget::class.java)
                if (ids.isNotEmpty()) {
                    widget.updateAll(context)
                    Log.d(TAG, "${widget::class.java.simpleName} update triggered for ${ids.size} instances.")
                } else {
                    Log.d(TAG, "Skipping ${widget::class.java.simpleName}: No instances found.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating ${widget::class.java.simpleName}", e)
            }
        }
    }

    private suspend fun updateManual(context: Context, manager: GlanceAppWidgetManager) {
        // ... previous manual logic as fallback ...
        val widgets = listOf(
            NetWorthWidget::class.java,
            BalanceWidget::class.java,
            TransactionHistoryWidget::class.java,
            BudgetWidget::class.java,
            CashFlowWidget::class.java
        )

        widgets.forEach { widgetClass ->
            val ids = manager.getGlanceIds(widgetClass)
            Log.d(TAG, "Manual Fallback: Found ${ids.size} for ${widgetClass.simpleName}")
            ids.forEach { id ->
                widgetClass.getDeclaredConstructor().newInstance().update(context, id)
            }
        }
    }
}
