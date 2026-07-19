package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.ui.components.AeonTimelinePeriodSelector
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

private val TransactionFilterSaver = Saver<MutableState<TransactionFilter>, String>(
    save = { Json.encodeToString(it.value) },
    restore = { mutableStateOf(Json.decodeFromString(it)) }
)


/**
 * Scrollable filter sheet content using Column + verticalScroll.
 * Designed to render beautifully inside an Aeon Glass surface (Filter Pill or Dialog).
 * Supports a conditional Hero Header to enable seamless morph transitions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheetContent(
    filter: TransactionFilter,
    accounts: List<Account>,
    allCategories: List<Category>,
    onSearchTransactions: (String) -> Flow<List<Transaction>>,
    onFilterEvent: (FilterEvent) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    allTags: List<Tag> = emptyList(),
    savedFilters: List<Filter> = emptyList(),
    onSaveFilterClick: (TransactionFilter) -> Unit = {},
    backgroundColor: Color = AppColors.transparent,
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
    val scrollRequester = remember { BringIntoViewRequester() }
    val focusManager = LocalFocusManager.current

    // H "Editorial Instrument" champagne/bronze fill for active (removable) filter chips.
    val activeChipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = AppColors.accentContainer,
        selectedLabelColor = AppColors.onAccentContainer,
        selectedLeadingIconColor = AppColors.onAccentContainer,
        selectedTrailingIconColor = AppColors.onAccentContainer,
    )

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
        if (debounceJob.value == null && localFilter != filter) {
            localFilter = filter
        }
    }

    val allAvailableCurrencies = remember(accounts) {
        accounts.flatMap { account ->
            account.allowedCurrencies.ifEmpty { listOf(account.currencyCode) }
        }.distinct().sorted()
    }

    val navPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val hasHeroHeader = !title.isNullOrBlank()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .background(backgroundColor)
            .padding(horizontal = AeonTheme.dimensions.screenPadding)
            .padding(top = if (hasHeroHeader) AeonTheme.dimensions.spacingLg else AeonTheme.dimensions.spacingMd)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            },
        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg)
    ) {
        // 0. Hero Header (Conditional)
        // If title is provided, we show the full Architect Slate header.
        // If title is null (e.g. inside a Pill), we hide this to allow the Pill's timeline to be the header.
        if (hasHeroHeader) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = AppTextStyles.heroAmount.copy(fontSize = 24.sp),
                    color = AppColors.contentPrimary
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
                    Text("Reset", style = AppTextStyles.listSectionHeader)
                }
            }
        }

        // 1. Base Scope Indicator (Standardized)
        val hasBaseFilters = countActiveFilters(baseFilter) > 0
        if (hasBaseFilters) {
            Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingXs)) {
                Text(
                    "BASE SCOPE",
                    style = AppTextStyles.sectionOverline.copy(fontSize = 10.sp),
                    color = AppColors.contentSecondary.copy(alpha = 0.5f)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
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
            }
        }

        // 2. Timeline Selector
        if (filterRules.timelineRule.isVisible) {
            AeonTimelinePeriodSelector(
                currentRange = localFilter.timelineRange,
                onRangeSelected = { range ->
                    localFilter = localFilter.copy(timelineRange = range)
                    onFilterEvent(FilterEvent.UpdateTimeline(range))
                    applyFilterWithDebounce(localFilter, debounceMs = 0)
                },
                swipeEntireComponent = true,
                isFlush = true
            )
            HorizontalDivider(thickness = 0.5.dp, color = AppColors.divider.copy(alpha = 0.4f))
        }

        // 3. Active Filter Chips + Inline Reset
        val activeFiltersRelative = countActiveFilters(localFilter, baseFilter, filterRules)
        if (activeFiltersRelative > 0 || !hasHeroHeader) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Inline Reset Button (only shown if Hero Header is hidden to ensure access)
                if (!hasHeroHeader) {
                    item {
                        TextButton(
                            onClick = {
                                localFilter = baseFilter
                                onFilterEvent(FilterEvent.Reset)
                            },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = AppColors.primary)
                        ) {
                            Icon(Icons.Rounded.RestartAlt, null, Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reset All", style = AppTextStyles.transactionSwipeAction)
                        }
                    }
                }

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
                            colors = activeChipColors,
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
                            colors = activeChipColors,
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
                            colors = activeChipColors,
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
                            colors = activeChipColors,
                            label = { Text(tag) },
                            trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) }
                        )
                    }
                }
                if (localFilter.tags.size > 1) {
                    item {
                        FilterChip(
                            selected = localFilter.matchAllTags,
                            onClick = {
                                val next =
                                    localFilter.copy(matchAllTags = !localFilter.matchAllTags)
                                localFilter = next
                                applyFilterWithDebounce(next, debounceMs = 0)
                            },
                            colors = activeChipColors,
                            label = { Text(if (localFilter.matchAllTags) "Match ALL Tags" else "Match ANY Tag") }
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
                            colors = activeChipColors,
                            label = { Text(status.displayName) },
                            trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) }
                        )
                    }
                }
            }
        }

        // 4. Filter fields (Core Criteria, Search & Tags, Amount & Records)
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
            allAvailableCurrencies = allAvailableCurrencies,
            onImmediateApply = { newFilter, debounceMs ->
                applyFilterWithDebounce(newFilter, debounceMs)
            },
            onAddTag = onAddTag,
            filterRules = filterRules,
            scrollRequester = scrollRequester
        )

        // 5. Footer Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onClose,
                modifier = Modifier.weight(0.4f),
                shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius)
            ) {
                Text(
                    "Close",
                    style = AppTextStyles.buttonLabel.copy(color = AppColors.contentSecondary)
                )
            }

            Button(
                onClick = {
                    debounceJob.value?.cancel()
                    onFilterEvent(FilterEvent.Apply(localFilter))
                    onSaveFilterClick(localFilter)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(AeonTheme.dimensions.buttonCornerRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.primary,
                    contentColor = AppColors.onPrimary
                )
            ) {
                Icon(Icons.Default.Save, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Save Preset",
                    style = AppTextStyles.buttonLabel
                )
            }
        }

        Spacer(modifier = Modifier.height(navPadding + 24.dp))
    }
}


@Preview(showBackground = true)
@Composable
fun FilterSheetContentPreview() {
    OssWalletTheme {
        FilterSheetContent(
            filter = TransactionFilter(),
            accounts = emptyList(),
            allCategories = emptyList(),
            onSearchTransactions = { _ -> flowOf(emptyList()) },
            onFilterEvent = {},
            onClose = {}
        )
    }
}
