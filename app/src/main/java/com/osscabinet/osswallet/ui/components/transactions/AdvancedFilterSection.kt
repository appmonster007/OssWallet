package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.toTransactionFilter
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.data.mapper.toDomain
import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.components.TimelineFilterBar
import kotlinx.coroutines.delay

import com.osscabinet.ossWallet.domain.card.CardFilterRules

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedFilterSection(
    filter: TransactionFilter,
    onFilterChange: (TransactionFilter) -> Unit,
    accounts: List<Account>,
    allCategories: List<Category>,
    savedFilters: List<Filter> = emptyList(),
    modifier: Modifier = Modifier,
    filterRules: CardFilterRules = CardFilterRules.DEFAULT,
    baseFilter: TransactionFilter = TransactionFilter()
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
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Render base filters as DISABLED (non-removable) chips
                items(baseFilter.categoryIds) { id ->
                    FilterChip(
                        selected = true,
                        onClick = { },
                        enabled = false,
                        label = { Text(categoryMap[id]?.name ?: "Cat: $id") }
                    )
                }
                items(baseFilter.accountIds) { id ->
                    FilterChip(
                        selected = true,
                        onClick = { },
                        enabled = false,
                        label = { Text(accountNames[id] ?: id) }
                    )
                }
                baseFilter.transactionType?.let { type ->
                    item {
                        FilterChip(
                            selected = true,
                            onClick = { },
                            enabled = false,
                            label = { Text(type.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(4.dp))
        }

        // Timeline Section
        if (filterRules.allowTimelineSelection) {
            Text(
                "TIMELINE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
            
            TimelineFilterBar(
                currentRange = filter.timelineRange,
                onRangeSelected = { range -> onFilterChange(filter.copy(timelineRange = range)) },
                modifier = Modifier.fillMaxWidth()
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
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
            
            if (countActiveFilters(filter) > 0) {
                TextButton(
                    onClick = { onFilterChange(TransactionFilter()) },
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text("Clear All", style = MaterialTheme.typography.labelSmall)
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
                items(filter.categoryIds) { id ->
                    FilterChip(
                        selected = true,
                        onClick = { removeFilter(filter, "categoryIds", id, onFilterChange) },
                        label = { Text(categoryMap[id]?.name ?: "Cat: $id") },
                        trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) }
                    )
                }
                // Accounts
                items(filter.accountIds) { id ->
                    FilterChip(
                        selected = true,
                        onClick = { removeFilter(filter, "accountIds", id, onFilterChange) },
                        label = { Text(accountNames[id] ?: id) },
                        trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) }
                    )
                }
                // Type
                filter.transactionType?.let { type ->
                    item {
                        FilterChip(
                            selected = true,
                            onClick = { removeFilter(filter, "transactionType", null, onFilterChange) },
                            label = { Text(type.replaceFirstChar { it.uppercase() }) },
                            trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) }
                        )
                    }
                }
                // Statuses
                items(filter.statuses) { status ->
                    FilterChip(
                        selected = true,
                        onClick = { removeFilter(filter, "statuses", status, onFilterChange) },
                        label = { Text(status.displayName) },
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
                    containerColor = if (expandedFilters) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = if (expandedFilters) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(if (expandedFilters) Icons.Default.ExpandLess else Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (expandedFilters) "Fewer Filters" else "More Filters", style = MaterialTheme.typography.labelLarge)
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
                        Text("Presets", style = MaterialTheme.typography.labelLarge)
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
                                leadingIcon = { Icon(Icons.Default.FilterList, null, Modifier.size(18.dp)) }
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
                    showAccountFilter = filterRules.allowAccountSelection,
                    onImmediateApply = { newFilter, _ -> onFilterChange(newFilter) },
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
        Surface {
            AdvancedFilterSection(
                filter = TransactionFilter(),
                onFilterChange = {},
                accounts = emptyList(),
                allCategories = emptyList(),
                baseFilter = TransactionFilter(accountIds = listOf("1"))
            )
        }
    }
}
