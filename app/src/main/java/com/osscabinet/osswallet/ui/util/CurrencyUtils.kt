package com.osscabinet.ossWallet.ui.util

import java.math.BigDecimal
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
    val pattern = if (precision == 3) "#,##0.000" else "#,##0.00"
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
