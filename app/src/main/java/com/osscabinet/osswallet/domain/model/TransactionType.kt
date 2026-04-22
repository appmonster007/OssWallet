package com.osscabinet.ossWallet.domain.model

/**
 * Enum representing the type of a transaction.
 * Replaces the boolean isIncome field for better type safety and extensibility.
 */
enum class TransactionType(val displayName: String, val value: String) {
    INCOME("Income", "income"),
    EXPENSE("Expense", "expense");

    companion object {
        /**
         * Get TransactionType from string value.
         * Returns null if the value is invalid.
         */
        fun fromValue(value: String?): TransactionType? {
            return values().find { it.value == value }
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
         * This is not a valid transaction type, only used for filtering.
         */
        const val ALL = "all"
    }

    /**
     * Convert to boolean (for backward compatibility).
     * INCOME = true, EXPENSE = false
     */
    fun toBoolean(): Boolean = this == INCOME
}
