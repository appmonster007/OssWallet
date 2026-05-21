package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.animation.core.Spring
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
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.ui.components.AeonTimelinePeriodSelector
import com.osscabinet.ossWallet.ui.components.entry.*
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import kotlinx.coroutines.flow.Flow

private val TransactionFilterSaver = Saver<MutableState<TransactionFilter>, String>(
    save = { Json.encodeToString(it.value) },
    restore = { mutableStateOf(Json.decodeFromString(it)) }
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheetContent(
    filter: TransactionFilter,
    accounts: List<Account>,
    allCategories: List<Category>,
    allTags: List<Tag> = emptyList(),
    onSearchTransactions: (String) -> Flow<List<Transaction>>,
    savedFilters: List<Filter> = emptyList(),
    onFilterEvent: (FilterEvent) -> Unit,
    onSaveFilterClick: (TransactionFilter) -> Unit = {},
    onClose: () -> Unit,
    backgroundColor: Color = AppColors.sheetSurface,
    showAccountFilter: Boolean = true,
    accountNames: Map<String, String> = emptyMap(),
    onAddTag: (String, Int) -> Unit = { _, _ -> },
    filterRules: CardFilterRules = CardFilterRules.DEFAULT,
    baseFilter: TransactionFilter = TransactionFilter()
) {
    var localFilter by rememberSaveable(saver = TransactionFilterSaver) { mutableStateOf(filter) }

    val categoryMap = remember(allCategories) { allCategories.associateBy { it.id } }
    val effectiveAccountNames = remember(accounts, accountNames) {
        accounts.associate { it.id to it.name } + accountNames
    }

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

    // Sync localFilter when the external filter changes (and no edit is in flight).
    LaunchedEffect(filter) {
        if (debounceJob.value == null) {
            localFilter = filter
        }
    }

    val scrollState = rememberScrollState()

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
                    applyFilterWithDebounce(localFilter, debounceMs = 0)
                },
                swipeEntireComponent = true
            )

            Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingMd))
            HorizontalDivider(thickness = 0.5.dp, color = AppColors.divider.copy(alpha = 0.7f))
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
            Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingMd))
            HorizontalDivider(thickness = 0.5.dp, color = AppColors.divider.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingMd))
        }

        // 4. Title row + Reset
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
                    onFilterEvent(FilterEvent.Reset)
                },
                contentPadding = PaddingValues(horizontal = AeonTheme.dimensions.spacingSm),
                colors = ButtonDefaults.textButtonColors(contentColor = AppColors.primary)
            ) {
                Icon(
                    Icons.Rounded.RestartAlt,
                    contentDescription = null,
                    modifier = Modifier.size(AeonTheme.dimensions.iconSizeXs)
                )
                Spacer(modifier = Modifier.width(AeonTheme.dimensions.spacingXs))
                Text("Reset All", style = AppTextStyles.listSectionHeader)
            }
        }

        Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingMd))

        // 5. Active Filter Chips (quick removal)
        val activeFiltersRelative = countActiveFilters(localFilter, baseFilter, filterRules)
        if (activeFiltersRelative > 0) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(items = localFilter.categoryIds.toList()) { id ->
                    if (id !in baseFilter.categoryIds) {
                        FilterChip(
                            selected = true,
                            onClick = {
                                removeFilter(localFilter, "categoryIds", id) {
                                    localFilter = it
                                    applyFilterWithDebounce(it, debounceMs = 0)
                                }
                            },
                            label = { Text(categoryMap[id]?.name ?: "Cat: $id") },
                            trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) }
                        )
                    }
                }
                items(items = localFilter.accountIds.toList()) { id ->
                    if (id !in baseFilter.accountIds) {
                        FilterChip(
                            selected = true,
                            onClick = {
                                removeFilter(localFilter, "accountIds", id) {
                                    localFilter = it
                                    applyFilterWithDebounce(it, debounceMs = 0)
                                }
                            },
                            label = { Text(effectiveAccountNames[id] ?: id) },
                            trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) }
                        )
                    }
                }
                items(items = localFilter.transactionTypes.toList()) { type ->
                    if (type !in baseFilter.transactionTypes) {
                        FilterChip(
                            selected = true,
                            onClick = {
                                removeFilter(localFilter, "transactionTypes", type) {
                                    localFilter = it
                                    applyFilterWithDebounce(it, debounceMs = 0)
                                }
                            },
                            label = { Text(type.displayName) },
                            trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) }
                        )
                    }
                }
                items(items = localFilter.tags.toList()) { tag ->
                    if (tag !in baseFilter.tags) {
                        FilterChip(
                            selected = true,
                            onClick = {
                                removeFilter(localFilter, "tags", tag) {
                                    localFilter = it
                                    applyFilterWithDebounce(it, debounceMs = 0)
                                }
                            },
                            label = { Text(tag) },
                            trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) }
                        )
                    }
                }
                items(items = localFilter.statuses.toList()) { status ->
                    if (status !in baseFilter.statuses) {
                        FilterChip(
                            selected = true,
                            onClick = {
                                removeFilter(localFilter, "statuses", status) {
                                    localFilter = it
                                    applyFilterWithDebounce(it, debounceMs = 0)
                                }
                            },
                            label = { Text(status.displayName) },
                            trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingMd))
        }

        // 6. Filter fields — always visible
        val allAvailableCurrencies = remember(accounts) {
            accounts.flatMap { account ->
                account.allowedCurrencies.ifEmpty { listOf(account.currencyCode) }
            }.distinct().sorted()
        }

        FilterFields(
            filter = localFilter,
            onFilterChange = {
                localFilter = it
                applyFilterWithDebounce(it, debounceMs = 300)
            },
            accounts = accounts,
            allCategories = allCategories,
            allTags = allTags,
            onSearchTransactions = onSearchTransactions,
            showAccountFilter = showAccountFilter && filterRules.allowAccountSelection,
            allAvailableCurrencies = allAvailableCurrencies,
            onImmediateApply = { newFilter, debounceMs ->
                applyFilterWithDebounce(newFilter, debounceMs)
            },
            onAddTag = onAddTag,
            filterRules = filterRules
        )

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

