package com.osscabinet.ossWallet.domain.model

import kotlinx.serialization.Serializable

/**
 * Transaction Status Enum
 * 
 * Represents the clearance status of a transaction in the accounting workflow.
 * 
 * @property displayName Human-readable name for UI display
 */
@Serializable
enum class TransactionStatus(val displayName: String) {
    /**
     * Transaction needs to be reconciled with bank statement
     */
    RECONCILE("Reconcile"),
    
    /**
     * Transaction has been cleared and confirmed
     */
    CLEARED("Cleared"),
    
    /**
     * Transaction has not yet been cleared
     */
    UNCLEARED("Uncleared");

    companion object {
        /**
         * Convert string to TransactionStatus enum
         * Defaults to CLEARED if string doesn't match any status
         */
        fun fromString(value: String): TransactionStatus {
            return values().find { it.name.equals(value, ignoreCase = true) }
                ?: CLEARED
        }
    }
}
