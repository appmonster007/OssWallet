package com.osscabinet.ossWallet.data.local.query

import java.util.concurrent.ConcurrentHashMap

/**
 * Simple in-memory cache for aggregation query results.
 */
class QueryCache {
    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private val cacheExpiryMs = 60 * 1000L // 1 minute default

    data class CacheEntry(
        val data: Any,
        val timestamp: Long
    )

    fun <T> get(key: String): T? {
        val entry = cache[key] ?: return null
        if (System.currentTimeMillis() - entry.timestamp > cacheExpiryMs) {
            cache.remove(key)
            return null
        }
        @Suppress("UNCHECKED_CAST")
        return entry.data as? T
    }

    fun put(key: String, data: Any) {
        cache[key] = CacheEntry(data, System.currentTimeMillis())
    }

    fun clear() {
        cache.clear()
    }
}
