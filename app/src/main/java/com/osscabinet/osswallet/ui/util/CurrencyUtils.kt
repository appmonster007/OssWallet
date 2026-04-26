package com.osscabinet.ossWallet.ui.util

import com.osscabinet.ossWallet.domain.service.CurrencyService
import java.math.BigDecimal

/**
 * UI-specific formatting functions that delegate to the domain CurrencyService.
 */
fun formatCurrencyRight(amount: BigDecimal?, currencyCode: String?, precision: Int = 2): String {
    val validAmount = amount ?: BigDecimal.ZERO
    return CurrencyService.formatRight(validAmount, currencyCode ?: "", precision)
}

fun formatCurrencyLeft(amount: BigDecimal?, currencyCode: String?, precision: Int = 2): String {
    val validAmount = amount ?: BigDecimal.ZERO
    return CurrencyService.formatLeft(validAmount, currencyCode ?: "", precision)
}

fun formatAmount(amount: BigDecimal, precision: Int = 2): String {
    return CurrencyService.formatAmount(amount, precision)
}

fun formatShortNumber(value: BigDecimal): String {
    return CurrencyService.formatShortNumber(value)
}

fun getCurrencySymbol(currencyCode: String?): String {
    return CurrencyService.getCurrencySymbol(currencyCode)
}

fun getCurrencyName(currencyCode: String?): String {
    return CurrencyService.getCurrencyName(currencyCode)
}

fun getAllCurrencies(): List<String> {
    return CurrencyService.getAllCurrencies()
}
