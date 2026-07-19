package com.osscabinet.ossWallet.widget

import android.content.Context
import com.osscabinet.ossWallet.data.local.query.QueryCache
import com.osscabinet.ossWallet.domain.service.WidgetRefreshTrigger

/**
 * Triggers a widget refresh by enqueueing a one-shot expedited [WidgetRefreshWorker] via
 * WorkManager using [ExistingWorkPolicy.REPLACE].
 *
 * The [QueryCache] is invalidated synchronously before the worker is enqueued so that when
 * [provideGlance] executes inside the worker it always reads from the database rather than a
 * stale cached result. This matters because TransactionManager calls [trigger] immediately after
 * a write completes at the Kotlin-suspend level, which races ahead of Room's asynchronous
 * InvalidationTracker notification path.
 *
 * Coalescing guarantee: if [trigger] is called N times in rapid succession (e.g. a bulk import),
 * WorkManager deduplicate them to a single queued entry because they all share the unique work
 * name [WidgetRefreshWorker.WORK_NAME] and use [ExistingWorkPolicy.REPLACE]. Only one refresh
 * runs for the entire batch.
 *
 * Deadlock guarantee: [trigger] is a plain (non-suspend) function that only writes to a
 * ConcurrentHashMap and calls WorkManager.enqueueUniqueWork() — both are non-blocking. No lock
 * is acquired, so there is no risk of deadlock regardless of which thread calls [trigger].
 */
class WorkManagerWidgetRefreshTrigger(
    private val context: Context,
    private val queryCache: QueryCache
) : WidgetRefreshTrigger {

    override fun trigger() {
        // Synchronously clear the cache before enqueuing so the worker always reads fresh data.
        queryCache.clear()
        WidgetRefreshWorker.enqueueNow(context)
    }
}
