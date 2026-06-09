package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

@Composable
fun CurrencySelector(
    selectedCurrency: String,
    onCurrencySelected: (String) -> Unit,
    supportedCurrencies: List<String>
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current

    val baseList = remember(supportedCurrencies) {
        if (supportedCurrencies.isEmpty()) listOf("USD") else supportedCurrencies.sorted()
    }
    val filteredList = remember(query, baseList) {
        if (query.isBlank()) baseList
        else baseList.filter { it.contains(query, ignoreCase = true) }
    }

    Box {
        Surface(
            modifier = Modifier.clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                query = ""
                expanded = true
            },
            shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
            color = AppColors.surfaceContainerLow,
            border = BorderStroke(1.dp, AppColors.outlineVariant)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    selectedCurrency.ifEmpty { "USD" },
                    style = AppTextStyles.chipLabel,
                    color = AppColors.contentPrimary
                )
                Icon(
                    if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm),
                    tint = AppColors.contentSecondary
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = AppColors.surfaceContainerHigh,
            shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
            modifier = Modifier.widthIn(min = 120.dp, max = 200.dp)
        ) {
            if (baseList.size > 6) {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = {
                        Text(
                            "Search...",
                            style = AppTextStyles.caption,
                            color = AppColors.contentSecondary
                        )
                    },
                    singleLine = true,
                    textStyle = AppTextStyles.caption.copy(color = AppColors.contentPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = AppColors.primary
                    )
                )
                HorizontalDivider(color = AppColors.outlineVariant)
            }
            filteredList.forEach { currency ->
                val isSelected = currency == selectedCurrency
                DropdownMenuItem(
                    modifier = Modifier
                        .fillMaxWidth(),
                    text = {
                        Text(
                            currency,
                            style = if (isSelected) AppTextStyles.chipLabelBold else AppTextStyles.chipLabel,
                            color = if (isSelected) AppColors.primary else AppColors.contentPrimary
                        )
                    },
                    onClick = {
                        onCurrencySelected(currency)
                        expanded = false
                        query = ""
                    },
                    colors = MenuItemColors(
                        textColor = if (isSelected) AppColors.primary else AppColors.contentPrimary,
                        leadingIconColor = AppColors.contentSecondary,
                        trailingIconColor = AppColors.primary,
                        disabledTextColor = AppColors.contentSecondary.copy(alpha = 0.38f),
                        disabledLeadingIconColor = AppColors.contentSecondary.copy(alpha = 0.38f),
                        disabledTrailingIconColor = AppColors.contentSecondary.copy(alpha = 0.38f)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

    }
}

@Composable
fun AeonStepperField(
    value: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    min: Int = 1,
    max: Int = Int.MAX_VALUE,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = AppColors.sheetSurface,
        modifier = modifier.height(40.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onDecrement,
                enabled = value > min,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Rounded.Remove,
                    contentDescription = "Decrease",
                    tint = if (value > min) AppColors.primary else AppColors.contentSecondary.copy(alpha = 0.3f)
                )
            }
            Text(
                text = "$value",
                style = AppTextStyles.transactionTitle.copy(textAlign = TextAlign.Center),
                modifier = Modifier.widthIn(min = 28.dp),
                textAlign = TextAlign.Center
            )
            IconButton(
                onClick = onIncrement,
                enabled = value < max,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = "Increase",
                    tint = if (value < max) AppColors.primary else AppColors.contentSecondary.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CurrencySelectorPreview() {
    OssWalletTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            CurrencySelector(
                selectedCurrency = "EUR",
                onCurrencySelected = {},
                supportedCurrencies = listOf("USD", "EUR", "GBP", "JPY", "INR")
            )
        }
    }
}
