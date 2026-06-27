package com.osscabinet.ossWallet.domain.service

/**
 * Domain interface for triggering widget refreshes.
 * Implemented in the widget/data layer to avoid Android imports in domain.
 */
interface WidgetRefreshTrigger {
    fun trigger()
}
