package com.osscabinet.gleemanager.ui.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

fun formatCurrency(amount: Double, currencyCode: String): String {
    val symbols = DecimalFormatSymbols(Locale.getDefault())
    val format = DecimalFormat("#,##0.000", symbols)
    val symbol = getCurrencySymbol(currencyCode)
    // Handle negative values correctly with the symbol
    val absoluteAmount = Math.abs(amount)
    val formattedAmount = format.format(absoluteAmount)
    return if (amount < 0) "-$symbol$formattedAmount" else "$symbol$formattedAmount"
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
