package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.data.local.entity.CategoryEntity
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterFields(
    filter: TransactionFilter,
    onFilterChange: (TransactionFilter) -> Unit,
    accounts: List<Account>,
    allCategories: List<CategoryEntity>
) {
    var showWarrantyStartPicker by remember { mutableStateOf(false) }
    var showWarrantyEndPicker by remember { mutableStateOf(false) }

    if (showWarrantyStartPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = filter.warrantyExpirationStart)
        DatePickerDialog(
            onDismissRequest = { showWarrantyStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onFilterChange(filter.copy(warrantyExpirationStart = datePickerState.selectedDateMillis))
                    showWarrantyStartPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showWarrantyStartPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showWarrantyEndPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = filter.warrantyExpirationEnd)
        DatePickerDialog(
            onDismissRequest = { showWarrantyEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onFilterChange(filter.copy(warrantyExpirationEnd = datePickerState.selectedDateMillis))
                    showWarrantyEndPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showWarrantyEndPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Search (Note/Payee)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CompactTextField(
                value = filter.queryNote ?: "",
                onValueChange = { onFilterChange(filter.copy(queryNote = it.ifBlank { null })) },
                placeholder = "Note...",
                modifier = Modifier.weight(1f),
                leadingIcon = Icons.Default.Search
            )
            CompactTextField(
                value = filter.queryPayee ?: "",
                onValueChange = { onFilterChange(filter.copy(queryPayee = it.ifBlank { null })) },
                placeholder = "Payee...",
                modifier = Modifier.weight(1f),
                leadingIcon = Icons.Default.Search
            )
        }

        // Transaction Type
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "TYPE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = listOf("All", "Income", "Expense")
                options.forEachIndexed { index, label ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                        onClick = {
                            onFilterChange(
                                filter.copy(
                                    isIncome = when (index) {
                                        1 -> true
                                        2 -> false
                                        else -> null
                                    }
                                )
                            )
                        },
                        selected = when (index) {
                            1 -> filter.isIncome == true
                            2 -> filter.isIncome == false
                            else -> filter.isIncome == null
                        },
                        icon = {
                            when (index) {
                                0 -> if (filter.isIncome == null) Icon(Icons.Default.Check, null, Modifier.size(18.dp))
                                1 -> Icon(Icons.AutoMirrored.Filled.TrendingUp, null, Modifier.size(18.dp))
                                2 -> Icon(Icons.AutoMirrored.Filled.TrendingDown, null, Modifier.size(18.dp))
                            }
                        }
                    ) {
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        // Accounts & Categories
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MultiSelectDropdown(
                label = "Accounts",
                selectedCount = filter.accountIds.size,
                modifier = Modifier.weight(1f),
                onClear = { onFilterChange(filter.copy(accountIds = emptyList())) }
            ) {
                accounts.forEach { account ->
                    val isSelected = account.id in filter.accountIds
                    DropdownMenuItem(
                        text = { Text(account.name) },
                        onClick = {
                            val current = filter.accountIds.toMutableList()
                            if (isSelected) current.remove(account.id) else current.add(account.id)
                            onFilterChange(filter.copy(accountIds = current))
                        },
                        leadingIcon = { Checkbox(checked = isSelected, onCheckedChange = null) }
                    )
                }
            }

            MultiSelectDropdown(
                label = "Categories",
                selectedCount = filter.categoryIds.size,
                modifier = Modifier.weight(1f),
                onClear = { onFilterChange(filter.copy(categoryIds = emptyList())) }
            ) {
                allCategories.forEach { category ->
                    val isSelected = category.id in filter.categoryIds
                    DropdownMenuItem(
                        text = { Text(category.name) },
                        onClick = {
                            val current = filter.categoryIds.toMutableList()
                            if (isSelected) current.remove(category.id) else current.add(category.id)
                            onFilterChange(filter.copy(categoryIds = current))
                        },
                        leadingIcon = { Checkbox(checked = isSelected, onCheckedChange = null) }
                    )
                }
            }
        }

        // Payment Method & Currency
        val paymentMethods = listOf("Cash", "Credit Card", "Bank Transfer", "Debit Card", "Other")
        val availableCurrencies = accounts.map { it.currencyCode }.distinct().sorted()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MultiSelectDropdown(
                label = "Currencies",
                selectedCount = filter.currencies.size,
                modifier = Modifier.weight(0.4f),
                onClear = { onFilterChange(filter.copy(currencies = emptyList())) }
            ) {
                availableCurrencies.forEach { currency ->
                    val isSelected = currency in filter.currencies
                    DropdownMenuItem(
                        text = { Text(currency) },
                        onClick = {
                            val current = filter.currencies.toMutableList()
                            if (isSelected) current.remove(currency) else current.add(currency)
                            onFilterChange(filter.copy(currencies = current))
                        },
                        leadingIcon = { Checkbox(checked = isSelected, onCheckedChange = null) }
                    )
                }
            }

            MultiSelectDropdown(
                label = "Payment Methods",
                selectedCount = filter.paymentMethods.size,
                modifier = Modifier.weight(0.6f),
                onClear = { onFilterChange(filter.copy(paymentMethods = emptyList())) }
            ) {
                paymentMethods.forEach { method ->
                    val isSelected = method in filter.paymentMethods
                    DropdownMenuItem(
                        text = { Text(method) },
                        onClick = {
                            val current = filter.paymentMethods.toMutableList()
                            if (isSelected) current.remove(method) else current.add(method)
                            onFilterChange(filter.copy(paymentMethods = current))
                        },
                        leadingIcon = { Checkbox(checked = isSelected, onCheckedChange = null) }
                    )
                }
            }
        }

        // Amount Range
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "AMOUNT RANGE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompactTextField(
                    value = filter.minAmount?.toString() ?: "",
                    onValueChange = { onFilterChange(filter.copy(minAmount = it.ifBlank { null }?.toBigDecimalOrNull())) },
                    placeholder = "Min",
                    modifier = Modifier.weight(1f),
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                )
                Text(
                    "-",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyMedium
                )
                CompactTextField(
                    value = filter.maxAmount?.toString() ?: "",
                    onValueChange = { onFilterChange(filter.copy(maxAmount = it.ifBlank { null }?.toBigDecimalOrNull())) },
                    placeholder = "Max",
                    modifier = Modifier.weight(1f),
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                )
            }
        }

        // Warranty Expiry Range
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "WARRANTY EXPIRY",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f).clickable { showWarrantyStartPicker = true }) {
                    CompactTextField(
                        value = filter.warrantyExpirationStart?.let {
                            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(it))
                        } ?: "",
                        onValueChange = { },
                        placeholder = "Start Date",
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = Icons.Default.CalendarMonth,
                        enabled = false
                    )
                }
                Text(
                    "-",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Box(modifier = Modifier.weight(1f).clickable { showWarrantyEndPicker = true }) {
                    CompactTextField(
                        value = filter.warrantyExpirationEnd?.let {
                            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(it))
                        } ?: "",
                        onValueChange = { },
                        placeholder = "End Date",
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = Icons.Default.CalendarMonth,
                        enabled = false
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiSelectDropdown(
    label: String,
    selectedCount: Int,
    modifier: Modifier = Modifier,
    onClear: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (selectedCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            ),
            onClick = { expanded = !expanded }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (selectedCount == 0) label else "$label ($selectedCount)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selectedCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectedCount > 0) {
                        IconButton(
                            onClick = { 
                                onClear()
                                expanded = false
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, null, Modifier.size(16.dp))
                        }
                    }
                    Icon(
                        Icons.Default.ArrowDropDown,
                        null,
                        Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
    enabled: Boolean = true
) {
    Surface(
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(
                    leadingIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                enabled = enabled,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    innerTextField()
                }
            )
            if (value.isNotEmpty() && enabled) {
                IconButton(
                    onClick = { onValueChange("") },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Close, null, Modifier.size(16.dp))
                }
            }
        }
    }
}
