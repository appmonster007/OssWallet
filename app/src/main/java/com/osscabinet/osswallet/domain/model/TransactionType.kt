package com.osscabinet.ossWallet.domain.model

/**
 * Enum representing the type of a transaction.
 *
 * @property displayName Human-readable name for UI display
 * @property value String identifier used for persistence
 * @property iconName Name of the icon to use (maps to AppIconMap in UI layer)
 * @property colorHex Hex color value for the type
 */
enum class TransactionType(
    val displayName: String,
    val value: String,
    val iconName: String,
    val colorHex: String
) {
    EXPENSE(
        displayName = "Expense",
        value = "expense",
        iconName = "Remove",
        colorHex = "#B3261E"
    ),
    INCOME(
        displayName = "Income",
        value = "income",
        iconName = "Add",
        colorHex = "#004D40"
    ),
    TRANSFER(
        displayName = "Transfer",
        value = "transfer",
        iconName = "SwapHoriz",
        colorHex = "#2196F3"
    );

    companion object {
        /**
         * Get TransactionType from string value.
         * Defaults to EXPENSE if the value is invalid.
         */
        fun fromValue(value: String?): TransactionType {
            return entries.find { it.value.equals(value, ignoreCase = true) } ?: EXPENSE
        }

        /**
         * Get TransactionType from integer (used in some entities).
         * 0: Expense, 1: Income, 2: Transfer
         */
        fun fromInt(type: Int): TransactionType {
            return when (type) {
                1 -> INCOME
                2 -> TRANSFER
                else -> EXPENSE
            }
        }

        /**
         * Get TransactionType from boolean (for backward compatibility).
         * true = INCOME, false = EXPENSE
         */
        fun fromBoolean(isIncome: Boolean): TransactionType {
            return if (isIncome) INCOME else EXPENSE
        }

        /**
         * Special value for filters to indicate "all types".
         */
        const val ALL = "all"
    }

    /**
     * Convert to integer representation.
     * 0: Expense, 1: Income, 2: Transfer
     */
    fun toInt(): Int = when (this) {
        EXPENSE -> 0
        INCOME -> 1
        TRANSFER -> 2
    }

    /**
     * Convert to boolean (for backward compatibility).
     * INCOME = true, EXPENSE = false
     */
    fun toBoolean(): Boolean = this == INCOME
}
