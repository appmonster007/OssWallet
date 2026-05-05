package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.ui.components.AeonHierarchicalHybridSelector
import com.osscabinet.ossWallet.ui.components.AeonHybridSelector
import com.osscabinet.ossWallet.ui.components.AeonSelectMode
import com.osscabinet.ossWallet.ui.components.AccountIconMap
import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.osscabinet.ossWallet.domain.card.CardFilterRules

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterFields(
    filter: TransactionFilter,
    onFilterChange: (TransactionFilter) -> Unit,
    accounts: List<Account>,
    allCategories: List<Category>,
    showAccountFilter: Boolean = true,
    // Unfiltered data for dropdowns
    allAvailableCurrencies: List<String> = emptyList(),
    // Callback for immediate apply with custom debounce time
    onImmediateApply: ((TransactionFilter, Long) -> Unit)? = null,
    filterRules: CardFilterRules = CardFilterRules.DEFAULT
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
                idMapper = { it.id },
                labelMapper = { it.name },
                iconMapper = { AccountIconMap[it.icon] },
                colorMapper = { Color(android.graphics.Color.parseColor(it.colorHex)) }
            )
        }
        "Categories" -> {
            CategorySelectionDialog(
                categories = allCategories,
                selectedCategoryIds = filter.categoryIds.toSet(),
                onDismissRequest = { activeDialog = null },
                onConfirm = { selectedIds ->
                    onFilterChange(filter.copy(categoryIds = selectedIds.toList()))
                    activeDialog = null
                }
            )
        }
        "Currencies" -> {
            val availableCurrencies = if (allAvailableCurrencies.isNotEmpty()) {
                allAvailableCurrencies
            } else {
                accounts.flatMap { account ->
                    if (account.allowedCurrencies.isEmpty()) {
                        listOf(account.currencyCode)
                    } else {
                        account.allowedCurrencies
                    }
                }.distinct().sorted()
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
                idMapper = { it },
                labelMapper = { it }
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
                idMapper = { it },
                labelMapper = { it }
            )
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        // Search (Note/Payee)
        if (filterRules.allowSearch) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "SEARCH",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CompactTextField(
                        value = filter.queryNote ?: "",
                        onValueChange = {
                            val newFilter = filter.copy(queryNote = it.ifBlank { null })
                            onFilterChange(newFilter)
                            onImmediateApply?.invoke(newFilter, 500)
                        },
                        placeholder = "Note...",
                        modifier = Modifier.weight(1f),
                        leadingIcon = Icons.Default.Search
                    )
                    CompactTextField(
                        value = filter.queryPayee ?: "",
                        onValueChange = {
                            val newFilter = filter.copy(queryPayee = it.ifBlank { null })
                            onFilterChange(newFilter)
                            onImmediateApply?.invoke(newFilter, 500)
                        },
                        placeholder = "Payee...",
                        modifier = Modifier.weight(1f),
                        leadingIcon = Icons.Default.Search
                    )
                }
            }
        }

        // Primary Context Grid (Type and Accounts/Categories)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "CONTEXT",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
            
            // 2-Column Grid for Primary Dimensions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Type Card
                ContextCard(
                    title = "Type",
                    value = when (filter.transactionType) {
                        "income" -> "Income"
                        "expense" -> "Expense"
                        else -> "All Types"
                    },
                    icon = when (filter.transactionType) {
                        "income" -> Icons.AutoMirrored.Filled.TrendingUp
                        "expense" -> Icons.AutoMirrored.Filled.TrendingDown
                        else -> Icons.Default.FilterList
                    },
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val nextType = when (filter.transactionType) {
                            null -> "expense"
                            "expense" -> "income"
                            else -> null
                        }
                        onFilterChange(filter.copy(transactionType = nextType))
                    }
                )

                // Accounts Card
                if (filterRules.allowAccountSelection && showAccountFilter) {
                    ContextCard(
                        title = "Accounts",
                        value = if (filter.accountIds.isEmpty()) "All" else "${filter.accountIds.size} Selected",
                        icon = Icons.Default.AccountBalance,
                        modifier = Modifier.weight(1f),
                        onClick = { activeDialog = "Accounts" }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Categories Card
                if (filterRules.allowCategorySelection) {
                    ContextCard(
                        title = "Categories",
                        value = if (filter.categoryIds.isEmpty()) "All" else "${filter.categoryIds.size} Selected",
                        icon = Icons.Default.Category,
                        modifier = Modifier.weight(1f),
                        onClick = { activeDialog = "Categories" }
                    )
                }

                // Currency Card
                if (filterRules.allowCurrencySelection) {
                    ContextCard(
                        title = "Currencies",
                        value = if (filter.currencies.isEmpty()) "All" else "${filter.currencies.size} Selected",
                        icon = Icons.Default.MonetizationOn,
                        modifier = Modifier.weight(1f),
                        onClick = { activeDialog = "Currencies" }
                    )
                }
            }
        }

        // Status
        if (filterRules.allowStatusSelection) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "STATUS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
                MultiChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val statusOptions = com.osscabinet.ossWallet.domain.model.TransactionStatus.values()
                    statusOptions.forEachIndexed { index, status ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = statusOptions.size),
                            checked = status in filter.statuses,
                            onCheckedChange = {
                                val newStatuses = if (it) {
                                    filter.statuses + status
                                } else {
                                    filter.statuses - status
                                }
                                onFilterChange(filter.copy(statuses = newStatuses))
                            },
                            icon = {
                                if (status in filter.statuses) {
                                    Icon(Icons.Default.Check, null, Modifier.size(18.dp))
                                } else null
                            }
                        ) {
                            Text(status.displayName, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        // Amount Range
        if (filterRules.allowAmountRange) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "AMOUNT RANGE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactTextField(
                        value = filter.minAmount?.toString() ?: "",
                        onValueChange = { onFilterChange(filter.copy(minAmount = it.ifBlank { null }?.toBigDecimalOrNull())) },
                        placeholder = "Min",
                        modifier = Modifier.weight(1f),
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
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
        }

        // Methods Card (Full Width)
        if (filterRules.allowPaymentMethodSelection) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "PAYMENT METHODS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
                ContextCard(
                    title = "Methods",
                    value = if (filter.paymentMethods.isEmpty()) "Any Method" else filter.paymentMethods.joinToString(", "),
                    icon = Icons.Default.Payments,
                    onClick = { activeDialog = "PaymentMethods" }
                )
            }
        }

        // Warranty Expiry Range
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "WARRANTY EXPIRY",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
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

@Composable
fun ContextCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(84.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    title.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Text(
                    value,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
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
    idMapper: (T) -> String,
    labelMapper: (T) -> String,
    iconMapper: (T) -> ImageVector? = { null },
    colorMapper: (T) -> Color? = { null }
) {
    var currentSelectionIds by remember { mutableStateOf(selectedItems.map(idMapper).toSet()) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredItems = remember(items, searchQuery) {
        if (searchQuery.isBlank()) items
        else items.filter { labelMapper(it).contains(searchQuery, ignoreCase = true) }
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
                    val allSelected = items.isNotEmpty() && currentSelectionIds.size == items.size

                    FilterChip(
                        selected = allSelected,
                        onClick = {
                            currentSelectionIds = if (allSelected) emptySet() else items.map(idMapper).toSet()
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
                            val allItemsSet = items.map(idMapper).toSet()
                            currentSelectionIds = allItemsSet.subtract(currentSelectionIds)
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
                            text = "${currentSelectionIds.size}",
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

                Box(modifier = Modifier.heightIn(max = 450.dp)) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        AeonHybridSelector(
                            items = filteredItems,
                            mode = AeonSelectMode.MULTI,
                            selectedIds = currentSelectionIds,
                            onSelectionChange = { currentSelectionIds = it },
                            idMapper = idMapper,
                            labelMapper = labelMapper,
                            iconMapper = iconMapper,
                            colorMapper = colorMapper
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(items.filter { idMapper(it) in currentSelectionIds }) },
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
            idMapper = { it },
            labelMapper = { it }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelectionDialog(
    categories: List<Category>,
    selectedCategoryIds: Set<Long>,
    onDismissRequest: () -> Unit,
    onConfirm: (Set<Long>) -> Unit
) {
    var tempSelectedIds by remember { mutableStateOf(selectedCategoryIds) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.fillMaxWidth(0.9f),
        title = {
            Text(
                "Select Categories",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                AeonHierarchicalHybridSelector(
                    categories = categories,
                    mode = AeonSelectMode.MULTI,
                    selectedIds = tempSelectedIds,
                    onSelectionChange = { tempSelectedIds = it },
                    modifier = Modifier.weight(1f)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(tempSelectedIds) },
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
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
