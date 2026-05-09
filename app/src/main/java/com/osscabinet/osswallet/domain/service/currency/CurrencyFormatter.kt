package com.osscabinet.ossWallet.domain.service.currency

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Currency
import java.util.Locale

/**
 * Domain service for currency and number formatting.
 * Handles localized grouping, decimal separators, and smart shortening.
 */
object CurrencyFormatter {

    /**
     * Formats an amount with localized grouping and decimal separators.
     */
    fun formatAmount(amount: BigDecimal?, precision: Int = 2): String {
        val validAmount = amount ?: BigDecimal.ZERO
        val symbols = DecimalFormatSymbols(Locale.getDefault())
        val pattern = buildString {
            append("#,##0")
            if (precision > 0) {
                append(".")
                repeat(precision) { append("0") }
            }
        }
        val format = DecimalFormat(pattern, symbols)
        return format.format(validAmount)
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
     * Shortens large numbers for UI display (e.g., 1.234M, 10.50K).
     * Keeps at least 4 digits total and respects requested precision.
     */
    fun formatShortNumber(value: BigDecimal?, precision: Int = 2): String {
        val validValue = value ?: BigDecimal.ZERO
        val absValue = validValue.abs()
        val (divisor, suffix) = when {
            absValue >= BigDecimal("1000000000000") -> BigDecimal("1000000000000") to "T"
            absValue >= BigDecimal("1000000000") -> BigDecimal("1000000000") to "B"
            absValue >= BigDecimal("1000000") -> BigDecimal("1000000") to "M"
            absValue >= BigDecimal("10000") -> BigDecimal("1000") to "K"
            else -> return formatAmount(validValue, precision)
        }

        val shortened = validValue.divide(divisor, 8, RoundingMode.HALF_UP)
        val integerPart = shortened.toBigInteger().abs().toString()
        val integerDigits = if (integerPart == "0") 0 else integerPart.length
        
        // Keep at least 4 digits total (integer + decimal)
        val adaptiveDecimals = (4 - integerDigits).coerceAtLeast(1)
        val finalScale = maxOf(adaptiveDecimals, precision)

        val result = shortened.setScale(finalScale, RoundingMode.HALF_UP)
        
        val symbols = DecimalFormatSymbols(Locale.getDefault())
        val pattern = buildString {
            append("0")
            if (finalScale > 0) {
                append(".")
                repeat(finalScale) { append("0") }
            }
        }
        val format = DecimalFormat(pattern, symbols)
        return "${format.format(result)}$suffix"
    }

    /**
     * Formats a currency amount in a shortened form (e.g., "$1.234M", "10.50K €").
     */
    fun formatShortCurrency(amount: BigDecimal?, currency: String?, precision: Int = 2, symbolOnLeft: Boolean = true): String {
        val shortNumber = formatShortNumber(amount, precision)
        val symbol = getCurrencySymbol(currency)
        return if (symbolOnLeft) "$symbol$shortNumber" else "$shortNumber $symbol"
    }

    /**
     * Main UI entry point for formatting currency with symbol on the right.
     */
    fun formatCurrencyRight(
        amount: BigDecimal?,
        currencyCode: String?,
        precision: Int = 2,
        shorten: Boolean = false
    ): String {
        return if (shorten) {
            formatShortCurrency(amount, currencyCode, precision, symbolOnLeft = false)
        } else {
            val formattedAmount = formatAmount(amount, precision)
            val symbol = getCurrencySymbol(currencyCode)
            "$formattedAmount $symbol"
        }
    }

    /**
     * Main UI entry point for formatting currency with symbol on the left.
     */
    fun formatCurrencyLeft(
        amount: BigDecimal?,
        currencyCode: String?,
        precision: Int = 2,
        shorten: Boolean = false
    ): String {
        return if (shorten) {
            formatShortCurrency(amount, currencyCode, precision, symbolOnLeft = true)
        } else {
            val formattedAmount = formatAmount(amount, precision)
            val symbol = getCurrencySymbol(currencyCode)
            "$symbol$formattedAmount"
        }
    }
}
