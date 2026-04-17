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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.data.local.entity.CategoryEntity
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheetContent(
    filter: TransactionFilter,
    accounts: List<Account>,
    allCategories: List<CategoryEntity>,
    onSetDateRange: (Long?, Long?) -> Unit,
    onApplyFilter: (TransactionFilter) -> Unit,
    onToggleExpand: () -> Unit,
    onClose: () -> Unit,
    isExpanded: Boolean,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainer
) {
    var localFilter by remember { mutableStateOf(filter) }
    var showWarrantyStartPicker by remember { mutableStateOf(false) }
    var showWarrantyEndPicker by remember { mutableStateOf(false) }

    if (showWarrantyStartPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = localFilter.warrantyExpirationStart)
        DatePickerDialog(
            onDismissRequest = { showWarrantyStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    localFilter = localFilter.copy(warrantyExpirationStart = datePickerState.selectedDateMillis)
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
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = localFilter.warrantyExpirationEnd)
        DatePickerDialog(
            onDismissRequest = { showWarrantyEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    localFilter = localFilter.copy(warrantyExpirationEnd = datePickerState.selectedDateMillis)
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp)
    ) {
        // Handle/Selector at top
        AeonPeriodSelector(
            filter = localFilter,
            onSetDateRange = { start, end ->
                localFilter = localFilter.copy(startDate = start, endDate = end)
                onSetDateRange(start, end)
            },
            onClick = onToggleExpand,
            isExpanded = isExpanded,
            showArcTab = true,
            backgroundColor = backgroundColor
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Title and Reset
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Filter Transactions",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                TextButton(
                    onClick = { localFilter = TransactionFilter() },
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text("Reset All", style = MaterialTheme.typography.labelLarge)
                }
            }

            // Search (Note/Payee) - Now 1st Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CompactTextField(
                    value = localFilter.queryNote ?: "",
                    onValueChange = { localFilter = localFilter.copy(queryNote = it.ifBlank { null }) },
                    placeholder = "Note...",
                    modifier = Modifier.weight(1f),
                    leadingIcon = Icons.Default.Search
                )
                CompactTextField(
                    value = localFilter.queryPayee ?: "",
                    onValueChange = { localFilter = localFilter.copy(queryPayee = it.ifBlank { null }) },
                    placeholder = "Payee...",
                    modifier = Modifier.weight(1f),
                    leadingIcon = Icons.Default.Search
                )
            }

            // Transaction Type (Segmented Buttons)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "TYPE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val options = listOf("All", "Income", "Expense")
                    options.forEachIndexed { index, label ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                            onClick = {
                                localFilter = localFilter.copy(
                                    isIncome = when (index) {
                                        1 -> true
                                        2 -> false
                                        else -> null
                                    }
                                )
                            },
                            selected = when (index) {
                                1 -> localFilter.isIncome == true
                                2 -> localFilter.isIncome == false
                                else -> localFilter.isIncome == null
                            },
                            icon = {
                                when (index) {
                                    0 -> if (localFilter.isIncome == null) Icon(Icons.Default.Check, null, Modifier.size(18.dp))
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

            // Accounts & Categories (Dropdowns in a Row)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MultiSelectDropdown(
                    label = "Accounts",
                    selectedCount = localFilter.accountIds.size,
                    modifier = Modifier.weight(1f),
                    onClear = { localFilter = localFilter.copy(accountIds = emptyList()) }
                ) {
                    accounts.forEach { account ->
                        val isSelected = account.id in localFilter.accountIds
                        DropdownMenuItem(
                            text = { Text(account.name) },
                            onClick = {
                                val current = localFilter.accountIds.toMutableList()
                                if (isSelected) current.remove(account.id) else current.add(account.id)
                                localFilter = localFilter.copy(accountIds = current)
                            },
                            leadingIcon = {
                                Checkbox(checked = isSelected, onCheckedChange = null)
                            }
                        )
                    }
                }

                MultiSelectDropdown(
                    label = "Categories",
                    selectedCount = localFilter.categoryIds.size,
                    modifier = Modifier.weight(1f),
                    onClear = { localFilter = localFilter.copy(categoryIds = emptyList()) }
                ) {
                    allCategories.forEach { category ->
                        val isSelected = category.id in localFilter.categoryIds
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = {
                                val current = localFilter.categoryIds.toMutableList()
                                if (isSelected) current.remove(category.id) else current.add(category.id)
                                localFilter = localFilter.copy(categoryIds = current)
                            },
                            leadingIcon = {
                                Checkbox(checked = isSelected, onCheckedChange = null)
                            }
                        )
                    }
                }
            }

            // Payment Method
            val paymentMethods = listOf("Cash", "Credit Card", "Bank Transfer", "Debit Card", "Other")
            MultiSelectDropdown(
                label = "Payment Methods",
                selectedCount = localFilter.paymentMethods.size,
                modifier = Modifier.fillMaxWidth(),
                onClear = { localFilter = localFilter.copy(paymentMethods = emptyList()) }
            ) {
                paymentMethods.forEach { method ->
                    val isSelected = method in localFilter.paymentMethods
                    DropdownMenuItem(
                        text = { Text(method) },
                        onClick = {
                            val current = localFilter.paymentMethods.toMutableList()
                            if (isSelected) current.remove(method) else current.add(method)
                            localFilter = localFilter.copy(paymentMethods = current)
                        },
                        leadingIcon = {
                            Checkbox(checked = isSelected, onCheckedChange = null)
                        }
                    )
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
                        value = localFilter.minAmount?.toString() ?: "",
                        onValueChange = { localFilter = localFilter.copy(minAmount = it.ifBlank { null }?.toBigDecimalOrNull()) },
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
                        value = localFilter.maxAmount?.toString() ?: "",
                        onValueChange = { localFilter = localFilter.copy(maxAmount = it.ifBlank { null }?.toBigDecimalOrNull()) },
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
                            value = localFilter.warrantyExpirationStart?.let {
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
                            value = localFilter.warrantyExpirationEnd?.let {
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

            // Apply Button
            Button(
                onClick = {
                    onApplyFilter(localFilter)
                    onClose()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4A5D88), // Match the dark blue in the image
                    contentColor = Color.White
                )
            ) {
                Text("Apply Filters", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
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

@Composable
fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
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

@Preview(showBackground = true)
@Composable
fun FilterSheetContentPreview() {
    val sampleAccounts = listOf(
        Account(
            id = "1",
            name = "Checking",
            balance = BigDecimal("1000.00"),
            currencyCode = "USD",
            structureType = "Personal",
            colorHex = "#FF0000",
            icon = "account_balance"
        ),
        Account(
            id = "2",
            name = "Savings",
            balance = BigDecimal("5000.00"),
            currencyCode = "USD",
            structureType = "Personal",
            colorHex = "#00FF00",
            icon = "savings"
        )
    )
    val sampleCategories = listOf(
        CategoryEntity(id = 1, name = "Food", iconName = "restaurant", colorHex = 0xFFFF0000.toInt()),
        CategoryEntity(id = 2, name = "Transport", iconName = "directions_bus", colorHex = 0xFF00FF00.toInt()),
        CategoryEntity(id = 3, name = "Shopping", iconName = "shopping_cart", colorHex = 0xFF0000FF.toInt())
    )

    OssWalletTheme {
        Surface {
            FilterSheetContent(
                filter = TransactionFilter(),
                accounts = sampleAccounts,
                allCategories = sampleCategories,
                onSetDateRange = { _, _ -> },
                onApplyFilter = {},
                onToggleExpand = {},
                onClose = {},
                isExpanded = false
            )
        }
    }
}
