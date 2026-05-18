package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
import androidx.compose.ui.text.style.TextOverflow
import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.ui.components.AeonTimelinePeriodSelector
import com.osscabinet.ossWallet.ui.components.entry.*
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

private val TransactionFilterSaver = Saver<MutableState<TransactionFilter>, String>(
    save = { Json.encodeToString(it.value) },
    restore = { mutableStateOf(Json.decodeFromString(it)) }
)

private val FilterIdsSaver = Saver<MutableState<Set<Long>>, ArrayList<Long>>(
    save = { ArrayList(it.value.toList()) },
    restore = { mutableStateOf(it.toSet()) }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheetContent(
    filter: TransactionFilter,
    accounts: List<Account>,
    allCategories: List<Category>,
    allTags: List<Tag> = emptyList(),
    allTransactions: List<Transaction> = emptyList(),
    savedFilters: List<Filter> = emptyList(),
    onFilterEvent: (FilterEvent) -> Unit,
    onSaveFilterClick: (TransactionFilter) -> Unit = {},
    onClose: () -> Unit,
    backgroundColor: Color = AppColors.sheetSurface,
    showAccountFilter: Boolean = true,
    accountNames: Map<String, String> = emptyMap(),
    onAddTag: (String, Int) -> Unit = { _, _ -> },
    initialShowCustomize: Boolean = false,
    filterRules: CardFilterRules = CardFilterRules.DEFAULT,
    baseFilter: TransactionFilter = TransactionFilter()
) {
    var localFilter by rememberSaveable(saver = TransactionFilterSaver) { mutableStateOf(filter) }
    var selectedFilterIds by rememberSaveable(saver = FilterIdsSaver) { mutableStateOf(emptySet<Long>()) }
    var presetsDropdownExpanded by remember { mutableStateOf(false) }
    var showCustomize by rememberSaveable { mutableStateOf(initialShowCustomize) }

    val categoryMap = remember(allCategories) { allCategories.associateBy { it.id } }
    val effectiveAccountNames = remember(accounts, accountNames) { 
        accounts.associate { it.id to it.name } + accountNames 
    }

    // Debouncing mechanism for instant filter application
    val filterSheetScope = rememberCoroutineScope()
    val debounceJob = remember { mutableStateOf<Job?>(null) }

    fun applyFilterWithDebounce(newFilter: TransactionFilter, debounceMs: Long = 300) {
        debounceJob.value?.cancel()
        debounceJob.value = filterSheetScope.launch {
            delay(debounceMs)
            onFilterEvent(FilterEvent.Apply(newFilter))
            debounceJob.value = null
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
            localFilter = baseFilter
            applyFilterWithDebounce(localFilter, debounceMs = 0)
            return
        }

        val filters = savedFilters
            .filter { it.id in selectedFilterIds }
            .map { it.filter }

        localFilter = combineFiltersWithOrLogic(filters)
        applyFilterWithDebounce(localFilter, debounceMs = 0)
    }

    // Sync localFilter with filter prop when it changes (only when not currently editing)
    LaunchedEffect(filter) {
        if (debounceJob.value == null) {
            localFilter = filter
        }
    }

    val scrollState = rememberScrollState()
    val hasActiveFilters = remember(localFilter, baseFilter) {
        countActiveFilters(localFilter, baseFilter, filterRules) > 0
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .verticalScroll(scrollState)
            .padding(AeonTheme.dimensions.screenPadding)
    ) {
        // 1. Base Scope Indicator (if any)
        val hasBaseFilters = countActiveFilters(baseFilter) > 0
        if (hasBaseFilters) {
            Text(
                "BASE SCOPE",
                style = AppTextStyles.transactionSwipeAction.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = AppColors.secondary.copy(alpha = 0.7f)
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                items(items = baseFilter.categoryIds.toList()) { id ->
                    FilterChip(
                        selected = true,
                        onClick = { },
                        enabled = false,
                        label = { Text(categoryMap[id]?.name ?: "Cat: $id") }
                    )
                }
                items(items = baseFilter.accountIds.toList()) { id ->
                    FilterChip(
                        selected = true,
                        onClick = { },
                        enabled = false,
                        label = { Text(effectiveAccountNames[id] ?: id) }
                    )
                }
                items(items = baseFilter.transactionTypes.toList()) { type ->
                    FilterChip(
                        selected = true,
                        onClick = { },
                        enabled = false,
                        label = { Text(type.displayName) }
                    )
                }
            }
            Spacer(Modifier.height(AeonTheme.dimensions.spacingMd))
        }

        // 2. Timeline Selector
        if (filterRules.allowTimelineSelection) {
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
                color = AppColors.divider.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingMd))
        }

        // 3. Future Transactions Toggle
        if (filterRules.allowFutureTransactionsToggle) {
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
        }

        // 4. Presets and Customise Options
        Box(modifier = Modifier.fillMaxWidth()) {
            FilterPeekOptions(
                onPresetsClick = {
                    presetsDropdownExpanded = !presetsDropdownExpanded
                },
                onMoreClick = {
                    showCustomize = !showCustomize
                },
                hasPresets = savedFilters.isNotEmpty(),
                hasActiveFilters = hasActiveFilters,
                isExpanded = showCustomize
            )

            // Presets Dropdown anchored here
            DropdownMenu(
                expanded = presetsDropdownExpanded,
                onDismissRequest = { presetsDropdownExpanded = false },
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
                            presetsDropdownExpanded = false
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

        // 5. Active Filter Chips (Quick removal)
        val activeFiltersRelative = countActiveFilters(localFilter, baseFilter, filterRules)
        if (activeFiltersRelative > 0) {
            Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingMd))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Categories
                items(items = localFilter.categoryIds.toList()) { id ->
                    if (id !in baseFilter.categoryIds) {
                        FilterChip(
                            selected = true,
                            onClick = { 
                                removeFilter(localFilter, "categoryIds", id) {
                                    localFilter = it
                                    selectedFilterIds = emptySet()
                                    applyFilterWithDebounce(it, debounceMs = 0)
                                }
                            },
                            label = { Text(categoryMap[id]?.name ?: "Cat: $id") },
                            trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) }
                        )
                    }
                }
                // Accounts
                items(items = localFilter.accountIds.toList()) { id ->
                    if (id !in baseFilter.accountIds) {
                        FilterChip(
                            selected = true,
                            onClick = { 
                                removeFilter(localFilter, "accountIds", id) {
                                    localFilter = it
                                    selectedFilterIds = emptySet()
                                    applyFilterWithDebounce(it, debounceMs = 0)
                                }
                            },
                            label = { Text(effectiveAccountNames[id] ?: id) },
                            trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) }
                        )
                    }
                }
                // Types
                items(items = localFilter.transactionTypes.toList()) { type ->
                    if (type !in baseFilter.transactionTypes) {
                        FilterChip(
                            selected = true,
                            onClick = { 
                                removeFilter(localFilter, "transactionTypes", type) {
                                    localFilter = it
                                    selectedFilterIds = emptySet()
                                    applyFilterWithDebounce(it, debounceMs = 0)
                                }
                            },
                            label = { Text(type.displayName) },
                            trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) }
                        )
                    }
                }
                // Tags
                items(items = localFilter.tags.toList()) { tag ->
                    if (tag !in baseFilter.tags) {
                        FilterChip(
                            selected = true,
                            onClick = { 
                                removeFilter(localFilter, "tags", tag) {
                                    localFilter = it
                                    selectedFilterIds = emptySet()
                                    applyFilterWithDebounce(it, debounceMs = 0)
                                }
                            },
                            label = { Text(tag) },
                            trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) }
                        )
                    }
                }
                // Statuses
                items(items = localFilter.statuses.toList()) { status ->
                    if (status !in baseFilter.statuses) {
                        FilterChip(
                            selected = true,
                            onClick = { 
                                removeFilter(localFilter, "statuses", status) {
                                    localFilter = it
                                    selectedFilterIds = emptySet()
                                    applyFilterWithDebounce(it, debounceMs = 0)
                                }
                            },
                            label = { Text(status.displayName) },
                            trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) }
                        )
                    }
                }
            }
        }

        // 6. Full Filter Section (Shown only if Customise selected)
        androidx.compose.animation.AnimatedVisibility(
            visible = showCustomize,
            enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AeonTheme.dimensions.spacingXl),
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
                            localFilter = baseFilter
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

                val allAvailableCurrencies = remember(accounts) {
                    accounts.flatMap { account ->
                        account.allowedCurrencies.ifEmpty { listOf(account.currencyCode) }
                    }.distinct().sorted()
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
                    showAccountFilter = showAccountFilter && filterRules.allowAccountSelection,
                    allAvailableCurrencies = allAvailableCurrencies,
                    onImmediateApply = { newFilter, debounceMs ->
                        applyFilterWithDebounce(newFilter, debounceMs)
                    },
                    onAddTag = onAddTag,
                    filterRules = filterRules
                )
            }
        }

        Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingXl))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onClose,
                modifier = Modifier.weight(0.4f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Close", style = AppTextStyles.listItemTitle)
            }

            Button(
                onClick = { 
                    debounceJob.value?.cancel()
                    onFilterEvent(FilterEvent.Apply(localFilter))
                    onSaveFilterClick(localFilter) 
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.primary,
                    contentColor = AppColors.onPrimary
                )
            ) {
                Icon(Icons.Default.Save, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Save as Preset", style = AppTextStyles.listItemTitle.copy(fontWeight = FontWeight.Bold))
            }
        }

        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun FilterPeekOptions(
    onPresetsClick: () -> Unit,
    onMoreClick: () -> Unit,
    hasPresets: Boolean,
    hasActiveFilters: Boolean,
    isExpanded: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
    ) {
        // Presets Option
        Surface(
            onClick = onPresetsClick,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            shape = RoundedCornerShape(16.dp),
            color = AppColors.cardSurface,
            border = androidx.compose.foundation.BorderStroke(
                1.dp, 
                AppColors.divider.copy(alpha = 0.1f)
            ),
            enabled = hasPresets
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = AppColors.primary.copy(alpha = 0.1f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = AppColors.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Column {
                    Text(
                        "PRESETS",
                        style = AppTextStyles.transactionSwipeAction.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = AppColors.contentSecondary.copy(alpha = 0.6f)
                    )
                    Text(
                        if (hasPresets) "Select..." else "None",
                        style = AppTextStyles.listItemTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // More/Customize Option
        Surface(
            onClick = onMoreClick,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            shape = RoundedCornerShape(16.dp),
            color = if (isExpanded) AppColors.primary.copy(alpha = 0.08f) 
                   else if (hasActiveFilters) AppColors.secondary.copy(alpha = 0.1f) 
                   else AppColors.cardSurface,
            border = androidx.compose.foundation.BorderStroke(
                1.dp, 
                if (isExpanded) AppColors.primary.copy(alpha = 0.3f)
                else if (hasActiveFilters) AppColors.secondary.copy(alpha = 0.3f) 
                else AppColors.divider.copy(alpha = 0.1f)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = (if (isExpanded) AppColors.primary 
                            else if (hasActiveFilters) AppColors.secondary 
                            else AppColors.contentSecondary).copy(alpha = 0.1f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.Tune,
                            contentDescription = null,
                            tint = if (isExpanded) AppColors.primary 
                                   else if (hasActiveFilters) AppColors.secondary 
                                   else AppColors.contentSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Column {
                    Text(
                        "CUSTOMIZE",
                        style = AppTextStyles.transactionSwipeAction.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = AppColors.contentSecondary.copy(alpha = 0.6f)
                    )
                    Text(
                        if (isExpanded) "Show Less" else "More...",
                        style = AppTextStyles.listItemTitle
                    )
                }
            }
        }
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
    val pullTabHeight = 20.dp

    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
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

@Preview(showBackground = true, name = "Filter Sheet - Peek Options")
@Composable
fun FilterSheetContentPeekPreview() {
    OssWalletTheme {
        Surface {
            FilterSheetContent(
                filter = TransactionFilter(),
                accounts = emptyList(),
                allCategories = emptyList(),
                onFilterEvent = {},
                onClose = {},
                backgroundColor = AppColors.insetSurface,
            )
        }
    }
}

@Preview(showBackground = true, name = "Filter Sheet - Single Data (Expanded)")
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
                onClose = {},
                backgroundColor = AppColors.insetSurface,
                initialShowCustomize = true
            )
        }
    }
}

@Preview(showBackground = true, name = "Filter Sheet - Long Content (Expanded)", heightDp = 1600)
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
                onClose = {},
                backgroundColor = AppColors.insetSurface,
                initialShowCustomize = true
            )
        }
    }
}
