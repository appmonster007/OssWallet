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
    val absAmount = validAmount.abs()
    val absFormatted = formatAmount(absAmount, precision)
    return if (validAmount.compareTo(BigDecimal.ZERO) < 0) "-$absFormatted $symbol" else "$absFormatted $symbol"
}

fun formatCurrencyLeft(amount: BigDecimal?, currencyCode: String?, precision: Int = 2): String {
    val validAmount = amount ?: BigDecimal.ZERO
    val symbol = getCurrencySymbol(currencyCode)
    val absAmount = validAmount.abs()
    val absFormatted = formatAmount(absAmount, precision)
    return if (validAmount.compareTo(BigDecimal.ZERO) < 0) "-$symbol$absFormatted" else "$symbol$absFormatted "
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

// Business logic functions - delegate to domain service
// These are kept for backward compatibility but should be replaced with direct calls to CurrencyService
@Deprecated("Use CurrencyService.convertAmount instead")
fun convertAmount(
    amount: BigDecimal,
    fromCurrency: String,
    toCurrency: String,
    exchangeRates: Map<String, BigDecimal>,
    precision: Int = 2
): BigDecimal {
    return CurrencyService.convertAmount(amount, fromCurrency, toCurrency, exchangeRates, precision)
}

@Deprecated("Use CurrencyService.calculateTotalInCurrency instead")
fun calculateTotalInCurrency(
    currencyTotals: Map<String, BigDecimal>,
    targetCurrency: String,
    exchangeRates: Map<String, BigDecimal>,
    precision: Int = 2
): BigDecimal {
    return CurrencyService.calculateTotalInCurrency(currencyTotals, targetCurrency, exchangeRates, precision)
}
