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
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.components.AeonTimelinePeriodSelector
import com.osscabinet.ossWallet.ui.components.entry.*
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheetContent(
    filter: TransactionFilter,
    accounts: List<Account>,
    allCategories: List<Category>,
    allTags: List<Tag> = emptyList(),
    allTransactions: List<com.osscabinet.ossWallet.domain.model.Transaction> = emptyList(),
    savedFilters: List<Filter> = emptyList(),
    onFilterEvent: (FilterEvent) -> Unit,
    onToggleExpand: () -> Unit,
    onClose: () -> Unit,
    backgroundColor: Color = AppColors.sheetSurface,
    showAccountFilter: Boolean = true,
    accountNames: Map<String, String> = emptyMap(),
    onAddTag: (String, Int) -> Unit = { _, _ -> }
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
            onFilterEvent(FilterEvent.Apply(newFilter))
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
            categoryIds = filters.flatMap { it.categoryIds }.toSet(),
            accountIds = filters.flatMap { it.accountIds }.toSet(),
            paymentMethods = filters.flatMap { it.paymentMethods }.toSet(),
            hasWarranty = if (filters.any { it.hasWarranty == null }) null else filters.any { it.hasWarranty == true },
            warrantyExpirationStart = filters.mapNotNull { it.warrantyExpirationStart }.minOrNull(),
            warrantyExpirationEnd = filters.mapNotNull { it.warrantyExpirationEnd }.maxOrNull(),
            minAmount = filters.mapNotNull { it.minAmount }.minOrNull(),
            maxAmount = filters.mapNotNull { it.maxAmount }.maxOrNull(),
            transactionTypes = filters.flatMap { it.transactionTypes }.toSet(),
            currencies = filters.flatMap { it.currencies }.toSet(),
            statuses = filters.flatMap { it.statuses }.toSet()
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
            .padding(AeonTheme.dimensions.screenPadding)
    ) {
        AeonTimelinePeriodSelector(
            currentRange = localFilter.timelineRange,
            onRangeSelected = { range ->
                localFilter = localFilter.copy(timelineRange = range)
                onFilterEvent(FilterEvent.UpdateTimeline(range))
                selectedFilterIds = emptySet()
                applyFilterWithDebounce(localFilter, debounceMs = 0)
            },
            swipeEntireComponent = true
        )

        Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingMd))

        HorizontalDivider(
            thickness = 0.5.dp,
            color = AppColors.divider
        )

        Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingMd))

        // Future Transactions Toggle - Moved below timeline as requested
        EntrySwitchRow(
            icon = Icons.Default.Update,
            label = "Show Future Transactions",
            checked = !localFilter.excludeFuture,
            onCheckedChange = { 
                localFilter = localFilter.copy(excludeFuture = !it)
                applyFilterWithDebounce(localFilter, debounceMs = 0)
            }
        )

        Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingLg))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor),
            verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingXl)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Filter Transactions",
                    style = AppTextStyles.screenTitle.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    )
                )
                TextButton(
                    onClick = {
                        localFilter = TransactionFilter()
                        selectedFilterIds = emptySet()
                        onFilterEvent(FilterEvent.Reset)
                    },
                    contentPadding = PaddingValues(horizontal = AeonTheme.dimensions.spacingSm),
                    colors = ButtonDefaults.textButtonColors(contentColor = AppColors.primary)
                ) {
                    Text(
                        "Reset All",
                        style = AppTextStyles.listSectionHeader
                    )
                }
            }

            if (savedFilters.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)) {
                    Text(
                        "FILTER PRESETS",
                        style = AppTextStyles.transactionSwipeAction.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        ),
                        color = AppColors.primary.copy(alpha = 0.7f)
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
                            color = AppColors.cardSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (selectedFilterIds.isNotEmpty()) AppColors.primary
                                else AppColors.divider.copy(alpha = 0.15f)
                            ),
                            onClick = { filtersExpanded = !filtersExpanded }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = AeonTheme.dimensions.spacingLg),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val selectedNames = savedFilters.filter { it.id in selectedFilterIds }.map { it.name }
                                val selectedName = if (selectedNames.isEmpty()) "Select a preset..." else selectedNames.joinToString(", ")
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)) {
                                    Icon(
                                        Icons.Default.Bookmark,
                                        contentDescription = null,
                                        tint = if (selectedFilterIds.isNotEmpty()) AppColors.primary else AppColors.contentSecondary.copy(alpha = 0.4f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = selectedName,
                                        style = AppTextStyles.listItemTitle.copy(
                                            fontWeight = if (selectedFilterIds.isNotEmpty()) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (selectedFilterIds.isNotEmpty()) AppColors.primary
                                        else AppColors.contentSecondary,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    if (filtersExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    null,
                                    tint = AppColors.contentSecondary.copy(alpha = 0.4f)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = filtersExpanded,
                            onDismissRequest = { filtersExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .background(AppColors.cardSurfaceProminent)
                        ) {
                            savedFilters.forEach { savedFilter ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            savedFilter.name,
                                            style = AppTextStyles.listItemTitle.copy(
                                                fontWeight = if (savedFilter.id in selectedFilterIds) FontWeight.Bold else FontWeight.Normal
                                            )
                                        ) 
                                    },
                                    onClick = {
                                        selectedFilterIds = if (savedFilter.id in selectedFilterIds) {
                                            selectedFilterIds - savedFilter.id
                                        } else {
                                            selectedFilterIds + savedFilter.id
                                        }
                                        updateFilterFromSelectedPresets()
                                    },
                                    trailingIcon = if (savedFilter.id in selectedFilterIds) {
                                        { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp), tint = AppColors.primary) }
                                    } else null,
                                    colors = MenuDefaults.itemColors(
                                        textColor = if (savedFilter.id in selectedFilterIds) AppColors.primary else AppColors.contentPrimary
                                    )
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
                allTags = allTags,
                allTransactions = allTransactions,
                showAccountFilter = showAccountFilter,
                allAvailableCurrencies = accounts.flatMap { account ->
                    if (account.allowedCurrencies.isEmpty()) listOf(account.currencyCode)
                    else account.allowedCurrencies
                }.distinct().sorted(),
                onImmediateApply = { newFilter, debounceMs ->
                    applyFilterWithDebounce(newFilter, debounceMs)
                },
                onAddTag = onAddTag
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
    val accentColor = AppColors.primary
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "handle_rotation"
    )
    // When fully expanded the sheet top should reach the status bar area.
    // The top of the pull tab starts at (status bar height - pull tab height)
    // so that the sheet content starts exactly below the status bar.
    val pullTabHeight = 20.dp
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val statusBarTopPadding = (statusBarHeight - pullTabHeight).coerceAtLeast(0.dp)
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
                .size(width = 88.dp, height = pullTabHeight)
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

@Preview(showBackground = true, name = "Filter Sheet - Single Data")
@Composable
fun FilterSheetContentPreview() {
    val sampleAccounts = listOf(
        Account(
            id = "1",
            name = "Checking",
            currencyCode = "USD",
            structureType = AccountStructureType.CHECKING,
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
                onFilterEvent = {},
                onToggleExpand = {},
                onClose = {},
                backgroundColor = AppColors.insetSurface,
            )
        }
    }
}

@Preview(showBackground = true, name = "Filter Sheet - Long Content", heightDp = 1600)
@Composable
fun FilterSheetContentLongPreview() {
    val sampleAccounts = listOf(
        Account(id = "1", name = "Main Checking", currencyCode = "USD", structureType = AccountStructureType.CHECKING, colorHex = "#4CAF50", icon = "account_balance"),
        Account(id = "2", name = "Savings", currencyCode = "USD", structureType = AccountStructureType.SAVINGS, colorHex = "#2196F3", icon = "savings"),
        Account(id = "3", name = "Credit Card", currencyCode = "USD", structureType = AccountStructureType.CREDIT_CARD, colorHex = "#F44336", icon = "credit_card"),
        Account(id = "4", name = "Cash Wallet", currencyCode = "USD", structureType = AccountStructureType.CASH, colorHex = "#FFC107", icon = "payments")
    )
    val sampleCategories = listOf(
        Category(id = 1, name = "Housing", iconName = "home", colorHex = 0xFF2196F3.toInt(), isDefault = true, parentId = null),
        Category(id = 2, name = "Food", iconName = "restaurant", colorHex = 0xFFFF9800.toInt(), isDefault = true, parentId = null),
        Category(id = 3, name = "Transport", iconName = "directions_car", colorHex = 0xFF4CAF50.toInt(), isDefault = true, parentId = null),
        Category(id = 4, name = "Entertainment", iconName = "movie", colorHex = 0xFF9C27B0.toInt(), isDefault = true, parentId = null),
        Category(id = 5, name = "Health", iconName = "medical_services", colorHex = 0xFFF44336.toInt(), isDefault = true, parentId = null)
    )
    val samplePresets = listOf(
        Filter(id = 1, name = "High Value Expenses", filter = TransactionFilter(minAmount = java.math.BigDecimal(500))),
        Filter(id = 2, name = "Tax Deductible", filter = TransactionFilter(categoryIds = setOf(1, 5))),
        Filter(id = 3, name = "Uncleared Transfers", filter = TransactionFilter(statuses = setOf(com.osscabinet.ossWallet.domain.model.TransactionStatus.UNCLEARED)))
    )
    val sampleTags = listOf(
        Tag(1, "Essential", 0xFF004D5F.toInt()),
        Tag(2, "Leisure", 0xFF1A237E.toInt()),
        Tag(3, "Work", 0xFF1B5E20.toInt())
    )

    OssWalletTheme {
        Surface {
            FilterSheetContent(
                filter = TransactionFilter(
                    hasWarranty = true,
                    warrantyExpirationStart = System.currentTimeMillis()
                ),
                accounts = sampleAccounts,
                allCategories = sampleCategories,
                allTags = sampleTags,
                savedFilters = samplePresets,
                onFilterEvent = {},
                onToggleExpand = {},
                onClose = {},
                backgroundColor = AppColors.insetSurface,
            )
        }
    }
}
