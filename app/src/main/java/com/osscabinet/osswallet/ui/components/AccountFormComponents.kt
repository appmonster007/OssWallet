package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.components.AeonFlowSectionHeader
import com.osscabinet.ossWallet.ui.util.isDark
import java.util.Currency.getAvailableCurrencies

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CurrencyManagementSection(
    primaryCurrency: String,
    onPrimaryCurrencyChange: (String) -> Unit,
    allowedCurrencies: Set<String>,
    onAllowedCurrenciesChange: (Set<String>) -> Unit,
    accentColorHex: String,
    isEditMode: Boolean = false,
    currencyUsageMap: Map<String, Boolean> = emptyMap()
) {
    var searchQuery by remember { mutableStateOf("") }
    val commonCurrencies = listOf("USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD", "INR", "SGD", "HKD", "CNY", "BRL", "IDR", "MYR", "THB", "PHP", "VND", "KRW", "RUB", "TRY", "ZAR", "MXN", "ILS")
    
    val allCurrencies = remember {
        getAvailableCurrencies()
            .map { it.currencyCode }
            .distinct()
            .sorted()
    }

    LaunchedEffect(allowedCurrencies) {
        if (allowedCurrencies.isEmpty()) {
            onPrimaryCurrencyChange("")
        } else if (allowedCurrencies.size == 1) {
            onPrimaryCurrencyChange(allowedCurrencies.first())
        } else if (primaryCurrency.isNotEmpty() && !allowedCurrencies.contains(primaryCurrency)) {
            onPrimaryCurrencyChange(allowedCurrencies.first())
        }
    }

    val filteredCurrencies = if (searchQuery.isBlank()) {
        commonCurrencies
    } else {
        allCurrencies.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(40.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            AeonFlowSectionHeader(title = "CURRENCY MANAGEMENT")
            
            // Search Bar
            Surface(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) Text("Search ISO codes...", color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                            innerTextField()
                        }
                    )
                }
            }

            // Top 10 Currencies in 5x2 grid or search results
            val accentColor = try { Color(android.graphics.Color.parseColor(accentColorHex)) } catch (e: Exception) { Color(0xFF004D5F) }

            if (searchQuery.isBlank()) {
                val topCurrencies = commonCurrencies.take(10)
                val otherSelectedCurrencies = allowedCurrencies.filter { !topCurrencies.contains(it) }
                
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Top 10 grid
                    topCurrencies.chunked(5).forEach { rowCurrencies ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowCurrencies.forEach { code ->
                                val isSelected = allowedCurrencies.contains(code)
                                val isPrimary = primaryCurrency == code
                                val hasTransactions = currencyUsageMap[code] ?: false
                                val canToggle = if (isEditMode) !isSelected || (!isPrimary && !hasTransactions) else {
                                    !isSelected || (allowedCurrencies.size > 1)
                                }

                                CurrencyGridItem(
                                    code = code,
                                    isSelected = isSelected,
                                    isPrimary = isPrimary,
                                    canToggle = canToggle,
                                    accentColor = accentColor,
                                    onClick = {
                                        if (isSelected) {
                                            if (isPrimary) onPrimaryCurrencyChange("")
                                            onAllowedCurrenciesChange(allowedCurrencies - code)
                                        } else {
                                            if (allowedCurrencies.isEmpty()) onPrimaryCurrencyChange(code)
                                            onAllowedCurrenciesChange(allowedCurrencies + code)
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Extra rows for other selected currencies
                    if (otherSelectedCurrencies.isNotEmpty()) {
                        otherSelectedCurrencies.chunked(5).forEach { rowCurrencies ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowCurrencies.forEach { code ->
                                    val isPrimary = primaryCurrency == code
                                    val hasTransactions = currencyUsageMap[code] ?: false
                                    val canToggle = if (isEditMode) !isPrimary && !hasTransactions else (allowedCurrencies.size > 1)

                                    CurrencyGridItem(
                                        code = code,
                                        isSelected = true,
                                        isPrimary = isPrimary,
                                        canToggle = canToggle,
                                        accentColor = accentColor,
                                        onClick = {
                                            if (isPrimary) onPrimaryCurrencyChange("")
                                            onAllowedCurrenciesChange(allowedCurrencies - code)
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                // Fill empty spaces to maintain grid alignment
                                repeat(5 - rowCurrencies.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filteredCurrencies.forEach { code ->
                        val isSelected = allowedCurrencies.contains(code)
                        val isPrimary = primaryCurrency == code
                        val hasTransactions = currencyUsageMap[code] ?: false
                        val canToggle = if (isEditMode) !isSelected || (!isPrimary && !hasTransactions) else {
                            !isSelected || (allowedCurrencies.size > 1)
                        }
                        
                        Surface(
                            modifier = Modifier
                                .alpha(if (canToggle) 1f else 0.5f)
                                .clip(CircleShape)
                                .clickable(enabled = canToggle) {
                                    if (isSelected) {
                                        if (isPrimary) onPrimaryCurrencyChange("")
                                        onAllowedCurrenciesChange(allowedCurrencies - code)
                                    } else {
                                        if (allowedCurrencies.isEmpty()) onPrimaryCurrencyChange(code)
                                        onAllowedCurrenciesChange(allowedCurrencies + code)
                                    }
                                },
                            shape = CircleShape,
                            color = when {
                                isPrimary -> accentColor
                                isSelected -> accentColor.copy(alpha = 0.3f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            },
                            contentColor = when {
                                isPrimary -> if (accentColor.isDark()) Color.White else Color.Black
                                isSelected -> MaterialTheme.colorScheme.onSurface
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(code, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                                if (isPrimary) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Rounded.Star, contentDescription = "Primary", modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Default Currency Selection (Moved after allowed currencies)
            if (allowedCurrencies.size > 1) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val isInvalid = primaryCurrency.isBlank()
                    Text(
                        "Primary Currency" + if (isInvalid) " (Required)" else "",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isInvalid) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                    var isExpanded by remember { mutableStateOf(false) }
                    Box {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .border(
                                    width = if (isInvalid) 2.dp else 0.dp,
                                    color = if (isInvalid) MaterialTheme.colorScheme.error else Color.Transparent,
                                    shape = RoundedCornerShape(28.dp)
                                )
                                .clickable { isExpanded = true },
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = if (isInvalid) 2.dp else 0.dp
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    primaryCurrency.ifEmpty { "Select Primary Currency" },
                                    color = if (primaryCurrency.isEmpty()) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (primaryCurrency.isEmpty()) FontWeight.Normal else FontWeight.Bold
                                )
                                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = if (isInvalid) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        DropdownMenu(
                            expanded = isExpanded,
                            onDismissRequest = { isExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.8f).background(MaterialTheme.colorScheme.surface)
                        ) {
                            allowedCurrencies.forEach { code ->
                                DropdownMenuItem(
                                    text = { Text(code, fontWeight = if (code == primaryCurrency) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        onPrimaryCurrencyChange(code)
                                        isExpanded = false
                                    },
                                    leadingIcon = {
                                        if (code == primaryCurrency) Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                )
                            }
                        }
                    }
                }
            } else if (allowedCurrencies.size == 1) {
                // If only one, it's already primary, but we can show a confirmation label
                Text(
                    "Primary Currency: $primaryCurrency",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            if (isEditMode) {
                Text("Note: Currencies with existing transactions cannot be removed.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
fun CurrencyGridItem(
    code: String,
    isSelected: Boolean,
    isPrimary: Boolean,
    canToggle: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .alpha(if (canToggle) 1f else 0.5f)
            .clip(CircleShape)
            .clickable(enabled = canToggle) { onClick() },
        shape = CircleShape,
        color = when {
            isPrimary -> accentColor
            isSelected -> accentColor.copy(alpha = 0.3f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        contentColor = when {
            isPrimary -> if (accentColor.isDark()) Color.White else Color.Black
            isSelected -> MaterialTheme.colorScheme.onSurface
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    code,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
                if (isPrimary) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Rounded.Star, contentDescription = "Primary", modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}

