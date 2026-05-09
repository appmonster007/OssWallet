package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

object WidgetUpdateCoordinator {
    suspend fun updateAll(context: Context) = coroutineScope {
        launch { NetWorthGlanceWidget().updateAll(context) }
        launch { CashFlowGlanceWidget().updateAll(context) }
    }
}
