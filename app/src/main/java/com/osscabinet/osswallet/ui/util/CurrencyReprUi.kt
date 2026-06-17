package com.osscabinet.ossWallet.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.currency.NumberFormatter
import com.osscabinet.ossWallet.domain.service.currency.NumberShortener
import com.osscabinet.ossWallet.ui.theme.AppColors
import java.math.BigDecimal

/**
 * Formatting behavior — what the number means, how precise, when to shorten.
 * Compose-free; safe to construct outside composables and in non-UI layers.
 */
data class CurrencyDisplaySpec(
    val forceShowPlus: Boolean = false,
    val useSemanticColor: Boolean = true,
    val isApproximate: Boolean = false,
    val precision: Int = CurrencyService.precision,
    val shortenThreshold: Int = 15,
)

/**
 * Per-part SpanStyle overrides for a formatted currency string.
 * null on any field = inherit the caller's base style.
 * Build inside a composable or remember block (contains Compose types).
 */
data class CurrencyPartStyles(
    val sign: SpanStyle? = null,
    val symbol: SpanStyle? = null,
    val value: SpanStyle? = null,
    val approx: SpanStyle? = null,
    val positive: Color? = null,
    val negative: Color? = null,
    val neutral: Color? = null,
)

object CurrencyReprUi {

    /** Decomposed parts of a formatted currency string, preserving layout intent. */
    data class CurrencyParts(
        val sign: String,
        val symbol: String,
        val value: String,
        val symbolPosition: CurrencyService.SymbolPosition,
    ) {
        fun fullString(): String = if (symbolPosition == CurrencyService.SymbolPosition.LEFT) {
            "$sign$symbol$value"
        } else {
            "$sign$value $symbol"
        }
    }

    /**
     * Orchestrates number formatting and shortening to stay within [CurrencyDisplaySpec.shortenThreshold].
     * Takes [system] at construction (locale config); per-call options come via [CurrencyDisplaySpec].
     */
    class CurrencyNumberFormatter(
        private val system: CurrencyService.FormattingSystem = CurrencyService.activeSystem,
    ) {
        private val formatter = NumberFormatter(
            decimalSeparator = system.decimalSeparator,
            groupingSeparator = system.groupingSeparator,
            isIndianGrouping = system.isIndianGrouping,
        )
        private val shortener = NumberShortener(isIndianGrouping = system.isIndianGrouping)

        fun formatParts(
            amount: BigDecimal?,
            currencyCode: String?,
            spec: CurrencyDisplaySpec = CurrencyDisplaySpec(),
        ): CurrencyParts {
            val valid = amount ?: BigDecimal.ZERO
            val abs = valid.abs()
            val sign = when {
                valid < BigDecimal.ZERO -> "-"
                spec.forceShowPlus && valid > BigDecimal.ZERO -> "+"
                else -> ""
            }
            val symbol = CurrencyService.getCurrencySymbol(currencyCode)

            fun build(v: String) = CurrencyParts(sign, symbol, v, system.symbolPosition)

            val standard = build(formatter.format(abs, spec.precision))
            if (spec.shortenThreshold == -1 || standard.fullString().length <= spec.shortenThreshold) return standard

            for (p in spec.precision downTo 0) {
                val candidate = build(shortener.shorten(abs, p))
                if (candidate.fullString().length <= spec.shortenThreshold) return candidate
            }
            return build(shortener.shorten(abs, 0))
        }

        fun format(
            amount: BigDecimal?,
            currencyCode: String?,
            spec: CurrencyDisplaySpec = CurrencyDisplaySpec(),
        ): String = formatParts(amount, currencyCode, spec).fullString()
    }

    /**
     * Builds an [AnnotatedString] for a currency amount with per-part style control.
     *
     * [spec] governs formatting behavior (sign, precision, shortening, semantic color).
     * [baseStyle] is the fallback SpanStyle for every part not overridden in [partStyles].
     */
    @Composable
    @ReadOnlyComposable
    fun formatCurrencyAnnotated(
        amount: BigDecimal?,
        currencyCode: String?,
        spec: CurrencyDisplaySpec = CurrencyDisplaySpec(),
        baseStyle: SpanStyle = SpanStyle(),
        partStyles: CurrencyPartStyles = CurrencyPartStyles(),
    ): AnnotatedString {
        val parts = CurrencyNumberFormatter().formatParts(amount, currencyCode, spec)

        val semanticColor = if (spec.useSemanticColor) {
            val v = amount ?: BigDecimal.ZERO
            when {
                v < BigDecimal.ZERO -> partStyles.negative ?: AppColors.negative
                v > BigDecimal.ZERO && spec.forceShowPlus -> partStyles.positive ?: AppColors.positive
                else -> partStyles.neutral ?: Color.Unspecified
            }
        } else {
            partStyles.neutral ?: Color.Unspecified
        }

        return buildAnnotatedString {
            withStyle(baseStyle.copy(color = semanticColor)) {
                if (spec.isApproximate) {
                    withStyle(partStyles.approx ?: baseStyle) { append("≈ ") }
                }
                withStyle(partStyles.sign ?: baseStyle) { append(parts.sign) }
                if (parts.symbolPosition == CurrencyService.SymbolPosition.LEFT) {
                    withStyle(partStyles.symbol ?: baseStyle) { append(parts.symbol) }
                    withStyle(partStyles.value ?: baseStyle) { append(parts.value) }
                } else {
                    withStyle(partStyles.value ?: baseStyle) { append(parts.value) }
                    if (parts.symbol.isNotEmpty()) {
                        withStyle(partStyles.symbol ?: baseStyle) {
                            append(" ")
                            append(parts.symbol)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Formats a currency amount to a plain [String]. Use for non-Compose contexts
 * (chart labels, widget text, list item subtitles) where [AnnotatedString] is not needed.
 */
fun formatCurrency(
    amount: BigDecimal?,
    currencyCode: String? = null,
    spec: CurrencyDisplaySpec = CurrencyDisplaySpec(),
): String = CurrencyReprUi.CurrencyNumberFormatter().format(amount, currencyCode, spec)
