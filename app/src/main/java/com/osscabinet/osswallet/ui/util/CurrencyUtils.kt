package com.osscabinet.ossWallet.ui.util

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

fun formatCurrencyRight(amount: BigDecimal, currencyCode: String): String {
    val symbol = getCurrencySymbol(currencyCode)
    val absAmount = amount.abs()
    val absFormatted = formatAmount(absAmount)
    return if (amount.compareTo(BigDecimal.ZERO) < 0) "-$absFormatted $symbol" else "$absFormatted $symbol"
}

fun formatCurrencyLeft(amount: BigDecimal, currencyCode: String): String {
    val symbol = getCurrencySymbol(currencyCode)
    val absAmount = amount.abs()
    val absFormatted = formatAmount(absAmount)
    return if (amount.compareTo(BigDecimal.ZERO) < 0) "-$symbol$absFormatted" else "$symbol$absFormatted "
}

fun formatAmount(amount: BigDecimal): String {
    val symbols = DecimalFormatSymbols(Locale.getDefault())
    val format = DecimalFormat("#,##0.00", symbols)
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
