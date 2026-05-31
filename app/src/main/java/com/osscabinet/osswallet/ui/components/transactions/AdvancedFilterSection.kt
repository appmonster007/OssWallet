package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.ui.components.AeonTimelinePeriodSelector
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedFilterSection(
    filter: TransactionFilter,
    onFilterChange: (TransactionFilter) -> Unit,
    accounts: List<Account>,
    allCategories: List<Category>,
    allTags: List<Tag> = emptyList(),
    onSearchTransactions: (String) -> kotlinx.coroutines.flow.Flow<List<com.osscabinet.ossWallet.domain.model.Transaction>>,
    savedFilters: List<Filter> = emptyList(),
    modifier: Modifier = Modifier,
    filterRules: CardFilterRules = CardFilterRules.DEFAULT,
    baseFilter: TransactionFilter = TransactionFilter(),
    onAddTag: (String, Int) -> Unit = { _, _ -> }
) {
    var expandedFilters by remember { mutableStateOf(false) }
    var presetsExpanded by remember { mutableStateOf(false) }
    val categoryMap = remember(allCategories) { allCategories.associateBy { it.id } }
    val accountNames = remember(accounts) { accounts.associate { it.id to it.name } }

    val hasBaseFilters = countActiveFilters(baseFilter) > 0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Base Filter Indicator (if any)
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
                modifier = Modifier.fillMaxWidth()
            ) {
                // Render base filters as DISABLED (non-removable) chips
                items(baseFilter.categoryIds.toList()) { id ->
                    FilterChip(
                        selected = true,
                        onClick = { },
                        enabled = false,
                        label = { Text(categoryMap[id]?.name ?: "Cat: $id") }
                    )
                }
                items(baseFilter.accountIds.toList()) { id ->
                    FilterChip(
                        selected = true,
                        onClick = { },
                        enabled = false,
                        label = { Text(accountNames[id] ?: id) }
                    )
                }
                items(baseFilter.transactionTypes.toList()) { type ->
                    FilterChip(
                        selected = true,
                        onClick = { },
                        enabled = false,
                        label = { Text(type.displayName) }
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
        }

        // Timeline Section
        if (filterRules.timelineRule.isVisible) {
            Text(
                "TIMELINE",
                style = AppTextStyles.transactionSwipeAction.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = AppColors.primary
            )

            AeonTimelinePeriodSelector(
                currentRange = filter.timelineRange,
                onRangeSelected = { range -> onFilterChange(filter.copy(timelineRange = range)) },
                modifier = Modifier.fillMaxWidth(),
                swipeEntireComponent = false
            )

            Spacer(Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "ADVANCED FILTERS",
                style = AppTextStyles.transactionSwipeAction.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = AppColors.primary
            )

            if (countActiveFilters(filter) > 0) {
                TextButton(
                    onClick = { onFilterChange(baseFilter) },
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        Icons.Rounded.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear All", style = AppTextStyles.transactionSwipeAction)
                }
            }
        }

        // Active Filter Chips
        if (countActiveFilters(filter) > 0) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // We reuse countActiveFilters and removeFilter logic but render as chips here
                // Categories
                items(filter.categoryIds.toList()) { id ->
                    FilterChip(
                        selected = true,
                        onClick = { removeFilter(filter, "categoryIds", id, onFilterChange) },
                        label = { Text(categoryMap[id]?.name ?: "Cat: $id") },
                        trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) }
                    )
                }
                // Accounts
                items(filter.accountIds.toList()) { id ->
                    FilterChip(
                        selected = true,
                        onClick = { removeFilter(filter, "accountIds", id, onFilterChange) },
                        label = { Text(accountNames[id] ?: id) },
                        trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) }
                    )
                }
                // Types
                items(filter.transactionTypes.toList()) { type ->
                    FilterChip(
                        selected = true,
                        onClick = {
                            removeFilter(
                                filter,
                                "transactionTypes",
                                type,
                                onFilterChange
                            )
                        },
                        label = { Text(type.displayName) },
                        trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) }
                    )
                }
                // Statuses
                items(filter.statuses.toList()) { status ->
                    FilterChip(
                        selected = true,
                        onClick = { removeFilter(filter, "statuses", status, onFilterChange) },
                        label = { Text(status.displayName) },
                        trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) }
                    )
                }
                // Tags
                items(filter.tags.toList()) { tag ->
                    FilterChip(
                        selected = true,
                        onClick = { removeFilter(filter, "tags", tag, onFilterChange) },
                        label = { Text(tag) },
                        trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) }
                    )
                }
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Toggle Advanced Filters Button
            Button(
                onClick = { expandedFilters = !expandedFilters },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (expandedFilters) AppColors.selectionContainer else AppColors.secondaryContainer,
                    contentColor = if (expandedFilters) AppColors.onSelectionContainer else AppColors.onSecondaryContainer
                )
            ) {
                Icon(
                    if (expandedFilters) Icons.Default.ExpandLess else Icons.Default.Add,
                    null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (expandedFilters) "Fewer Filters" else "More Filters",
                    style = AppTextStyles.drawerItemLabel
                )
            }

            // Load Preset Button
            if (savedFilters.isNotEmpty()) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { presetsExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Presets", style = AppTextStyles.drawerItemLabel)
                    }

                    DropdownMenu(
                        expanded = presetsExpanded,
                        onDismissRequest = { presetsExpanded = false }
                    ) {
                        savedFilters.forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset.name) },
                                onClick = {
                                    onFilterChange(preset.filter)
                                    presetsExpanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.FilterList,
                                        null,
                                        Modifier.size(18.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // Embedded Filter Fields
        androidx.compose.animation.AnimatedVisibility(
            visible = expandedFilters,
            enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FilterFields(
                    filter = filter,
                    onFilterChange = onFilterChange,
                    accounts = accounts,
                    allCategories = allCategories,
                    allTags = allTags,
                    onSearchTransactions = onSearchTransactions,
                    onImmediateApply = { newFilter, _ -> onFilterChange(newFilter) },
                    onAddTag = onAddTag,
                    filterRules = filterRules
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AdvancedFilterSectionPreview() {
    OssWalletTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            AdvancedFilterSection(
                filter = TransactionFilter(),
                onFilterChange = {},
                accounts = emptyList(),
                allCategories = emptyList(),
                onSearchTransactions = { _ -> flowOf(emptyList()) }
            )
        }
    }
}
