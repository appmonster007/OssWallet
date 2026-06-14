package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatAuto
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import java.math.BigDecimal

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
    val primary = remember(amount, nativeCurrency) {
        formatAuto(amount, nativeCurrency)
    }
    val showSubtext = nativeCurrency != targetCurrency
    val subtext = if (showSubtext) {
        remember(amount, nativeCurrency, targetCurrency, rates) {
            val converted = CurrencyService.convertAmount(
                amount = amount,
                fromCurrency = nativeCurrency,
                toCurrency = targetCurrency,
                exchangeRates = rates,
            )
            "≈ ${formatAuto(converted, targetCurrency)}"
        }
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
 * payments, loans) but the user should still see the native amount they actually
 * deal with.
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
    val primary = remember(converted, targetCurrency) {
        formatAuto(converted, targetCurrency)
    }
    val subtext = if (!sameCurrency) {
        remember(amount, nativeCurrency) { formatAuto(amount, nativeCurrency) }
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
) {
    val primary = remember(amount, nativeCurrency) { formatAuto(amount, nativeCurrency) }
    val subtext = if (nativeCurrency != targetCurrency) {
        remember(convertedAmount, targetCurrency) {
            "≈ ${formatAuto(convertedAmount, targetCurrency)}"
        }
    } else null

    Column(modifier = modifier, horizontalAlignment = horizontalAlignment) {
        Text(text = primary, style = primaryStyle, color = primaryColor)
        if (subtext != null) {
            Text(text = subtext, style = subtextStyle, color = subtextColor)
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
    val primary = remember(amount, convertedAmount, nativeCurrency, targetCurrency) {
        formatAuto(if (sameCurrency) amount else convertedAmount, targetCurrency)
    }
    val subtext = if (!sameCurrency) {
        remember(amount, nativeCurrency) { formatAuto(amount, nativeCurrency) }
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
 * "native primary, ≈ converted subtext" pair. Subtext is null when `nativeCurrency`
 * already equals `targetCurrency`. Useful for call sites that hand pre-formatted strings
 * to a richer component (e.g. AeonTransactionItem) instead of rendering Text composables
 * directly.
 */
fun nativeWithConvertedStrings(
    amount: BigDecimal,
    nativeCurrency: String,
    targetCurrency: String,
    convertedAmount: BigDecimal?,
): Pair<String, String?> {
    val primary = formatAuto(amount, nativeCurrency)
    val subtext = if (nativeCurrency != targetCurrency && convertedAmount != null) {
        "≈ ${formatAuto(convertedAmount, targetCurrency)}"
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
    val text = remember(amount, nativeCurrency, targetCurrency, rates) {
        val primary = formatAuto(amount, nativeCurrency)
        if (nativeCurrency == targetCurrency) primary
        else {
            val converted = CurrencyService.convertAmount(
                amount = amount,
                fromCurrency = nativeCurrency,
                toCurrency = targetCurrency,
                exchangeRates = rates,
            )
            "$primary (≈ ${formatAuto(converted, targetCurrency)})"
        }
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
        Text(text = text, style = style, color = color)
    }
}
