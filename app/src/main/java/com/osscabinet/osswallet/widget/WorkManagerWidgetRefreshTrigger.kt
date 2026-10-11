package com.osscabinet.ossWallet.widget

import android.content.Context
import android.util.Log
import com.osscabinet.ossWallet.data.local.query.QueryCache
import com.osscabinet.ossWallet.domain.service.WidgetRefreshTrigger

/**
 * Bridges domain-level refresh triggers to [WidgetRefreshCoordinator].
 *
 * The [QueryCache] is cleared synchronously before the request is recorded so the worker always
 * reads from the database. TransactionManager calls [trigger] right after a write completes,
 * which races ahead of Room's asynchronous InvalidationTracker notification.
 *
 * Coalescing: requests only mark state, and one worker drains it, so a bulk import produces a
 * handful of passes at most, and a request during a pass never cancels it.
 *
 * Echo suppression: a write through [trigger] is followed by Room's invalidation for the same
 * commit via [onDatabaseChanged]. The pass for [trigger] already reads after that commit, so the
 * first invalidation within [ECHO_WINDOW_MS] is dropped. Invalidations with no preceding
 * [trigger] (imports, restores, the seeder) still refresh. Both methods are non-blocking and
 * lock-free apart from the short state lock, so any thread may call them.
 */
class WorkManagerWidgetRefreshTrigger(
    private val context: Context,
    private val queryCache: QueryCache,
    private val clock: () -> Long = System::currentTimeMillis
) : WidgetRefreshTrigger {

    @Volatile
    private var echoDeadline = 0L

    override fun trigger() {
        queryCache.clear()
        echoDeadline = clock() + ECHO_WINDOW_MS
        Log.d(TAG, "trigger() from app write")
        WidgetRefreshCoordinator.requestAll(context)
    }

    override fun onDatabaseChanged() {
        queryCache.clear()
        if (echoDeadline != 0L) {
            val expected = echoDeadline
            echoDeadline = 0L
            if (clock() <= expected) {
                Log.d(TAG, "onDatabaseChanged(): echo dropped")
                return
            }
        }
        Log.d(TAG, "onDatabaseChanged(): external change, refreshing")
        WidgetRefreshCoordinator.requestAll(context)
    }

    private companion object {
        const val ECHO_WINDOW_MS = 1_500L
        const val TAG = "WidgetRefreshTrigger"
    }
}
