package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.components.CurrencySelector
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

/**
 * Standardized amount display with auto-resizing text and currency selector.
 * Following the "Architect Slate" design specification.
 */
@Composable
fun EntryAmountDisplay(
    amount: String,
    currency: String,
    onCurrencyClick: (String) -> Unit,
    onAmountClick: () -> Unit,
    modifier: Modifier = Modifier,
    amountColor: Color = AppColors.primary,
    supportedCurrencies: List<String> = listOf("USD"),
    showCurrency: Boolean = true,
    label: String? = null,
    prefix: String = ""
) {
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AeonTheme.dimensions.containerCornerRadius))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onAmountClick()
            }
            .padding(vertical = AeonTheme.dimensions.spacingSm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (label != null) {
            Text(
                text = label.uppercase(),
                style = AppTextStyles.sectionOverline,
                color = AppColors.contentSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (showCurrency) {
                CurrencySelector(
                    selectedCurrency = currency.ifEmpty { "USD" },
                    onCurrencySelected = onCurrencyClick,
                    supportedCurrencies = supportedCurrencies
                )

                Spacer(modifier = Modifier.width(AeonTheme.dimensions.spacingMd))
            }

            val amountDisplay = if (amount.isEmpty()) "0.00" else amount
            val fullDisplay = if (amount.isEmpty()) amountDisplay else "$prefix$amountDisplay"
            var multiplier by remember(fullDisplay) { mutableStateOf(1f) }

            Text(
                text = fullDisplay,
                style = AppTextStyles.calculatorDisplay.copy(
                    letterSpacing = (-1).sp,
                    fontSize = (AppTextStyles.calculatorDisplay.fontSize.value * multiplier).sp
                ),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                onTextLayout = { textLayoutResult ->
                    if (textLayoutResult.didOverflowWidth && multiplier > 0.3f) {
                        multiplier *= 0.8f
                    }
                },
                color = if (amount.isEmpty()) amountColor.copy(alpha = 0.38f) else amountColor,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EntryAmountDisplayPreview() {
    OssWalletTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                EntryAmountDisplay(
                    amount = "1,250.50",
                    currency = "USD",
                    onCurrencyClick = {},
                    onAmountClick = {},
                    label = "Amount"
                )

                EntryAmountDisplay(
                    amount = "",
                    currency = "EUR",
                    onCurrencyClick = {},
                    onAmountClick = {},
                    amountColor = AppColors.error,
                    label = "Expense Amount"
                )
            }
        }
    }
}
