package com.osscabinet.ossWallet.widget

import android.content.Context
import com.osscabinet.ossWallet.domain.service.WidgetRefreshTrigger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Triggers an immediate widget refresh by calling AppWidgetManager.updateAppWidget() directly
 * from a background coroutine. No WorkManager overhead — data-change refreshes are
 * sub-second end-to-end.
 */
class WorkManagerWidgetRefreshTrigger(private val context: Context) : WidgetRefreshTrigger {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun trigger() {
        scope.launch {
            WidgetUpdateCoordinator.updateAll(context)
        }
    }
}
