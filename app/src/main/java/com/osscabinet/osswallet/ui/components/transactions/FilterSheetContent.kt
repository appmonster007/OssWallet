package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.data.local.entity.CategoryEntity
import com.osscabinet.ossWallet.data.local.entity.FilterEntity
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.toDomain
import com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheetContent(
    filter: TransactionFilter,
    accounts: List<Account>,
    allCategories: List<CategoryEntity>,
    savedFilters: List<FilterEntity> = emptyList(),
    onSetDateRange: (Long?, Long?) -> Unit,
    onApplyFilter: (TransactionFilter) -> Unit,
    onToggleExpand: () -> Unit,
    onClose: () -> Unit,
    isExpanded: Boolean,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    showAccountFilter: Boolean = true,
    // Context-aware parameters
    initialFilterContext: RecordsFilterContext = RecordsFilterContext.None,
    accountNames: Map<String, String> = emptyMap(),
    onResetToInitial: (() -> Unit)? = null
) {
    var localFilter by remember { mutableStateOf(filter) }
    var selectedFilterId by remember { mutableStateOf<Long?>(null) }
    var filtersExpanded by remember { mutableStateOf(false) }

    // Determine pre-applied filters from context
    val preAppliedAccountId = when (initialFilterContext) {
        is RecordsFilterContext.ByAccount,
        is RecordsFilterContext.ByAccountAndType,
        is RecordsFilterContext.ByAccountAndCurrency -> {
            (initialFilterContext as? RecordsFilterContext.ByAccount)?.accountId
                ?: (initialFilterContext as? RecordsFilterContext.ByAccountAndType)?.accountId
                ?: (initialFilterContext as? RecordsFilterContext.ByAccountAndCurrency)?.accountId
        }
        else -> null
    }

    val preAppliedIsIncome = when (initialFilterContext) {
        is RecordsFilterContext.ByAccountAndType -> (initialFilterContext as RecordsFilterContext.ByAccountAndType).isIncome
        else -> null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Sticky: Handle/Selector at top
            item {
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
            }

            // Context indicator header (if context is not None)
            if (initialFilterContext != RecordsFilterContext.None) {
                item {
                    ContextIndicator(
                        context = initialFilterContext,
                        accountNames = accountNames,
                        onReset = {
                            // Call the ViewModel's reset method if available
                            onResetToInitial?.invoke()
                            // Reset to None (All) - clear all filters
                            localFilter = TransactionFilter()
                            onApplyFilter(localFilter)
                        }
                    )
                }
                item { HorizontalDivider() }
            }

            // Active filters summary (collapsible)
            item {
                ActiveFiltersSummary(
                    filter = localFilter,
                    preAppliedAccountId = preAppliedAccountId,
                    preAppliedIsIncome = preAppliedIsIncome,
                    accountNames = accountNames,
                    categoryMap = allCategories.associateBy { it.id },
                    onRemoveFilter = { field, value ->
                        removeFilter(localFilter, field, value) { updatedFilter ->
                            localFilter = updatedFilter
                            selectedFilterId = null // Clear preset when manually modified
                        }
                    }
                )
            }

            item { HorizontalDivider() }

            // Filter fields content
            item {
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
                            onClick = {
                                // Call the ViewModel's reset method if available
                                onResetToInitial?.invoke()
                                localFilter = TransactionFilter()
                                selectedFilterId = null
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Reset All", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    // Saved Filters Dropdown
                    if (savedFilters.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "FILTER PRESET",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            ExposedDropdownMenuBox(
                                expanded = filtersExpanded,
                                onExpandedChange = { filtersExpanded = !filtersExpanded },
                                modifier = Modifier.fillMaxWidth()
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
                                        if (selectedFilterId != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    ),
                                    onClick = { filtersExpanded = !filtersExpanded }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        val selectedName = savedFilters.find { it.id == selectedFilterId }?.name ?: "Select a preset..."
                                        Text(
                                            text = selectedName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (selectedFilterId != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                        Icon(
                                            Icons.Default.ArrowDropDown,
                                            null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                ExposedDropdownMenu(
                                    expanded = filtersExpanded,
                                    onDismissRequest = { filtersExpanded = false },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                ) {
                                    savedFilters.forEach { savedFilter ->
                                        DropdownMenuItem(
                                            text = { Text(savedFilter.name) },
                                            onClick = {
                                                localFilter = savedFilter.toDomain()
                                                selectedFilterId = savedFilter.id
                                                filtersExpanded = false
                                            },
                                            trailingIcon = if (selectedFilterId == savedFilter.id) {
                                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
                                            } else null
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Shared Filter Fields
                    FilterFields(
                        filter = localFilter,
                        onFilterChange = {
                            localFilter = it
                            // Clear preset selection when a field is manually modified
                            selectedFilterId = null
                        },
                        accounts = accounts,
                        allCategories = allCategories,
                        showAccountFilter = showAccountFilter,
                        preAppliedAccountId = preAppliedAccountId,
                        preAppliedIsIncome = preAppliedIsIncome
                    )

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
                            containerColor = Color(0xFF4A5D88),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Apply Filters", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

@Composable
fun ContextIndicator(
    context: RecordsFilterContext,
    accountNames: Map<String, String>,
    onReset: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Applied filter",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = getContextDisplayName(context, accountNames),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            TextButton(onClick = onReset) {
                Text("Reset")
            }
        }

        if (expanded) {
            Column(
                modifier = Modifier
                    .padding(start = 24.dp, top = 8.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                when (context) {
                    is RecordsFilterContext.ByAccount -> {
                        ContextFilterField(
                            label = "Account",
                            value = accountNames[context.accountId] ?: context.accountId
                        )
                    }
                    is RecordsFilterContext.ByAccountAndType -> {
                        ContextFilterField(
                            label = "Account",
                            value = accountNames[context.accountId] ?: context.accountId
                        )
                        ContextFilterField(
                            label = "Type",
                            value = if (context.isIncome) "Income" else "Expense"
                        )
                    }
                    is RecordsFilterContext.ByAccountAndCurrency -> {
                        ContextFilterField(
                            label = "Account",
                            value = accountNames[context.accountId] ?: context.accountId
                        )
                        ContextFilterField(
                            label = "Currency",
                            value = context.currency
                        )
                    }
                    is RecordsFilterContext.Custom -> {
                        if (context.accountIds.isNotEmpty()) {
                            ContextFilterField(
                                label = "Accounts",
                                value = context.accountIds.map { accountNames[it] ?: it }.joinToString(", ")
                            )
                        }
                        if (context.isIncome != null) {
                            ContextFilterField(
                                label = "Type",
                                value = if (context.isIncome) "Income" else "Expense"
                            )
                        }
                        if (context.startDate != null || context.endDate != null) {
                            ContextFilterField(
                                label = "Date Range",
                                value = formatDateRange(context.startDate, context.endDate)
                            )
                        }
                        if (context.categoryIds.isNotEmpty()) {
                            ContextFilterField(
                                label = "Categories",
                                value = context.categoryIds.joinToString(", ")
                            )
                        }
                        if (context.currencies.isNotEmpty()) {
                            ContextFilterField(
                                label = "Currencies",
                                value = context.currencies.joinToString(", ")
                            )
                        }
                        if (context.paymentMethods.isNotEmpty()) {
                            ContextFilterField(
                                label = "Payment Methods",
                                value = context.paymentMethods.joinToString(", ")
                            )
                        }
                        if (context.queryNote != null) {
                            ContextFilterField(
                                label = "Note",
                                value = context.queryNote
                            )
                        }
                        if (context.queryPayee != null) {
                            ContextFilterField(
                                label = "Payee",
                                value = context.queryPayee
                            )
                        }
                        if (context.minAmount != null || context.maxAmount != null) {
                            val range = buildString {
                                if (context.minAmount != null) append("≥${context.minAmount}")
                                if (context.minAmount != null && context.maxAmount != null) append(" - ")
                                if (context.maxAmount != null) append("≤${context.maxAmount}")
                            }
                            ContextFilterField(
                                label = "Amount",
                                value = range
                            )
                        }
                    }
                    is RecordsFilterContext.None -> {
                        ContextFilterField(
                            label = "Filter",
                            value = "All transactions"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContextFilterField(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun getContextDisplayName(context: RecordsFilterContext, accountNames: Map<String, String>): String {
    return when (context) {
        is RecordsFilterContext.None -> "All"
        is RecordsFilterContext.ByAccount -> "Account: ${accountNames[context.accountId] ?: context.accountId}"
        is RecordsFilterContext.ByAccountAndType ->
            "Account: ${accountNames[context.accountId] ?: context.accountId} (${if (context.isIncome) "Income" else "Expense"})"
        is RecordsFilterContext.ByAccountAndCurrency ->
            "Account: ${accountNames[context.accountId] ?: context.accountId} (${context.currency})"
        is RecordsFilterContext.Custom -> "Custom"
    }
}

@Composable
fun ActiveFiltersSummary(
    filter: TransactionFilter,
    preAppliedAccountId: String?,
    preAppliedIsIncome: Boolean?,
    accountNames: Map<String, String>,
    categoryMap: Map<Long, CategoryEntity>,
    onRemoveFilter: (String, Any?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterAlt,
                    contentDescription = "Active filters",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "${countActiveFilters(filter)} filters active",
                    style = MaterialTheme.typography.titleSmall
                )
            }
            IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }
        }

        if (expanded) {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Account filters
                if (filter.accountIds.isNotEmpty()) {
                    FilterGroup(
                        label = "Accounts",
                        chips = filter.accountIds.map { accountId ->
                            FilterChipData(
                                label = accountNames[accountId] ?: accountId,
                                isPreApplied = preAppliedAccountId == accountId,
                                onRemove = { onRemoveFilter("accountIds", accountId) }
                            )
                        }
                    )
                }

                // Type filter
                if (filter.isIncome != null) {
                    FilterGroup(
                        label = "Type",
                        chips = listOf(
                            FilterChipData(
                                label = if (filter.isIncome == true) "Income" else "Expense",
                                isPreApplied = preAppliedIsIncome == filter.isIncome,
                                onRemove = { onRemoveFilter("isIncome", null) }
                            )
                        )
                    )
                }

                // Date range filter
                if (filter.startDate != null || filter.endDate != null) {
                    FilterGroup(
                        label = "Date Range",
                        chips = listOf(
                            FilterChipData(
                                label = formatDateRange(filter.startDate, filter.endDate),
                                isPreApplied = false,
                                onRemove = { onRemoveFilter("dateRange", null) }
                            )
                        )
                    )
                }

                // Category filters
                if (filter.categoryIds.isNotEmpty()) {
                    FilterGroup(
                        label = "Categories",
                        chips = filter.categoryIds.map { categoryId ->
                            FilterChipData(
                                label = categoryMap[categoryId]?.name ?: "Category: $categoryId",
                                isPreApplied = false,
                                onRemove = { onRemoveFilter("categoryIds", categoryId) }
                            )
                        }
                    )
                }

                // Currency filters
                if (filter.currencies.isNotEmpty()) {
                    FilterGroup(
                        label = "Currencies",
                        chips = filter.currencies.map { currency ->
                            FilterChipData(
                                label = currency,
                                isPreApplied = false,
                                onRemove = { onRemoveFilter("currencies", currency) }
                            )
                        }
                    )
                }

                // Payment method filters
                if (filter.paymentMethods.isNotEmpty()) {
                    FilterGroup(
                        label = "Payment Methods",
                        chips = filter.paymentMethods.map { method ->
                            FilterChipData(
                                label = method,
                                isPreApplied = false,
                                onRemove = { onRemoveFilter("paymentMethods", method) }
                            )
                        }
                    )
                }

                // Note filter
                if (!filter.queryNote.isNullOrBlank()) {
                    FilterGroup(
                        label = "Note",
                        chips = listOf(
                            FilterChipData(
                                label = filter.queryNote,
                                isPreApplied = false,
                                onRemove = { onRemoveFilter("queryNote", null) }
                            )
                        )
                    )
                }

                // Payee filter
                if (!filter.queryPayee.isNullOrBlank()) {
                    FilterGroup(
                        label = "Payee",
                        chips = listOf(
                            FilterChipData(
                                label = filter.queryPayee,
                                isPreApplied = false,
                                onRemove = { onRemoveFilter("queryPayee", null) }
                            )
                        )
                    )
                }

                // Amount range filters
                if (filter.minAmount != null || filter.maxAmount != null) {
                    val range = buildString {
                        if (filter.minAmount != null) append("≥${filter.minAmount}")
                        if (filter.minAmount != null && filter.maxAmount != null) append(" - ")
                        if (filter.maxAmount != null) append("≤${filter.maxAmount}")
                    }
                    FilterGroup(
                        label = "Amount",
                        chips = listOf(
                            FilterChipData(
                                label = range,
                                isPreApplied = false,
                                onRemove = { onRemoveFilter("amountRange", null) }
                            )
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterGroup(
    label: String,
    chips: List<FilterChipData>
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        chips.forEach { chipData ->
            ActiveFilterChip(
                label = chipData.label,
                isPreApplied = chipData.isPreApplied,
                onRemove = chipData.onRemove
            )
        }
    }
}

private data class FilterChipData(
    val label: String,
    val isPreApplied: Boolean,
    val onRemove: () -> Unit
)

@Composable
fun ActiveFilterChip(
    label: String,
    isPreApplied: Boolean,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isPreApplied) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Pre-applied",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove filter",
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

fun countActiveFilters(filter: TransactionFilter): Int {
    var count = 0
    count += filter.accountIds.size
    if (filter.isIncome != null) count++
    count += filter.categoryIds.size
    count += filter.currencies.size
    if (filter.startDate != null || filter.endDate != null) count++
    count += filter.paymentMethods.size
    if (!filter.queryNote.isNullOrBlank()) count++
    if (!filter.queryPayee.isNullOrBlank()) count++
    if (filter.minAmount != null || filter.maxAmount != null) count++
    return count
}

fun removeFilter(
    filter: TransactionFilter,
    field: String,
    value: Any?,
    onUpdateFilter: (TransactionFilter) -> Unit
) {
    val updatedFilter = when (field) {
        "accountIds" -> filter.copy(accountIds = filter.accountIds - (value as String))
        "isIncome" -> filter.copy(isIncome = null)
        "dateRange" -> filter.copy(startDate = null, endDate = null)
        "categoryIds" -> filter.copy(categoryIds = filter.categoryIds - (value as Long))
        "currencies" -> filter.copy(currencies = filter.currencies - (value as String))
        "paymentMethods" -> filter.copy(paymentMethods = filter.paymentMethods - (value as String))
        "queryNote" -> filter.copy(queryNote = null)
        "queryPayee" -> filter.copy(queryPayee = null)
        "amountRange" -> filter.copy(minAmount = null, maxAmount = null)
        else -> filter
    }
    onUpdateFilter(updatedFilter)
}

private fun formatDateRange(startDate: Long?, endDate: Long?): String {
    val start = startDate?.let { java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(java.util.Date(it)) } ?: "Start"
    val end = endDate?.let { java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(java.util.Date(it)) } ?: "End"
    return "$start - $end"
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
