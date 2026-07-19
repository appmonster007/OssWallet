package com.osscabinet.ossWallet.domain.util

/**
 * Generic wrapper for cached data with expiration logic.
 */
data class CachedCardData<T>(
    val timestamp: Long,
    val data: T
) {
    /**
     * Checks if the cached data is older than the specified minutes.
     */
    fun isExpired(minutes: Long): Boolean {
        return System.currentTimeMillis() - timestamp > minutes * 60 * 1000
    }
}
