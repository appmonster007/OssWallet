package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.ui.theme.AccentBlue
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.PositiveAmountColor
import com.osscabinet.ossWallet.ui.util.CurrencyDisplaySpec
import com.osscabinet.ossWallet.ui.util.CurrencyPartStyles
import com.osscabinet.ossWallet.ui.util.CurrencyReprUi
import com.osscabinet.ossWallet.ui.util.formatCurrency as formatCurrencyBase
import java.math.BigDecimal

/**
 * Overload of [CurrencyText] that automatically determines color and formatting
 * based on the [EntryMode] (Income, Expense, Transfer).
 */
@Composable
fun CurrencyText(
    amount: BigDecimal,
    currencyCode: String,
    mode: EntryMode,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
) {
    val color = getEntryAmountColor(mode)
    val formatted = formatCurrency(amount, currencyCode, mode)

    Text(
        text = formatted,
        style = style,
        color = color,
        modifier = modifier
    )
}

/**
 * Returns the semantic color for a given [EntryMode].
 */
@Composable
@ReadOnlyComposable
fun getEntryAmountColor(mode: EntryMode): Color {
    return when (mode) {
        EntryMode.INCOME -> PositiveAmountColor
        EntryMode.TRANSFER -> AccentBlue
        else -> AppColors.error
    }
}

/**
 * Formats a currency amount based on the [EntryMode].
 * Handles the prefix (⇄ for transfers) and sign (+/-).
 */
fun formatCurrency(
    amount: BigDecimal,
    currencyCode: String,
    mode: EntryMode
): String {
    val amountWithSign = when (mode) {
        EntryMode.INCOME, EntryMode.TRANSFER -> amount.abs()
        else -> amount.abs().negate()
    }

    val formatted = formatCurrencyBase(
        amount = amountWithSign,
        currencyCode = currencyCode,
        spec = CurrencyDisplaySpec(
            forceShowPlus = mode == EntryMode.INCOME,
            shortenThreshold = -1
        )
    )

    return if (mode == EntryMode.TRANSFER) "⇄ $formatted" else formatted
}

/**
 * Single-call composable for displaying a formatted currency amount.
 *
 * [style] is the base for the whole Text composable.
 * [spec] governs formatting behavior (sign, precision, shortening, semantic color).
 * [partStyles] fields override individual spans — null = inherit [style] as SpanStyle.
 *
 * Examples:
 *   // Zero config
 *   CurrencyText(amount = balance, currencyCode = "USD", style = AppTextStyles.headline)
 *
 *   // Dimmer symbol, larger value
 *   CurrencyText(
 *       amount = balance, currencyCode = "EUR",
 *       spec = CurrencyDisplaySpec(forceShowPlus = true),
 *       partStyles = CurrencyPartStyles(symbol = SpanStyle(fontSize = 12.sp, color = AppColors.muted))
 *   )
 */
@Composable
fun CurrencyText(
    amount: BigDecimal?,
    currencyCode: String?,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    spec: CurrencyDisplaySpec = CurrencyDisplaySpec(),
    partStyles: CurrencyPartStyles = CurrencyPartStyles(),
) {
    val base = style.toSpanStyle()
    val text = CurrencyReprUi.formatCurrencyAnnotated(
        amount = amount,
        currencyCode = currencyCode,
        spec = spec.copy(useSemanticColor = if (color == Color.Unspecified) spec.useSemanticColor else false),
        baseStyle = base,
        partStyles = partStyles,
    )
    Text(text = text, style = style, color = color, modifier = modifier)
}

/**
 * Renders an amount where the user's native currency is the primary line and the
 * target-currency equivalent (≈ …) appears as a subtext below, only when the two differ.
 *
 * Use this when the original currency carries identity (e.g. a transaction's currency,
 * a per-currency balance row).
 */
@Composable
fun NativeWithConvertedSubtext(
    amount: BigDecimal,
    nativeCurrency: String,
    targetCurrency: String,
    rates: ExchangeRateSnapshot,
    modifier: Modifier = Modifier,
    primaryStyle: TextStyle = LocalTextStyle.current,
    subtextStyle: TextStyle = MaterialTheme.typography.labelSmall,
    primaryColor: Color = Color.Unspecified,
    subtextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    horizontalAlignment: Alignment.Horizontal = Alignment.End,
) {
    val primary = CurrencyReprUi.formatCurrencyAnnotated(
        amount = amount,
        currencyCode = nativeCurrency,
        spec = CurrencyDisplaySpec(useSemanticColor = primaryColor == Color.Unspecified),
    )
    val showSubtext = nativeCurrency != targetCurrency
    val subtext = if (showSubtext) {
        val converted = remember(amount, nativeCurrency, targetCurrency, rates) {
            CurrencyService.convertAmount(
                amount = amount,
                fromCurrency = nativeCurrency,
                toCurrency = targetCurrency,
                exchangeRates = rates,
            )
        }
        CurrencyReprUi.formatCurrencyAnnotated(
            amount = converted,
            currencyCode = targetCurrency,
            spec = CurrencyDisplaySpec(isApproximate = true, useSemanticColor = false),
        )
    } else null

    Column(modifier = modifier, horizontalAlignment = horizontalAlignment) {
        Text(text = primary, style = primaryStyle, color = primaryColor)
        if (subtext != null) {
            Text(text = subtext, style = subtextStyle, color = subtextColor)
        }
    }
}

/**
 * Renders an amount where the target currency (converted) is the primary line and the
 * original native amount appears as a subtext below, only when the two differ.
 *
 * Use this when comparability across rows is what matters (account list, planned
 * payments, loans) but the user should still see the native amount they actually deal with.
 */
@Composable
fun ConvertedWithNativeSubtext(
    amount: BigDecimal,
    nativeCurrency: String,
    targetCurrency: String,
    rates: ExchangeRateSnapshot,
    modifier: Modifier = Modifier,
    primaryStyle: TextStyle = LocalTextStyle.current,
    subtextStyle: TextStyle = MaterialTheme.typography.labelSmall,
    primaryColor: Color = Color.Unspecified,
    subtextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    horizontalAlignment: Alignment.Horizontal = Alignment.End,
) {
    val sameCurrency = nativeCurrency == targetCurrency
    val converted = remember(amount, nativeCurrency, targetCurrency, rates) {
        if (sameCurrency) amount
        else CurrencyService.convertAmount(
            amount = amount,
            fromCurrency = nativeCurrency,
            toCurrency = targetCurrency,
            exchangeRates = rates,
        )
    }
    val primary = CurrencyReprUi.formatCurrencyAnnotated(
        amount = converted,
        currencyCode = targetCurrency,
        spec = CurrencyDisplaySpec(useSemanticColor = primaryColor == Color.Unspecified),
    )
    val subtext = if (!sameCurrency) {
        CurrencyReprUi.formatCurrencyAnnotated(
            amount = amount,
            currencyCode = nativeCurrency,
            spec = CurrencyDisplaySpec(useSemanticColor = false),
        )
    } else null

    Column(modifier = modifier, horizontalAlignment = horizontalAlignment) {
        Text(text = primary, style = primaryStyle, color = primaryColor)
        if (subtext != null) {
            Text(text = subtext, style = subtextStyle, color = subtextColor)
        }
    }
}

/**
 * Same as [NativeWithConvertedSubtext] but takes a precomputed [convertedAmount] instead
 * of re-running conversion at render time. Use this when the data layer already produced
 * the equivalent (e.g. `record.amountInBase`, `AccountSummaryItem.balanceInBase`).
 */
@Composable
fun NativeWithConvertedSubtext(
    amount: BigDecimal,
    nativeCurrency: String,
    targetCurrency: String,
    convertedAmount: BigDecimal,
    modifier: Modifier = Modifier,
    primaryStyle: TextStyle = LocalTextStyle.current,
    subtextStyle: TextStyle = MaterialTheme.typography.labelSmall,
    primaryColor: Color = Color.Unspecified,
    subtextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    horizontalAlignment: Alignment.Horizontal = Alignment.End,
    showSubtextSpace: Boolean = true
) {
    val primary = CurrencyReprUi.formatCurrencyAnnotated(
        amount = amount,
        currencyCode = nativeCurrency,
        spec = CurrencyDisplaySpec(useSemanticColor = primaryColor == Color.Unspecified),
    )
    val subtext = if (nativeCurrency != targetCurrency) {
        CurrencyReprUi.formatCurrencyAnnotated(
            amount = convertedAmount,
            currencyCode = targetCurrency,
            spec = CurrencyDisplaySpec(isApproximate = true, useSemanticColor = false),
        )
    } else null

    Column(modifier = modifier, horizontalAlignment = horizontalAlignment) {
        Text(text = primary, style = primaryStyle, color = primaryColor)
        when {
            subtext != null -> Text(text = subtext, style = subtextStyle, color = subtextColor)
            showSubtextSpace -> Text(text = " ", style = subtextStyle, color = Color.Transparent)
        }
    }
}

/**
 * Same as [ConvertedWithNativeSubtext] but takes a precomputed [convertedAmount] instead
 * of re-running conversion at render time.
 */
@Composable
fun ConvertedWithNativeSubtext(
    amount: BigDecimal,
    nativeCurrency: String,
    targetCurrency: String,
    convertedAmount: BigDecimal,
    modifier: Modifier = Modifier,
    primaryStyle: TextStyle = LocalTextStyle.current,
    subtextStyle: TextStyle = MaterialTheme.typography.labelSmall,
    primaryColor: Color = Color.Unspecified,
    subtextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    horizontalAlignment: Alignment.Horizontal = Alignment.End,
) {
    val sameCurrency = nativeCurrency == targetCurrency
    val primary = CurrencyReprUi.formatCurrencyAnnotated(
        amount = if (sameCurrency) amount else convertedAmount,
        currencyCode = targetCurrency,
        spec = CurrencyDisplaySpec(useSemanticColor = primaryColor == Color.Unspecified),
    )
    val subtext = if (!sameCurrency) {
        CurrencyReprUi.formatCurrencyAnnotated(
            amount = amount,
            currencyCode = nativeCurrency,
            spec = CurrencyDisplaySpec(useSemanticColor = false),
        )
    } else null

    Column(modifier = modifier, horizontalAlignment = horizontalAlignment) {
        Text(text = primary, style = primaryStyle, color = primaryColor)
        if (subtext != null) {
            Text(text = subtext, style = subtextStyle, color = subtextColor)
        }
    }
}

/**
 * Non-composable helper that returns `(primary, subtext?)` formatted strings for a
 * "native primary, ≈ converted subtext" pair. Subtext is null when currencies match.
 * Use for call sites that pass pre-formatted strings to richer components (e.g. AeonTransactionItem).
 */
fun nativeWithConvertedStrings(
    amount: BigDecimal,
    nativeCurrency: String,
    targetCurrency: String,
    convertedAmount: BigDecimal?,
): Pair<String, String?> {
    val formatter = CurrencyReprUi.CurrencyNumberFormatter()
    val primary = formatter.format(amount, nativeCurrency)
    val subtext = if (nativeCurrency != targetCurrency && convertedAmount != null) {
        "≈ ${formatter.format(convertedAmount, targetCurrency)}"
    } else null
    return primary to subtext
}

/**
 * Inline single-line equivalent. Renders "primary (≈ subtext)" on one line if currencies
 * differ; otherwise just the primary. Useful in tight rows where a stacked Column is too tall.
 */
@Composable
fun NativeWithConvertedInline(
    amount: BigDecimal,
    nativeCurrency: String,
    targetCurrency: String,
    rates: ExchangeRateSnapshot,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
) {
    val primary = CurrencyReprUi.formatCurrencyAnnotated(
        amount = amount,
        currencyCode = nativeCurrency,
        spec = CurrencyDisplaySpec(useSemanticColor = color == Color.Unspecified),
    )

    val text = if (nativeCurrency == targetCurrency) primary
    else {
        val converted = remember(amount, nativeCurrency, targetCurrency, rates) {
            CurrencyService.convertAmount(
                amount = amount,
                fromCurrency = nativeCurrency,
                toCurrency = targetCurrency,
                exchangeRates = rates,
            )
        }
        val secondary = CurrencyReprUi.formatCurrencyAnnotated(
            amount = converted,
            currencyCode = targetCurrency,
            spec = CurrencyDisplaySpec(useSemanticColor = false, isApproximate = true),
        )
        buildAnnotatedString {
            append(primary)
            append(" (")
            append(secondary)
            append(")")
        }
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
        Text(text = text, style = style, color = color)
    }
}
