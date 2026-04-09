package com.osscabinet.gleemanager.ui.util

import java.util.Currency

fun getCurrencySymbol(currencyCode: String): String {
    return try {
        Currency.getInstance(currencyCode).symbol
    } catch (e: Exception) {
        "$"
    }
}
