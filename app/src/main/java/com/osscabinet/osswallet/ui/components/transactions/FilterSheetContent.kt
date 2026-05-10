package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheetContent(
    filter: TransactionFilter,
    accounts: List<Account>,
    allCategories: List<Category>,
    savedFilters: List<Filter> = emptyList(),
    onUpdateTimelineRange: (TimelineRange) -> Unit,
    onApplyFilter: (TransactionFilter) -> Unit,
    onToggleExpand: () -> Unit,
    onClose: () -> Unit,
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

        val filters = savedFilters
            .filter { it.id in selectedFilterIds }
            .map { it.filter }

        localFilter = combineFiltersWithOrLogic(filters)
        applyFilterWithDebounce(localFilter, debounceMs = 0)
    }

    // Sync localFilter with filter prop when it changes
    LaunchedEffect(filter) {
        localFilter = filter
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxSize()
            .background(backgroundColor)
            .verticalScroll(scrollState)
    ) {
        AeonPeriodSelector(
            filter = localFilter,
            onSetDateRange = { range ->
                localFilter = localFilter.copy(timelineRange = range)
                onUpdateTimelineRange(range)
                selectedFilterIds = emptySet()
                applyFilterWithDebounce(localFilter, debounceMs = 0)
            },
            onClick = onToggleExpand,
            showArcTab = false,
            backgroundColor = backgroundColor
        )

        HorizontalDivider()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                        onResetToInitial?.invoke()
                        localFilter = TransactionFilter()
                        selectedFilterIds = emptySet()
                        applyFilterWithDebounce(localFilter, debounceMs = 0)
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text("Reset All", style = MaterialTheme.typography.labelLarge)
                }
            }

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
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (selectedFilterIds.isNotEmpty()) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
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
                                    color = if (selectedFilterIds.isNotEmpty()) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                                Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                        selectedFilterIds = if (savedFilter.id in selectedFilterIds) {
                                            selectedFilterIds - savedFilter.id
                                        } else {
                                            selectedFilterIds + savedFilter.id
                                        }
                                        updateFilterFromSelectedPresets()
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
                    if (account.allowedCurrencies.isEmpty()) listOf(account.currencyCode)
                    else account.allowedCurrencies
                }.distinct().sorted(),
                onImmediateApply = { newFilter, debounceMs ->
                    applyFilterWithDebounce(newFilter, debounceMs)
                }
            )
        }

        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
internal fun FilterSheetDragHandle(
    isExpanded: Boolean,
    onClick: () -> Unit,
    backgroundColor: Color
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "handle_rotation"
    )
    // When fully expanded the sheet top can reach the status bar area.
    // Animate a top padding equal to the status bar height so the visible
    // pull tab always stays below the system status bar.
    val statusBarTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        .plus(64.dp) // title bar height
        .minus(20.dp) // pull tab height
    val topPadding by animateDpAsState(
        targetValue = if (isExpanded) statusBarTopPadding else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "handle_top_padding"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding),
        contentAlignment = Alignment.TopCenter
    ) {
        // Flat bookmark-style pull tab: wide, short, gently rounded top corners only.
        Box(
            modifier = Modifier
                .size(width = 88.dp, height = 20.dp)
                .background(
                    backgroundColor,
                    RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = null,
                modifier = Modifier
                    .size(14.dp)
                    .graphicsLayer { rotationZ = rotation },
                tint = accentColor.copy(alpha = 0.55f)
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
        )
    )
    val sampleCategories = listOf(
        Category(id = 1, name = "Food", iconName = "restaurant", colorHex = 0xFFFF0000.toInt(), isDefault = true, parentId = null)
    )

    OssWalletTheme {
        Surface {
            FilterSheetContent(
                filter = TransactionFilter(),
                accounts = sampleAccounts,
                allCategories = sampleCategories,
                onUpdateTimelineRange = { },
                onApplyFilter = {},
                onToggleExpand = {},
                onClose = {}
            )
        }
    }
}
