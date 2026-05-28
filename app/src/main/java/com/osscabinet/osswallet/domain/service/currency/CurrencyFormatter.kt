package com.osscabinet.ossWallet.domain.service.currency

import android.icu.text.CompactDecimalFormat
import android.icu.text.DecimalFormat
import android.icu.text.DecimalFormatSymbols
import android.icu.text.NumberFormat
import android.icu.util.Currency
import android.icu.util.ULocale
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.activeSystem
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatValue
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/**
 * Domain service for currency and number formatting.
 * Handles localized grouping, decimal separators, and smart shortening.
 */
object CurrencyFormatter {

    /**
     * Global settings used for automatic formatting.
     */
    var precision: Int = 2
    var activeSystem: FormattingSystem = FormattingSystem.WESTERN

    enum class SymbolPosition { LEFT, RIGHT }

    /**
     * Formatting system defines the style of number and currency display.
     */
    enum class FormattingSystem(
        val decimalSeparator: Char,
        val groupingSeparator: Char,
        val symbolPosition: SymbolPosition,
        val isIndianGrouping: Boolean = false
    ) {
        WESTERN(
            decimalSeparator = '.',
            groupingSeparator = ',',
            symbolPosition = SymbolPosition.LEFT
        ),
        EUROPEAN(
            decimalSeparator = ',',
            groupingSeparator = '.',
            symbolPosition = SymbolPosition.RIGHT
        ),
        INDIAN(
            decimalSeparator = '.',
            groupingSeparator = ',',
            symbolPosition = SymbolPosition.LEFT,
            isIndianGrouping = true
        ),
        SWISS(
            decimalSeparator = '.',
            groupingSeparator = '\'',
            symbolPosition = SymbolPosition.LEFT
        ),
        FRENCH(
            decimalSeparator = ',',
            groupingSeparator = ' ',
            symbolPosition = SymbolPosition.RIGHT
        ),
        ISO(decimalSeparator = '.', groupingSeparator = ' ', symbolPosition = SymbolPosition.LEFT),
        SOUTH_AMERICAN(
            decimalSeparator = ',',
            groupingSeparator = '.',
            symbolPosition = SymbolPosition.LEFT
        );

        companion object {
            fun fromString(value: String?): FormattingSystem =
                entries.find { it.name.equals(value, ignoreCase = true) } ?: WESTERN
        }
    }

    /**
     * Unified method for formatting values.
     * Handles separator system, shortening system, and precision system.
     *
     * @param value The value to format.
     * @param shorten Whether to use compact notation (K, M, B / Lakh, Crore).
     * @param precision Decimal places.
     * @param isIndianGrouping Use Indian (10,00,000) or Western (1,000,000) grouping.
     * @param decimalSeparator Optional override for the decimal separator.
     * @param groupingSeparator Optional override for the grouping separator.
     */
    fun formatValue(
        value: BigDecimal?,
        shorten: Boolean = false,
        precision: Int = this.precision,
        isIndianGrouping: Boolean = false,
        decimalSeparator: Char? = null,
        groupingSeparator: Char? = null
    ): String {
        val validAmount = value ?: BigDecimal.ZERO
        val locale = if (isIndianGrouping) ULocale("en_IN") else ULocale.getDefault()

        val symbols = DecimalFormatSymbols(locale)
        decimalSeparator?.let { symbols.decimalSeparator = it }
        groupingSeparator?.let { symbols.groupingSeparator = it }

        return if (shorten) {
            val compactFormat =
                CompactDecimalFormat.getInstance(locale, CompactDecimalFormat.CompactStyle.SHORT)
            compactFormat.minimumFractionDigits = precision
            compactFormat.maximumFractionDigits = precision
            compactFormat.roundingMode = RoundingMode.HALF_UP.ordinal
            compactFormat.format(validAmount)
        } else {
            val nf = NumberFormat.getInstance(locale) as DecimalFormat
            nf.decimalFormatSymbols = symbols
            nf.minimumFractionDigits = precision
            nf.maximumFractionDigits = precision
            nf.roundingMode = RoundingMode.HALF_UP.ordinal
            nf.format(validAmount)
        }
    }

    /**
     * Unified method for formatting currency.
     * Uses [formatValue] and formats it with currency system (symbol and position).
     *
     * @param amount The value to format.
     * @param currencyCode ISO 4217 currency code (e.g., "USD").
     * @param shorten Whether to use compact notation.
     * @param precision Decimal places.
     * @param symbolPosition Position of the currency symbol.
     * @param isIndianGrouping Western or Indian grouping.
     * @param decimalSeparator Optional override for the decimal separator.
     * @param groupingSeparator Optional override for the grouping separator.
     */
    fun formatCurrency(
        amount: BigDecimal?,
        currencyCode: String?,
        shorten: Boolean = false,
        precision: Int = this.precision,
        symbolPosition: SymbolPosition = SymbolPosition.LEFT,
        isIndianGrouping: Boolean = false,
        decimalSeparator: Char? = null,
        groupingSeparator: Char? = null
    ): String {
        val formattedValue = formatValue(
            value = amount,
            shorten = shorten,
            precision = precision,
            isIndianGrouping = isIndianGrouping,
            decimalSeparator = decimalSeparator,
            groupingSeparator = groupingSeparator
        )

        val symbol = getCurrencySymbol(currencyCode)
        if (symbol.isEmpty()) return formattedValue

        return when (symbolPosition) {
            SymbolPosition.LEFT -> "$symbol$formattedValue"
            SymbolPosition.RIGHT -> "$formattedValue $symbol"
        }
    }

    /**
     * High-level formatting method that uses global settings.
     * Automatically applies separators, symbol position, and grouping based on [activeSystem].
     */
    fun formatAuto(
        amount: BigDecimal?,
        currencyCode: String? = null,
        shorten: Boolean = false,
        precision: Int = this.precision
    ): String {
        return formatForSystem(
            amount = amount,
            system = activeSystem,
            currencyCode = currencyCode,
            shorten = shorten,
            precision = precision
        )
    }

    /**
     * Formatting with a specific system, useful for previews or overrides.
     */
    fun formatForSystem(
        amount: BigDecimal?,
        system: FormattingSystem,
        currencyCode: String? = null,
        shorten: Boolean = false,
        precision: Int = this.precision
    ): String {
        return if (currencyCode != null) {
            formatCurrency(
                amount = amount,
                currencyCode = currencyCode,
                shorten = shorten,
                precision = precision,
                symbolPosition = system.symbolPosition,
                isIndianGrouping = system.isIndianGrouping,
                decimalSeparator = system.decimalSeparator,
                groupingSeparator = system.groupingSeparator
            )
        } else {
            formatValue(
                value = amount,
                shorten = shorten,
                precision = precision,
                isIndianGrouping = system.isIndianGrouping,
                decimalSeparator = system.decimalSeparator,
                groupingSeparator = system.groupingSeparator
            )
        }
    }

    /**
     * Returns the symbol for a given currency code.
     */
    fun getCurrencySymbol(currencyCode: String?): String {
        if (currencyCode.isNullOrBlank()) return ""
        return try {
            val code = currencyCode.uppercase().trim()
            if (code.length == 3 && code.all { it.isLetter() }) {
                Currency.getInstance(code).getSymbol(ULocale.getDefault())
            } else {
                currencyCode
            }
        } catch (e: Exception) {
            currencyCode
        }
    }

    /**
     * Returns the full name for a given currency code.
     */
    fun getCurrencyName(currencyCode: String?): String {
        if (currencyCode.isNullOrBlank()) return ""
        return try {
            val code = currencyCode.uppercase().trim()
            if (code.length == 3 && code.all { it.isLetter() }) {
                Currency.getInstance(code).getDisplayName(Locale.getDefault())
            } else {
                currencyCode
            }
        } catch (e: Exception) {
            currencyCode
        }
    }
}
