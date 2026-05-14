package com.osscabinet.ossWallet.domain.model

import kotlinx.serialization.Serializable

/**
 * Transaction Status Enum
 *
 * Represents the clearance status of a transaction in the accounting workflow.
 *
 * @property displayName Human-readable name for UI display
 * @property iconName Name of the icon to use (maps to AppIconMap in UI layer)
 * @property colorHex Hex color value for the status (e.g., "#4CAF50")
 * @property description Description of the status for UI tooltips
 */
@Serializable
enum class TransactionStatus(
    val displayName: String,
    val iconName: String,
    val colorHex: String,
    val description: String
) {
    /**
     * Transaction has been cleared and confirmed
     */
    CLEARED(
        displayName = "Cleared",
        iconName = "CheckCircle",
        colorHex = "#4CAF50",
        description = "Transaction has been confirmed"
    ),

    /**
     * Transaction needs to be reconciled with bank statement
     */
    RECONCILE(
        displayName = "Reconcile",
        iconName = "Refresh",
        colorHex = "#2196F3",
        description = "Needs to be reconciled with bank statement"
    ),

    /**
     * Transaction has not yet been cleared
     */
    UNCLEARED(
        displayName = "Uncleared",
        iconName = "RadioButtonUnchecked",
        colorHex = "#FF9800",
        description = "Awaiting confirmation"
    );

    companion object {
        /**
         * Convert string to TransactionStatus enum
         * Defaults to CLEARED if string doesn't match any status
         */
        fun fromString(value: String): TransactionStatus {
            return entries.find { it.name.equals(value, ignoreCase = true) }
                ?: CLEARED
        }
    }
}
