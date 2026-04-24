package com.osscabinet.ossWallet.ui.util

import java.math.BigDecimal

/**
 * Utility functions for determining transaction type from value sign.
 * This is part of the calculator numpad migration where negative values
 * automatically toggle transaction type to expense.
 */

/**
 * Determines transaction type based on the sign of the amount.
 * 
 * @param amount The transaction amount
 * @return "income" if amount is positive or zero, "expense" if negative
 */
fun determineTransactionType(amount: BigDecimal): String {
    return if (amount.signum() >= 0) "income" else "expense"
}

/**
 * Determines transaction type from a string amount.
 * 
 * @param amountStr The transaction amount as a string
 * @return "income" if amount is positive or zero, "expense" if negative
 */
fun determineTransactionType(amountStr: String): String {
    val amount = amountStr.toBigDecimalOrNull() ?: BigDecimal.ZERO
    return determineTransactionType(amount)
}

/**
 * Checks if an amount represents an expense (negative value).
 * 
 * @param amount The transaction amount
 * @return true if negative, false otherwise
 */
fun isExpense(amount: BigDecimal): Boolean {
    return amount.signum() < 0
}

/**
 * Checks if an amount represents income (positive or zero value).
 * 
 * @param amount The transaction amount
 * @return true if positive or zero, false otherwise
 */
fun isIncome(amount: BigDecimal): Boolean {
    return amount.signum() >= 0
}

/**
 * Formats an amount for display with proper sign handling.
 * For expenses, this will show the amount as positive with a minus prefix
 * or in red color depending on the UI implementation.
 * 
 * @param amount The transaction amount
 * @return The formatted amount string
 */
fun formatAmountForDisplay(amount: BigDecimal): String {
    return amount.toPlainString()
}

/**
 * Gets the absolute value of an amount for calculations.
 * This is useful when the transaction type is already known
 * and you just need the magnitude.
 * 
 * @param amount The transaction amount (can be negative)
 * @return The absolute value
 */
fun getAbsoluteAmount(amount: BigDecimal): BigDecimal {
    return amount.abs()
}
