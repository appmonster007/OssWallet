package com.osscabinet.ossWallet.ui.util

import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import java.math.BigDecimal

/**
 * UI-specific formatting functions that delegate to the domain Currency services.
 */
fun formatCurrencyRight(amount: BigDecimal?, currencyCode: String?, precision: Int = 2): String {
    val validAmount = amount ?: BigDecimal.ZERO
    return CurrencyFormatter.formatRight(validAmount, currencyCode ?: "", precision)
}

fun formatCurrencyLeft(amount: BigDecimal?, currencyCode: String?, precision: Int = 2): String {
    val validAmount = amount ?: BigDecimal.ZERO
    return CurrencyFormatter.formatLeft(validAmount, currencyCode ?: "", precision)
}

fun formatAmount(amount: BigDecimal, precision: Int = 2): String {
    return CurrencyFormatter.formatAmount(amount, precision)
}

fun formatShortNumber(value: BigDecimal): String {
    return CurrencyFormatter.formatShortNumber(value)
}

fun getCurrencySymbol(currencyCode: String?): String {
    return CurrencyFormatter.getCurrencySymbol(currencyCode)
}

fun getCurrencyName(currencyCode: String?): String {
    return CurrencyFormatter.getCurrencyName(currencyCode)
}

fun getAllCurrencies(): List<String> {
    return CurrencyService.getAllCurrencies()
}
