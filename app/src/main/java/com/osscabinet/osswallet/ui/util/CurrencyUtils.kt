package com.osscabinet.ossWallet.ui.util

import com.osscabinet.ossWallet.domain.service.CurrencyService
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

// UI-specific formatting functions (can remain in UI layer)
fun formatCurrencyRight(amount: BigDecimal?, currencyCode: String?, precision: Int = 2): String {
    val validAmount = amount ?: BigDecimal.ZERO
    val symbol = getCurrencySymbol(currencyCode)
    val formatted = formatAmount(validAmount, precision)
    return "$formatted $symbol"
}

fun formatCurrencyLeft(amount: BigDecimal?, currencyCode: String?, precision: Int = 2): String {
    val validAmount = amount ?: BigDecimal.ZERO
    val symbol = getCurrencySymbol(currencyCode)
    val formatted = formatAmount(validAmount, precision)
    return "$symbol$formatted"
}

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
 * Shortens large numbers for UI display (e.g., 1000 -> 1K, 1000000 -> 1M)
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

fun getCurrencySymbol(currencyCode: String?): String {
    if (currencyCode.isNullOrBlank()) return ""
    return try {
        val code = currencyCode.uppercase().trim()
        // Check if it's a valid 3-letter ISO code to avoid unnecessary exceptions
        if (code.length == 3 && code.all { it.isLetter() }) {
            Currency.getInstance(code).symbol
        } else {
            currencyCode
        }
    } catch (e: Exception) {
        currencyCode
    }
}

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

fun getAllCurrencies(): List<String> {
    return Currency.getAvailableCurrencies().map { it.currencyCode }.sorted()
}
