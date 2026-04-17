package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainer
) {
    var localFilter by remember { mutableStateOf(filter) }
    var selectedFilterId by remember { mutableStateOf<Long?>(null) }
    var filtersExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp)
    ) {
        // Handle/Selector at top
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
                onFilterChange = { localFilter = it },
                accounts = accounts,
                allCategories = allCategories
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
