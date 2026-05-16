package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.components.CurrencySelector
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

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
    showCurrency: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AeonTheme.dimensions.containerCornerRadius))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onAmountClick() }
            .padding(vertical = AeonTheme.dimensions.spacingSm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
            var multiplier by remember(amountDisplay) { mutableStateOf(1f) }

            Text(
                text = amountDisplay,
                style = AppTextStyles.calculatorDisplay.copy(
                    fontWeight = FontWeight.Bold,
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
                color = if (amount.isEmpty()) amountColor.copy(alpha = 0.3f) else amountColor,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
    }
}
