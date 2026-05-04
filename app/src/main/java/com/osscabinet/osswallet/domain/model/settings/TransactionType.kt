package com.osscabinet.ossWallet.domain.model.settings

import kotlinx.serialization.Serializable

/**
 * Transaction type filtering options for category-based cards.
 * Restricts analysis to either income or expense transactions only.
 */
@Serializable
enum class TransactionType {
    /**
     * Show only expense transactions (negative amounts)
     */
    EXPENSE_ONLY,
    
    /**
     * Show only income transactions (positive amounts)
     */
    INCOME_ONLY
}
