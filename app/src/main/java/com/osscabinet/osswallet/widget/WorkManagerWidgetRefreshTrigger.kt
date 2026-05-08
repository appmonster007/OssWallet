package com.osscabinet.ossWallet.widget

import android.content.Context
import com.osscabinet.ossWallet.domain.service.WidgetRefreshTrigger

/**
 * Implementation of WidgetRefreshTrigger that schedules an immediate
 * one-time widget refresh via WorkManager.
 */
class WorkManagerWidgetRefreshTrigger(private val context: Context) : WidgetRefreshTrigger {
    override fun trigger() {
        WidgetRefreshWorker.scheduleImmediate(context)
    }
}
