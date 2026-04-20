package com.osscabinet.ossWallet.domain.formatter

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Pure domain function for currency formatting
 * No Android framework dependencies
 */
object CurrencyFormatter {
    /**
     * Formats a currency amount with the given currency code and precision
     * 
     * @param amount The amount to format
     * @param currency The currency code (e.g., "USD", "EUR")
     * @param precision The number of decimal places
     * @return Formatted string like "1,234.56 USD"
     */
    fun format(amount: BigDecimal, currency: String, precision: Int): String {
        val scaledAmount = amount.setScale(precision, RoundingMode.HALF_UP)
        return "$scaledAmount $currency"
    }
    
    /**
     * Formats a currency amount with right-aligned currency code
     * 
     * @param amount The amount to format
     * @param currency The currency code (e.g., "USD", "EUR")
     * @param precision The number of decimal places
     * @return Formatted string like "1,234.56 USD" (right-aligned for display)
     */
    fun formatRight(amount: BigDecimal, currency: String, precision: Int): String {
        val scaledAmount = amount.setScale(precision, RoundingMode.HALF_UP)
        return "$scaledAmount $currency"
    }
    
    /**
     * Formats a currency amount without currency code
     * Useful when currency is displayed separately
     * 
     * @param amount The amount to format
     * @param precision The number of decimal places
     * @return Formatted string like "1,234.56"
     */
    fun formatAmountOnly(amount: BigDecimal, precision: Int): String {
        return amount.setScale(precision, RoundingMode.HALF_UP).toPlainString()
    }
}
