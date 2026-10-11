package com.osscabinet.ossWallet.data.local

import androidx.room.InvalidationTracker
import com.osscabinet.ossWallet.data.local.query.QueryCache
import com.osscabinet.ossWallet.domain.service.WidgetRefreshTrigger

/**
 * Watches every table the widget fetchers read via Room's InvalidationTracker.
 *
 * This is the fallback invalidation path — it fires for any database write, including writes
 * from the seeder, backup restore, or any DAO call that bypasses TransactionManager.
 *
 * For writes through TransactionManager the cache is already cleared synchronously inside
 * WorkManagerWidgetRefreshTrigger.trigger() before the widget update coroutine is launched,
 * so the queryCache.clear() here is a redundant-but-harmless safety net in that path.
 *
 * Note: Room 2.7.0 still supports the InvalidationTracker.Observer API (the new Flow-based
 * API is additive). The observer fires on Room's background invalidation thread after the
 * write transaction is committed, so onInvalidated() is always safe to call cache.clear().
 */
class WidgetInvalidationObserver(
    private val db: AppDatabase,
    private val queryCache: QueryCache,
    private val trigger: WidgetRefreshTrigger
) {
    private val observer = object : InvalidationTracker.Observer(
        "transactions", "accounts", "categories", "currency_rates", "profiles", "transaction_links", "budgets"
    ) {
        override fun onInvalidated(tables: Set<String>) {
            // Clear the cache before calling trigger() so that trigger()'s synchronous
            // cache.clear() and this clear() are both no-ops if they race — ConcurrentHashMap
            // makes both operations thread-safe.
            queryCache.clear()
            trigger.onDatabaseChanged()
        }
    }

    fun start() {
        db.invalidationTracker.addObserver(observer)
    }

    fun stop() {
        db.invalidationTracker.removeObserver(observer)
    }
}
