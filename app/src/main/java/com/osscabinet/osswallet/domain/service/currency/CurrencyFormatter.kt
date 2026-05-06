package com.osscabinet.ossWallet.domain.service.currency

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Currency
import java.util.Locale

/**
 * Domain service for currency and number formatting.
 * Separated from conversion logic to maintain SRP.
 */
object CurrencyFormatter {

    /**
     * Formats an amount with localized grouping and decimal separators.
     */
    fun formatAmount(amount: BigDecimal, precision: Int = 2): String {
        val symbols = DecimalFormatSymbols(Locale.getDefault())
        val pattern = buildString {
            append("#,##0")
            if (precision > 0) {
                append(".")
                repeat(precision) { append("0") }
            }
        }
        val format = DecimalFormat(pattern, symbols)
        return format.format(amount)
    }

    /**
     * Returns the symbol for a given currency code.
     */
    fun getCurrencySymbol(currencyCode: String?): String {
        if (currencyCode.isNullOrBlank()) return ""
        return try {
            val code = currencyCode.uppercase().trim()
            if (code.length == 3 && code.all { it.isLetter() }) {
                Currency.getInstance(code).symbol
            } else {
                currencyCode
            }
        } catch (e: Exception) {
            currencyCode
        }
    }

    /**
     * Returns the full name for a given currency code.
     */
    fun getCurrencyName(currencyCode: String?): String {
        if (currencyCode.isNullOrBlank()) return ""
        return try {
            val code = currencyCode.uppercase().trim()
            if (code.length == 3 && code.all { it.isLetter() }) {
                Currency.getInstance(code).getDisplayName(Locale.getDefault())
            } else {
                currencyCode
            }
        } catch (e: Exception) {
            currencyCode
        }
    }

    /**
     * Shortens large numbers for UI display (e.g., 1.2M, 500K).
     */
    fun formatShortNumber(value: BigDecimal): String {
        val absValue = value.abs()
        return when {
            absValue >= BigDecimal("1000000000") -> {
                val short = value.divide(BigDecimal("1000000000"), 1, RoundingMode.HALF_UP)
                "${short.stripTrailingZeros().toPlainString()}B"
            }
            absValue >= BigDecimal("1000000") -> {
                val short = value.divide(BigDecimal("1000000"), 1, RoundingMode.HALF_UP)
                "${short.stripTrailingZeros().toPlainString()}M"
            }
            absValue >= BigDecimal("1000") -> {
                val short = value.divide(BigDecimal("1000"), 1, RoundingMode.HALF_UP)
                "${short.stripTrailingZeros().toPlainString()}K"
            }
            else -> value.setScale(0, RoundingMode.HALF_UP).toString()
        }
    }

    /**
     * Formats a currency amount with the given currency code and precision.
     */
    fun format(amount: BigDecimal, currency: String, precision: Int): String {
        val formattedAmount = formatAmount(amount, precision)
        return "$formattedAmount $currency"
    }

    /**
     * Formats with symbol on the right (e.g., "1,000 $")
     */
    fun formatRight(amount: BigDecimal, currency: String, precision: Int): String {
        val formattedAmount = formatAmount(amount, precision)
        val symbol = getCurrencySymbol(currency)
        return "$formattedAmount $symbol"
    }

    /**
     * Formats with symbol on the left (e.g., "$1,000")
     */
    fun formatLeft(amount: BigDecimal, currency: String, precision: Int): String {
        val formattedAmount = formatAmount(amount, precision)
        val symbol = getCurrencySymbol(currency)
        return "$symbol$formattedAmount"
    }
}
