package com.osscabinet.ossWallet.domain.util

/**
 * Cache manager for card computation results to avoid redundant calculations.
 * Thread-safe cache with expiration support.
 */
class CardDataCache(
    private val cacheExpirationMinutes: Long = 5L
) {
    private val cache = mutableMapOf<String, CachedCardData<*>>()

    /**
     * Get cached data if available and not expired, otherwise compute and cache.
     */
    suspend fun <T> getOrCompute(
        cacheKey: String,
        compute: suspend () -> T
    ): T {
        val cached = cache[cacheKey]
        @Suppress("UNCHECKED_CAST")
        if (cached != null && !cached.isExpired(cacheExpirationMinutes)) {
            return cached.data as T
        }

        val result = compute()
        cache[cacheKey] = CachedCardData(System.currentTimeMillis(), result)
        return result
    }

    /**
     * Invalidate cache entries for a specific card (by cardId prefix).
     */
    fun invalidateCard(cardId: String) {
        cache.keys.removeIf { it.startsWith(cardId) }
    }

    /**
     * Invalidate all cache entries.
     */
    fun invalidateAll() {
        cache.clear()
    }
}
