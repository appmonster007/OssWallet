package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
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
            shape = RoundedCornerShape(12.dp),
            color = AppColors.insetSurface.copy(alpha = 0.4f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    selectedCurrency.ifEmpty { "USD" },
                    style = AppTextStyles.drawerItemLabel.copy(
                        fontWeight = FontWeight.Bold,
                        color = AppColors.contentSecondary
                    )
                )
                Icon(
                    if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = AppColors.contentSecondary
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = AppColors.cardSurface,
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
                            style = AppTextStyles.drawerItemLabel,
                            color = AppColors.contentSecondary.copy(alpha = 0.5f)
                        )
                    },
                    singleLine = true,
                    textStyle = AppTextStyles.drawerItemLabel.copy(color = AppColors.contentPrimary),
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
                HorizontalDivider(color = AppColors.divider.copy(alpha = 0.3f))
            }
            filteredList.forEach { currency ->
                val isSelected = currency == selectedCurrency
                DropdownMenuItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isSelected) AppColors.primary.copy(alpha = 0.1f) else Color.Transparent),
                    text = {
                        Text(
                            currency,
                            style = AppTextStyles.drawerItemLabel.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) AppColors.primary else AppColors.contentPrimary
                            )
                        )
                    },
                    onClick = {
                        onCurrencySelected(currency)
                        expanded = false
                        query = ""
                    },
                    colors = MenuItemColors(
                        textColor = AppColors.contentPrimary,
                        leadingIconColor = AppColors.contentSecondary,
                        trailingIconColor = AppColors.primary,
                        disabledTextColor = AppColors.contentSecondary.copy(alpha = 0.4f),
                        disabledLeadingIconColor = AppColors.contentSecondary.copy(alpha = 0.4f),
                        disabledTrailingIconColor = AppColors.contentSecondary.copy(alpha = 0.4f)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
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
