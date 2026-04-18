package com.osscabinet.ossWallet.ui.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager

import androidx.glance.appwidget.updateAll

object WidgetUpdateHelper {
    private const val TAG = "WidgetUpdateHelper"

    suspend fun updateAllWidgets(context: Context) {
        Log.d(TAG, "Updating all widgets using updateAll()...")
        
        try {
            NetWorthWidget().updateAll(context)
            BalanceWidget().updateAll(context)
            TransactionHistoryWidget().updateAll(context)
            BudgetWidget().updateAll(context)
            CashFlowWidget().updateAll(context)
            Log.d(TAG, "All updateAll() calls triggered.")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating widgets with updateAll()", e)
            
            // Fallback to manual update if updateAll fails for some reason
            val manager = GlanceAppWidgetManager(context)
            updateManual(context, manager)
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
