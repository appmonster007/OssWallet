package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.tooling.preview.Preview
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
    allCategories: List<CategoryEntity>,
    showAccountFilter: Boolean = true,
    // Context-aware parameters
    preAppliedAccountId: String? = null,
    preAppliedIsIncome: Boolean? = null,
    // Unfiltered data for dropdowns
    allAvailableCurrencies: List<String> = emptyList()
) {
    var showWarrantyStartPicker by remember { mutableStateOf(false) }
    var showWarrantyEndPicker by remember { mutableStateOf(false) }
    var activeDialog by remember { mutableStateOf<String?>(null) }

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

    // Dialogs for multi-selection
    when (activeDialog) {
        "Accounts" -> {
            MultiSelectionDialog(
                title = "Select Accounts",
                items = accounts,
                selectedItems = accounts.filter { it.id in filter.accountIds },
                onDismissRequest = { activeDialog = null },
                onConfirm = { selected ->
                    onFilterChange(filter.copy(accountIds = selected.map { it.id }))
                    activeDialog = null
                },
                itemLabel = { it.name }
            )
        }
        "Categories" -> {
            MultiSelectionDialog(
                title = "Select Categories",
                items = allCategories,
                selectedItems = allCategories.filter { it.id in filter.categoryIds },
                onDismissRequest = { activeDialog = null },
                onConfirm = { selected ->
                    onFilterChange(filter.copy(categoryIds = selected.map { it.id }))
                    activeDialog = null
                },
                itemLabel = { it.name }
            )
        }
        "Currencies" -> {
            val availableCurrencies = if (allAvailableCurrencies.isNotEmpty()) {
                allAvailableCurrencies
            } else {
                accounts.map { it.currencyCode }.distinct().sorted()
            }
            MultiSelectionDialog(
                title = "Select Currencies",
                items = availableCurrencies,
                selectedItems = filter.currencies,
                onDismissRequest = { activeDialog = null },
                onConfirm = { selected ->
                    onFilterChange(filter.copy(currencies = selected))
                    activeDialog = null
                },
                itemLabel = { it }
            )
        }
        "PaymentMethods" -> {
            val paymentMethods = listOf("Cash", "Credit Card", "Bank Transfer", "Debit Card", "Other")
            MultiSelectionDialog(
                title = "Select Payment Methods",
                items = paymentMethods,
                selectedItems = filter.paymentMethods,
                onDismissRequest = { activeDialog = null },
                onConfirm = { selected ->
                    onFilterChange(filter.copy(paymentMethods = selected))
                    activeDialog = null
                },
                itemLabel = { it }
            )
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
                    val isPreApplied = when (index) {
                        1 -> preAppliedIsIncome == true
                        2 -> preAppliedIsIncome == false
                        else -> preAppliedIsIncome == null
                    }
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
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                // Show pre-applied indicator if type was pre-applied
                                if (isPreApplied && filter.isIncome != null) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Pre-applied",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                                when (index) {
                                    0 -> if (filter.isIncome == null) Icon(Icons.Default.Check, null, Modifier.size(18.dp))
                                    1 -> Icon(Icons.AutoMirrored.Filled.TrendingUp, null, Modifier.size(18.dp))
                                    2 -> Icon(Icons.AutoMirrored.Filled.TrendingDown, null, Modifier.size(18.dp))
                                }
                            }
                        }
                    ) {
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        // Collection Filters
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "FILTERS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            MultiChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val totalButtons = if (showAccountFilter) 4 else 3
                var currentIndex = 0

                if (showAccountFilter) {
                    SegmentedButton(
                        checked = filter.accountIds.isNotEmpty(),
                        onCheckedChange = { activeDialog = "Accounts" },
                        shape = SegmentedButtonDefaults.itemShape(index = currentIndex++, count = totalButtons),
                        icon = {},
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccountBalance, null, modifier = Modifier.size(18.dp))
                                // Show pre-applied indicator if account was pre-applied
                                if (preAppliedAccountId != null && preAppliedAccountId in filter.accountIds) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Pre-applied",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                                if (filter.accountIds.isNotEmpty()) {
                                    Spacer(Modifier.width(4.dp))
                                    Text("${filter.accountIds.size}", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    )
                }

                SegmentedButton(
                    checked = filter.categoryIds.isNotEmpty(),
                    onCheckedChange = { activeDialog = "Categories" },
                    shape = SegmentedButtonDefaults.itemShape(index = currentIndex++, count = totalButtons),
                    icon = {},
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Category, null, modifier = Modifier.size(18.dp))
                            if (filter.categoryIds.isNotEmpty()) {
                                Spacer(Modifier.width(4.dp))
                                Text("${filter.categoryIds.size}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                )

                SegmentedButton(
                    checked = filter.currencies.isNotEmpty(),
                    onCheckedChange = { activeDialog = "Currencies" },
                    shape = SegmentedButtonDefaults.itemShape(index = currentIndex++, count = totalButtons),
                    icon = {},
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MonetizationOn, null, modifier = Modifier.size(18.dp))
                            if (filter.currencies.isNotEmpty()) {
                                Spacer(Modifier.width(4.dp))
                                Text("${filter.currencies.size}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                )

                SegmentedButton(
                    checked = filter.paymentMethods.isNotEmpty(),
                    onCheckedChange = { activeDialog = "PaymentMethods" },
                    shape = SegmentedButtonDefaults.itemShape(index = currentIndex++, count = totalButtons),
                    icon = {},
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Payments, null, modifier = Modifier.size(18.dp))
                            if (filter.paymentMethods.isNotEmpty()) {
                                Spacer(Modifier.width(4.dp))
                                Text("${filter.paymentMethods.size}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
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
fun <T> MultiSelectionDialog(
    title: String,
    items: List<T>,
    selectedItems: List<T>,
    onDismissRequest: () -> Unit,
    onConfirm: (List<T>) -> Unit,
    itemLabel: (T) -> String
) {
    var currentSelection by remember { mutableStateOf(selectedItems.toSet()) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredItems = remember(items, searchQuery) {
        if (searchQuery.isBlank()) items
        else items.filter { itemLabel(it).contains(searchQuery, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 32.sp
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val allSelected = items.isNotEmpty() && currentSelection.size == items.size

                    FilterChip(
                        selected = allSelected,
                        onClick = {
                            currentSelection = if (allSelected) emptySet() else items.toSet()
                        },
                        label = { Text(if (allSelected) "Deselect All" else "Select All") },
                        leadingIcon = if (allSelected) {
                            { Icon(Icons.Default.Close, null, Modifier.size(18.dp)) }
                        } else {
                            { Icon(Icons.Default.SelectAll, null, Modifier.size(18.dp)) }
                        },
                        shape = RoundedCornerShape(12.dp)
                    )

                    AssistChip(
                        onClick = {
                            val allItemsSet = items.toSet()
                            currentSelection = allItemsSet.subtract(currentSelection)
                        },
                        label = { Text("Invert") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Flip,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(Modifier.weight(1f))

                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.sizeIn(minWidth = 24.dp, minHeight = 24.dp)
                    ) {
                        Text(
                            text = "${currentSelection.size}",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (items.size > 5) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search items...") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, null)
                                }
                            }
                        } else null,
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                if (filteredItems.isEmpty() && searchQuery.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Text(
                            "No results for \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(filteredItems) { item ->
                        val isSelected = item in currentSelection
                        Surface(
                            onClick = {
                                currentSelection = if (isSelected) {
                                    currentSelection - item
                                } else {
                                    currentSelection + item
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            else androidx.compose.ui.graphics.Color.Transparent,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            ) else null
                        ) {
                            ListItem(
                                headlineContent = {
                                    Text(
                                        itemLabel(item),
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                leadingContent = {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = null,
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(currentSelection.toList()) },
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Confirm Selection", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text("Cancel", style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun MultiSelectionDialogPreview() {
    MaterialTheme {
        MultiSelectionDialog(
            title = "Select Categories with a very long title that should wrap nicely in this expressive layout",
            items = listOf("Food & Dining", "Transportation", "Shopping", "Entertainment", "Health & Fitness", "Education", "Gifts & Donations", "Investments", "Long Category Name That Might Wrap To Multiple Lines To Test UI Robustness"),
            selectedItems = listOf("Food & Dining", "Shopping"),
            onDismissRequest = {},
            onConfirm = {},
            itemLabel = { it }
        )
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
