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

    val fromRate = exchangeRates[fromCurrency] ?: BigDecimal.ONE
    val toRate = exchangeRates[toCurrency] ?: BigDecimal.ONE

    // We use a high intermediate precision to avoid loss during calculation
    // but the final result is always rounded to the requested precision.
    val intermediatePrecision = maxOf(precision + 4, 10)

    val inBase = if (fromRate.signum() != 0) {
        amount.divide(fromRate, intermediatePrecision, RoundingMode.HALF_UP)
    } else {
        amount
    }

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
