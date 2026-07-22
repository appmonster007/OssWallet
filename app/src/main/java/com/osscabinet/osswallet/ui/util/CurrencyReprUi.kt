package com.osscabinet.ossWallet.ui.util

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.currency.NumberFormatter
import com.osscabinet.ossWallet.domain.service.currency.NumberShortener
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
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
                v > BigDecimal.ZERO && spec.forceShowPlus -> partStyles.positive
                    ?: AppColors.positive

                else -> partStyles.neutral ?: Color.Unspecified
            }
        } else {
            partStyles.neutral ?: Color.Unspecified
        }

        return buildAnnotatedString {
            withStyle(baseStyle.copy(color = semanticColor)) {
                if (spec.isApproximate) {
                    partStyles.approx?.let { withStyle(it) { append("≈ ") } } ?: append("≈ ")
                }
                partStyles.sign?.let { withStyle(it) { append(parts.sign) } } ?: append(parts.sign)

                if (parts.symbolPosition == CurrencyService.SymbolPosition.LEFT) {
                    partStyles.symbol?.let { withStyle(it) { append(parts.symbol) } } ?: append(parts.symbol)
                    partStyles.value?.let { withStyle(it) { append(parts.value) } } ?: append(parts.value)
                } else {
                    partStyles.value?.let { withStyle(it) { append(parts.value) } } ?: append(parts.value)
                    if (parts.symbol.isNotEmpty()) {
                        append(" ")
                        partStyles.symbol?.let { withStyle(it) { append(parts.symbol) } } ?: append(parts.symbol)
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

@Preview(showBackground = true, name = "Light Mode")
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
fun CurrencyReprUiPreview() {
    OssWalletTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "Default (Positive):", style = MaterialTheme.typography.labelSmall)
                Text(
                    text = CurrencyReprUi.formatCurrencyAnnotated(
                        amount = BigDecimal("1234.56"),
                        currencyCode = "USD"
                    ),
                    style = MaterialTheme.typography.bodyLarge
                )

                Text(text = "Negative (Semantic Color):", style = MaterialTheme.typography.labelSmall)
                Text(
                    text = CurrencyReprUi.formatCurrencyAnnotated(
                        amount = BigDecimal("-1234.56"),
                        currencyCode = "USD"
                    ),
                    style = MaterialTheme.typography.bodyLarge
                )

                Text(text = "Force Show Plus:", style = MaterialTheme.typography.labelSmall)
                Text(
                    text = CurrencyReprUi.formatCurrencyAnnotated(
                        amount = BigDecimal("1234.56"),
                        currencyCode = "USD",
                        spec = CurrencyDisplaySpec(forceShowPlus = true)
                    ),
                    style = MaterialTheme.typography.bodyLarge
                )

                Text(text = "Shortened (Large Amount):", style = MaterialTheme.typography.labelSmall)
                Text(
                    text = CurrencyReprUi.formatCurrencyAnnotated(
                        amount = BigDecimal("1234567.89"),
                        currencyCode = "INR",
                        spec = CurrencyDisplaySpec(shortenThreshold = 10)
                    ),
                    style = MaterialTheme.typography.bodyLarge
                )

                Text(text = "Approximate:", style = MaterialTheme.typography.labelSmall)
                Text(
                    text = CurrencyReprUi.formatCurrencyAnnotated(
                        amount = BigDecimal("500"),
                        currencyCode = "EUR",
                        spec = CurrencyDisplaySpec(isApproximate = true)
                    ),
                    style = MaterialTheme.typography.bodyLarge
                )

                Text(text = "Custom Precision (0):", style = MaterialTheme.typography.labelSmall)
                Text(
                    text = CurrencyReprUi.formatCurrencyAnnotated(
                        amount = BigDecimal("1234.56"),
                        currencyCode = "JPY",
                        spec = CurrencyDisplaySpec(precision = 0)
                    ),
                    style = MaterialTheme.typography.bodyLarge
                )

                Text(text = "Partially Styled (Symbol Light):", style = MaterialTheme.typography.labelSmall)
                Text(
                    text = CurrencyReprUi.formatCurrencyAnnotated(
                        amount = BigDecimal("245800.50"),
                        currencyCode = "USD",
                        spec = CurrencyDisplaySpec(useSemanticColor = true),
                        partStyles = CurrencyPartStyles(
                            symbol = SpanStyle(
                                fontWeight = FontWeight.Light,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        )
                    ),
                    style = MaterialTheme.typography.headlineMedium
                )

                Text(text = "Partially Styled Neg(Symbol Light):", style = MaterialTheme.typography.labelSmall)
                Text(
                    text = CurrencyReprUi.formatCurrencyAnnotated(
                        amount = BigDecimal("-245800.50"),
                        currencyCode = "USD",
                        spec = CurrencyDisplaySpec(useSemanticColor = true),
                        partStyles = CurrencyPartStyles(
                            symbol = SpanStyle(
                                fontWeight = FontWeight.Light,
                                fontSize = 14.sp,
                                // No color specified, should inherit negative color
                            )
                        )
                    ),
                    style = MaterialTheme.typography.headlineMedium
                )

                Text(text = "Partially Styled (Value Bold):", style = MaterialTheme.typography.labelSmall)
                Text(
                    text = CurrencyReprUi.formatCurrencyAnnotated(
                        amount = BigDecimal("4200.00"),
                        currencyCode = "EUR",
                        partStyles = CurrencyPartStyles(
                            value = SpanStyle(fontWeight = FontWeight.ExtraBold),
                            symbol = SpanStyle(fontSize = 12.sp)
                        )
                    ),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
