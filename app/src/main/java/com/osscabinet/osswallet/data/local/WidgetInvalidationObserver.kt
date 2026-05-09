package com.osscabinet.ossWallet.data.local

import androidx.room.InvalidationTracker
import com.osscabinet.ossWallet.data.local.query.QueryCache
import com.osscabinet.ossWallet.domain.service.WidgetRefreshTrigger

/**
 * Watches the transactions table via Room's InvalidationTracker.
 * On any write it clears the query cache first, then triggers the widget update.
 * Clearing the cache here guarantees that when provideGlance() runs aggregate(),
 * it always hits the database rather than returning the previous result.
 */
class WidgetInvalidationObserver(
    private val db: AppDatabase,
    private val queryCache: QueryCache,
    private val trigger: WidgetRefreshTrigger
) {
    private val observer = object : InvalidationTracker.Observer("transactions") {
        override fun onInvalidated(tables: Set<String>) {
            queryCache.clear()
            trigger.trigger()
        }
    }

    fun start() {
        db.invalidationTracker.addObserver(observer)
    }

    fun stop() {
        db.invalidationTracker.removeObserver(observer)
    }
}
