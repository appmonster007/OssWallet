package com.osscabinet.ossWallet.domain.service.currency

import android.icu.text.DecimalFormat
import android.icu.text.DecimalFormatSymbols
import android.icu.text.NumberFormat
import android.icu.util.ULocale
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Handles localized number formatting with specific separators and grouping.
 */
class NumberFormatter(
    val decimalSeparator: Char,
    val groupingSeparator: Char,
    val isIndianGrouping: Boolean = false
) {
    fun format(value: BigDecimal, precision: Int): String {
        val locale = if (isIndianGrouping) ULocale("en_IN") else ULocale.getDefault()
        val symbols = DecimalFormatSymbols(locale)
        symbols.decimalSeparator = decimalSeparator
        symbols.groupingSeparator = groupingSeparator

        val nf = NumberFormat.getInstance(locale) as DecimalFormat
        nf.decimalFormatSymbols = symbols
        nf.minimumFractionDigits = precision
        nf.maximumFractionDigits = precision
        nf.roundingMode = RoundingMode.HALF_UP.ordinal
        return nf.format(value)
    }
}
