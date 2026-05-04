package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.alpha
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
            .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Tier 1: Expressive Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            onResetToInitial?.invoke()
                            localFilter = TransactionFilter()
                            selectedFilterIds = emptySet()
                            applyFilterWithDebounce(localFilter, debounceMs = 0)
                        }
                    ) {
                        Text(
                            "RESET",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        "FILTERS",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                    )

                    TextButton(onClick = onClose) {
                        Text(
                            "DONE",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Tier 1: Impact (Period Selector) - Sticky
            stickyHeader {
                AeonPeriodSelector(
                    filter = localFilter,
                    onSetDateRange = { start, end ->
                        localFilter = localFilter.copy(startDate = start, endDate = end)
                        onSetDateRange(start, end)
                        selectedFilterIds = emptySet()
                        applyFilterWithDebounce(localFilter, debounceMs = 0)
                    },
                    onClick = onToggleExpand,
                    isExpanded = isExpanded,
                    showArcTab = true,
                    backgroundColor = backgroundColor
                )
            }

            // Tier 1: Active Filters Summary (Dismissible Chips)
            item {
                ActiveFiltersSummary(
                    filter = localFilter,
                    accountNames = accountNames,
                    categoryMap = allCategories.associateBy { it.id },
                    onRemoveFilter = { field, value ->
                        removeFilter(localFilter, field, value) { updatedFilter ->
                            localFilter = updatedFilter
                            selectedFilterIds = emptySet()
                            applyFilterWithDebounce(updatedFilter, debounceMs = 0)
                        }
                    }
                )
            }

            // Tier 2: Presets
            if (savedFilters.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "PRESETS",
                            modifier = Modifier.padding(horizontal = 24.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(items = savedFilters) { savedFilter ->
                                val isSelected = savedFilter.id in selectedFilterIds
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedFilterIds = if (isSelected) {
                                            selectedFilterIds - savedFilter.id
                                        } else {
                                            selectedFilterIds + savedFilter.id
                                        }
                                        updateFilterFromSelectedPresets()
                                    },
                                    label = { Text(savedFilter.name) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        selectedLabelColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 24.dp).alpha(0.1f)) }

            // Tier 3: Filter Fields
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    FilterFields(
                        filter = localFilter,
                        onFilterChange = {
                            localFilter = it
                            selectedFilterIds = emptySet()
                            applyFilterWithDebounce(it, debounceMs = 300)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActiveFiltersSummary(
    filter: TransactionFilter,
    accountNames: Map<String, String>,
    categoryMap: Map<Long, Category>,
    onRemoveFilter: (String, Any?) -> Unit
) {
    val activeCount = countActiveFilters(filter)
    if (activeCount == 0) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "ACTIVE (${activeCount})",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            ),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Account filters
            filter.accountIds.forEach { accountId ->
                ActiveFilterChip(
                    label = accountNames[accountId] ?: accountId,
                    onRemove = { onRemoveFilter("accountIds", accountId) }
                )
            }

            // Type filter
            if (filter.transactionType != null) {
                ActiveFilterChip(
                    label = if (filter.transactionType == "income") "Income" else "Expense",
                    onRemove = { onRemoveFilter("transactionType", null) }
                )
            }

            // Status filter
            filter.statuses.forEach { status ->
                ActiveFilterChip(
                    label = status.displayName,
                    onRemove = { onRemoveFilter("statuses", status) }
                )
            }

            // Date range filter
            if (filter.startDate != null || filter.endDate != null) {
                ActiveFilterChip(
                    label = formatDateRange(filter.startDate, filter.endDate),
                    onRemove = { onRemoveFilter("dateRange", null) }
                )
            }

            // Category filters
            filter.categoryIds.forEach { categoryId ->
                ActiveFilterChip(
                    label = categoryMap[categoryId]?.name ?: "Category: $categoryId",
                    onRemove = { onRemoveFilter("categoryIds", categoryId) }
                )
            }

            // Currency filters
            filter.currencies.forEach { currency ->
                ActiveFilterChip(
                    label = currency,
                    onRemove = { onRemoveFilter("currencies", currency) }
                )
            }

            // Payment method filters
            filter.paymentMethods.forEach { method ->
                ActiveFilterChip(
                    label = method,
                    onRemove = { onRemoveFilter("paymentMethods", method) }
                )
            }

            // Note filter
            if (!filter.queryNote.isNullOrBlank()) {
                ActiveFilterChip(
                    label = "Note: ${filter.queryNote}",
                    onRemove = { onRemoveFilter("queryNote", null) }
                )
            }

            // Payee filter
            if (!filter.queryPayee.isNullOrBlank()) {
                ActiveFilterChip(
                    label = "Payee: ${filter.queryPayee}",
                    onRemove = { onRemoveFilter("queryPayee", null) }
                )
            }

            // Amount range filter
            if (filter.minAmount != null || filter.maxAmount != null) {
                val range = buildString {
                    if (filter.minAmount != null) append("≥${filter.minAmount}")
                    if (filter.minAmount != null && filter.maxAmount != null) append(" - ")
                    if (filter.maxAmount != null) append("≤${filter.maxAmount}")
                }
                ActiveFilterChip(
                    label = range,
                    onRemove = { onRemoveFilter("amountRange", null) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveFilterChip(
    label: String,
    onRemove: () -> Unit
) {
    InputChip(
        selected = true,
        onClick = onRemove,
        label = { Text(label, style = MaterialTheme.typography.bodySmall) },
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove",
                modifier = Modifier.size(16.dp)
            )
        },
        shape = RoundedCornerShape(12.dp),
        colors = InputChipDefaults.inputChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
            selectedTrailingIconColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        border = InputChipDefaults.inputChipBorder(
            enabled = true,
            selected = true,
            borderColor = Color.Transparent,
            selectedBorderColor = Color.Transparent
        )
    )
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
