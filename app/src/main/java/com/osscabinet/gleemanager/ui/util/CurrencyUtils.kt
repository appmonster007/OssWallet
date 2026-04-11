package com.osscabinet.gleemanager.ui.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

fun formatCurrency(amount: Double, currencyCode: String): String {
    val symbol = getCurrencySymbol(currencyCode)
    val absAmount = Math.abs(amount)
    val absFormatted = formatAmount(absAmount)
    return if (amount < 0) "-$symbol$absFormatted" else "$symbol$absFormatted"
}

fun formatAmount(amount: Double): String {
    val symbols = DecimalFormatSymbols(Locale.getDefault())
    val format = DecimalFormat("#,##0.000", symbols)
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
