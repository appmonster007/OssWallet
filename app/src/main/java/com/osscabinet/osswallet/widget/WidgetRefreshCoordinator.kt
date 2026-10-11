package com.osscabinet.ossWallet.widget

import android.content.Context
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import java.util.concurrent.atomic.AtomicInteger

/**
 * Single entry point for every widget refresh request: data writes, settings changes, resize,
 * config save and provider onUpdate. Requests only record what is dirty in [WidgetRefreshState];
 * one [WidgetRefreshWorker] drains it. A request that arrives while a worker is running never
 * cancels it, it just marks more work for the worker's next pass.
 *
 * Work is enqueued with KEEP on one of two alternating names. The worker-claim flag in
 * [WidgetRefreshState] already guarantees at most one outstanding worker, and alternating names
 * keeps a request that lands just after a worker retires from being dropped by KEEP while
 * WorkManager still reports the old unique work as running.
 */
object WidgetRefreshCoordinator {
    private const val TAG = "WidgetRefresh"
    private const val WORK_NAME_PREFIX = "widget_refresh_"

    internal val state = WidgetRefreshState()
    private val slot = AtomicInteger()

    /** Data or settings changed: every widget needs a fresh read. */
    fun requestAll(context: Context) {
        if (state.requestAll()) enqueue(context)
    }

    /** Only these widgets need a render (resize, config save, onUpdate). */
    fun requestWidgets(context: Context, appWidgetIds: Collection<Int>) {
        if (state.requestWidgets(appWidgetIds)) enqueue(context)
    }

    fun requestWidgets(context: Context, appWidgetIds: IntArray) =
        requestWidgets(context, appWidgetIds.toList())

    private fun enqueue(context: Context) {
        try {
            val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            val name = WORK_NAME_PREFIX + (slot.getAndIncrement() and 1)
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniqueWork(name, ExistingWorkPolicy.KEEP, request)
        } catch (t: Throwable) {
            // Nothing will drain the request, so release the claim or later requests never enqueue.
            Log.e(TAG, "Failed to enqueue widget refresh", t)
            state.abort(null)
        }
    }
}
