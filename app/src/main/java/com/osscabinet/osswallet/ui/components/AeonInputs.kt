package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
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
            modifier = Modifier.widthIn(min = 160.dp)
        ) {
            if (baseList.size > 6) {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search currencies...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
                HorizontalDivider()
            }
            filteredList.forEach { currency ->
                DropdownMenuItem(
                    text = { Text(currency) },
                    onClick = {
                        onCurrencySelected(currency)
                        expanded = false
                        query = ""
                    },
                    trailingIcon = if (currency == selectedCurrency) {
                        {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = AppColors.primary
                            )
                        }
                    } else null
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
