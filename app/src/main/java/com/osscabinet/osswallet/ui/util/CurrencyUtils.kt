package com.osscabinet.ossWallet.ui.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

fun formatCurrencyRight(amount: BigDecimal, currencyCode: String, precision: Int = 2): String {
    val symbol = getCurrencySymbol(currencyCode)
    val absAmount = amount.abs()
    val absFormatted = formatAmount(absAmount, precision)
    return if (amount.compareTo(BigDecimal.ZERO) < 0) "-$absFormatted $symbol" else "$absFormatted $symbol"
}

fun formatCurrencyLeft(amount: BigDecimal, currencyCode: String, precision: Int = 2): String {
    val symbol = getCurrencySymbol(currencyCode)
    val absAmount = amount.abs()
    val absFormatted = formatAmount(absAmount, precision)
    return if (amount.compareTo(BigDecimal.ZERO) < 0) "-$symbol$absFormatted" else "$symbol$absFormatted "
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

fun getCurrencySymbol(currencyCode: String): String {
    return try {
        Currency.getInstance(currencyCode).symbol
    } catch (e: Exception) {
        currencyCode
    }
}

fun getCurrencyName(currencyCode: String): String {
    return try {
        Currency.getInstance(currencyCode).getDisplayName(Locale.getDefault())
    } catch (e: Exception) {
        currencyCode
    }
}

fun getAllCurrencies(): List<String> {
    return Currency.getAvailableCurrencies().map { it.currencyCode }.sorted()
}

fun convertAmount(
    amount: BigDecimal,
    fromCurrency: String,
    toCurrency: String,
    exchangeRates: Map<String, BigDecimal>,
    precision: Int = 2
): BigDecimal {
    if (fromCurrency == toCurrency) return amount.setScale(precision, RoundingMode.HALF_UP)

    // Check if we have both rates in the map
    // Note: The map usually contains rates relative to a base currency (e.g., USD or the user's base)
    val fromRate = exchangeRates[fromCurrency]
    val toRate = exchangeRates[toCurrency]

    if (fromRate == null || toRate == null || fromRate.signum() == 0) {
        // Fallback: If we can't find rates, return original amount or zero to avoid crashing
        // This is where "not working" usually happens if the map is empty or missing keys
        return amount.setScale(precision, RoundingMode.HALF_UP)
    }

    // Standard cross-rate conversion: (Amount / RateFromBase) * RateToBase
    val intermediatePrecision = maxOf(precision + 4, 10)
    val inBase = amount.divide(fromRate, intermediatePrecision, RoundingMode.HALF_UP)

    return inBase.multiply(toRate).setScale(precision, RoundingMode.HALF_UP)
}

fun calculateTotalInCurrency(
    currencyTotals: Map<String, BigDecimal>,
    targetCurrency: String,
    exchangeRates: Map<String, BigDecimal>,
    precision: Int = 2
): BigDecimal {
    return currencyTotals.entries.fold(BigDecimal.ZERO) { acc, (curr, amount) ->
        // Use high precision for intermediate conversion to avoid accumulation errors
        acc.add(convertAmount(amount, curr, targetCurrency, exchangeRates, precision + 4))
    }.setScale(precision, RoundingMode.HALF_UP)
}
