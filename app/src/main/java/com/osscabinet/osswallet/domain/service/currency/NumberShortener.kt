package com.osscabinet.ossWallet.domain.service.currency

import android.icu.text.CompactDecimalFormat
import android.icu.util.ULocale
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Handles compact number formatting (e.g., 1K, 1M / 1 Lakh, 1 Crore).
 */
class NumberShortener(
    val isIndianGrouping: Boolean = false
) {
    fun shorten(value: BigDecimal, precision: Int): String {
        val locale = if (isIndianGrouping) ULocale("en_IN") else ULocale.getDefault()
        val compactFormat = CompactDecimalFormat.getInstance(locale, CompactDecimalFormat.CompactStyle.SHORT)
        compactFormat.minimumFractionDigits = precision
        compactFormat.maximumFractionDigits = precision
        compactFormat.roundingMode = RoundingMode.HALF_UP.ordinal
        return compactFormat.format(value)
    }
}
