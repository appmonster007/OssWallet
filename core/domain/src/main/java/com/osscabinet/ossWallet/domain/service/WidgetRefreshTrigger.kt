package com.osscabinet.ossWallet.domain.service

/**
 * Domain interface for triggering widget refreshes.
 * Implemented in the widget/data layer to avoid Android imports in domain.
 */
interface WidgetRefreshTrigger {
    /** A data or settings change the caller knows about (e.g. TransactionManager after a write). */
    fun trigger()

    /**
     * The database reported a committed change (Room InvalidationTracker). For writes that went
     * through [trigger] this is an echo of the same change, so implementations may drop it.
     */
    fun onDatabaseChanged() = trigger()
}
