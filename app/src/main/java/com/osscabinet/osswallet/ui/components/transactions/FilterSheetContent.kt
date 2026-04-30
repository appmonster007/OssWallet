package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.foundation.ExperimentalFoundationApi
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.data.mapper.toDomain
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FilterSheetContent(
    filter: TransactionFilter,
    accounts: List<Account>,
    allCategories: List<Category>,
    savedFilters: List<Filter> = emptyList(),
    onSetDateRange: (Long?, Long?) -> Unit,
    onApplyFilter: (TransactionFilter) -> Unit,
    onToggleExpand: () -> Unit,
    onClose: () -> Unit,
    isExpanded: Boolean,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    showAccountFilter: Boolean = true,
    accountNames: Map<String, String> = emptyMap(),
    onResetToInitial: (() -> Unit)? = null
) {
    var localFilter by remember { mutableStateOf(filter) }
    var selectedFilterIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var filtersExpanded by remember { mutableStateOf(false) }

    // Debouncing mechanism for instant filter application
    val filterSheetScope = rememberCoroutineScope()
    val debounceJob = remember { mutableStateOf<Job?>(null) }

    fun applyFilterWithDebounce(newFilter: TransactionFilter, debounceMs: Long = 300) {
        debounceJob.value?.cancel()
        debounceJob.value = filterSheetScope.launch {
            delay(debounceMs)
            onApplyFilter(newFilter)
        }
    }

    fun combineFiltersWithOrLogic(filters: List<TransactionFilter>): TransactionFilter {
        if (filters.isEmpty()) return TransactionFilter()
        if (filters.size == 1) return filters[0]

        return TransactionFilter(
            queryNote = filters.mapNotNull { it.queryNote }.takeIf { it.isNotEmpty() }?.joinToString("|"),
            isNoteRegex = filters.any { it.isNoteRegex },
            queryPayee = filters.mapNotNull { it.queryPayee }.takeIf { it.isNotEmpty() }?.joinToString("|"),
            isPayeeRegex = filters.any { it.isPayeeRegex },
            categoryIds = filters.flatMap { it.categoryIds }.distinct(),
            accountIds = filters.flatMap { it.accountIds }.distinct(),
            startDate = filters.mapNotNull { it.startDate }.minOrNull(),
            endDate = filters.mapNotNull { it.endDate }.maxOrNull(),
            paymentMethods = filters.flatMap { it.paymentMethods }.distinct(),
            hasWarranty = if (filters.any { it.hasWarranty == null }) null else filters.any { it.hasWarranty == true },
            warrantyExpirationStart = filters.mapNotNull { it.warrantyExpirationStart }.minOrNull(),
            warrantyExpirationEnd = filters.mapNotNull { it.warrantyExpirationEnd }.maxOrNull(),
            minAmount = filters.mapNotNull { it.minAmount }.minOrNull(),
            maxAmount = filters.mapNotNull { it.maxAmount }.maxOrNull(),
            transactionType = if (filters.mapNotNull { it.transactionType }.distinct().size > 1) null else filters.firstOrNull()?.transactionType,
            currencies = filters.flatMap { it.currencies }.distinct(),
            statuses = filters.flatMap { it.statuses }.distinct()
        )
    }

    // Combine multiple filters from selected presets using TransactionFilter
    fun updateFilterFromSelectedPresets() {
        if (selectedFilterIds.isEmpty()) {
            localFilter = TransactionFilter()
            applyFilterWithDebounce(localFilter, debounceMs = 0)
            return
        }

        // Use the nested filter from the Domain model
        val filters = savedFilters
            .filter { it.id in selectedFilterIds }
            .map { it.filter }

        // Combine all filters using OR logic (union of filters)
        localFilter = combineFiltersWithOrLogic(filters)
        applyFilterWithDebounce(localFilter, debounceMs = 0)
    }

    // Sync localFilter with filter prop when it changes (e.g., context filters applied by ViewModel)
    LaunchedEffect(filter) {
        localFilter = filter
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
            stickyHeader {
                AeonPeriodSelector(
                    filter = localFilter,
                    onSetDateRange = { start, end ->
                        localFilter = localFilter.copy(startDate = start, endDate = end)
                        onSetDateRange(start, end)
                        selectedFilterIds = emptySet()  // Clear presets when manually modified
                        applyFilterWithDebounce(localFilter, debounceMs = 0)  // Immediate apply
                    },
                    onClick = onToggleExpand,
                    isExpanded = isExpanded,
                    showArcTab = true,
                    backgroundColor = backgroundColor
                )
            }

            // Active filters summary (collapsible)
            item {
                ActiveFiltersSummary(
                    filter = localFilter,
                    accountNames = accountNames,
                    categoryMap = allCategories.associateBy { it.id },
                    onRemoveFilter = { field, value ->
                        removeFilter(localFilter, field, value) { updatedFilter ->
                            localFilter = updatedFilter
                            selectedFilterIds = emptySet() // Clear presets when manually modified
                            applyFilterWithDebounce(updatedFilter, debounceMs = 0)  // Immediate apply
                        }
                    },
                    onResetAll = {
                        // Call the ViewModel's reset method if available
                        onResetToInitial?.invoke()
                        // Reset to None (All) - clear all filters
                        localFilter = TransactionFilter()
                        selectedFilterIds = emptySet()
                        applyFilterWithDebounce(localFilter, debounceMs = 0)  // Immediate apply
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
                                selectedFilterIds = emptySet()
                                applyFilterWithDebounce(localFilter, debounceMs = 0)  // Immediate apply
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
                            Box(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (selectedFilterIds.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    ),
                                    onClick = { filtersExpanded = !filtersExpanded }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        val selectedNames = savedFilters.filter { it.id in selectedFilterIds }.map { it.name }
                                        val selectedName = if (selectedNames.isEmpty()) "Select presets..." else selectedNames.joinToString(", ")
                                        Text(
                                            text = selectedName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (selectedFilterIds.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                        Icon(
                                            Icons.Default.ArrowDropDown,
                                            null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = filtersExpanded,
                                    onDismissRequest = { filtersExpanded = false },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                ) {
                                    savedFilters.forEach { savedFilter ->
                                        DropdownMenuItem(
                                            text = { Text(savedFilter.name) },
                                            onClick = {
                                                // Toggle selection
                                                selectedFilterIds = if (savedFilter.id in selectedFilterIds) {
                                                    selectedFilterIds - savedFilter.id
                                                } else {
                                                    selectedFilterIds + savedFilter.id
                                                }
                                                // Update filter from selected presets
                                                updateFilterFromSelectedPresets()
                                                // Don't close dropdown - allow multiple selection
                                            },
                                            trailingIcon = if (savedFilter.id in selectedFilterIds) {
                                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
                                            } else null
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = filtersExpanded,
                                    onDismissRequest = { filtersExpanded = false },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                ) {
                                    savedFilters.forEach { savedFilter ->
                                        DropdownMenuItem(
                                            text = { Text(savedFilter.name) },
                                            onClick = {
                                                // Toggle selection
                                                selectedFilterIds = if (savedFilter.id in selectedFilterIds) {
                                                    selectedFilterIds - savedFilter.id
                                                } else {
                                                    selectedFilterIds + savedFilter.id
                                                }
                                                // Update filter from selected presets
                                                updateFilterFromSelectedPresets()
                                                // Don't close dropdown - allow multiple selection
                                            },
                                            trailingIcon = if (savedFilter.id in selectedFilterIds) {
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
                            selectedFilterIds = emptySet()
                            applyFilterWithDebounce(it, debounceMs = 300)  // Debounced apply
                        },
                        accounts = accounts,
                        allCategories = allCategories,
                        showAccountFilter = showAccountFilter,
                        allAvailableCurrencies = accounts.flatMap { account ->
                            if (account.allowedCurrencies.isEmpty()) {
                                listOf(account.currencyCode)
                            } else {
                                account.allowedCurrencies
                            }
                        }.distinct().sorted(),
                        onImmediateApply = { newFilter, debounceMs ->
                            applyFilterWithDebounce(newFilter, debounceMs)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveFiltersSummary(
    filter: TransactionFilter,
    accountNames: Map<String, String>,
    categoryMap: Map<Long, Category>,
    onRemoveFilter: (String, Any?) -> Unit,
    onResetAll: () -> Unit
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (countActiveFilters(filter) > 0) {
                    TextButton(
                        onClick = onResetAll,
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("Reset", style = MaterialTheme.typography.labelSmall)
                    }
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
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
                                isPreApplied = false,
                                onRemove = { onRemoveFilter("accountIds", accountId) }
                            )
                        }
                    )
                }

                // Type filter
                if (filter.transactionType != null) {
                    FilterGroup(
                        label = "Type",
                        chips = listOf(
                            FilterChipData(
                                label = if (filter.transactionType == "income") "Income" else "Expense",
                                isPreApplied = false,
                                onRemove = { onRemoveFilter("transactionType", null) }
                            )
                        )
                    )
                }

                // Status filter
                if (filter.statuses.isNotEmpty()) {
                    FilterGroup(
                        label = "Status",
                        chips = filter.statuses.map { status ->
                            FilterChipData(
                                label = status.displayName,
                                isPreApplied = false,
                                onRemove = { onRemoveFilter("statuses", status) }
                            )
                        }
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

                // Amount range filter
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

@Preview(showBackground = true)
@Composable
fun FilterSheetContentPreview() {
    val sampleAccounts = listOf(
        Account(
            id = "1",
            name = "Checking",
            currencyCode = "USD",
            structureType = "Personal",
            colorHex = "#FF0000",
            icon = "account_balance",
            allowedCurrencies = listOf("USD")
        ),
        Account(
            id = "2",
            name = "Savings",
            currencyCode = "USD",
            structureType = "Personal",
            colorHex = "#00FF00",
            icon = "savings",
            allowedCurrencies = listOf("USD")
        )
    )
    val sampleCategories = listOf(
        Category(id = 1, name = "Food", iconName = "restaurant", colorHex = 0xFFFF0000.toInt(), isDefault = true, parentId = null),
        Category(id = 2, name = "Transport", iconName = "directions_bus", colorHex = 0xFF00FF00.toInt(), isDefault = true, parentId = null),
        Category(id = 3, name = "Shopping", iconName = "shopping_cart", colorHex = 0xFF0000FF.toInt(), isDefault = true, parentId = null)
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
