package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.data.local.entity.CategoryEntity
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheetContent(
    filter: TransactionFilter,
    accounts: List<Account>,
    allCategories: List<CategoryEntity>,
    onSetDateRange: (Long?, Long?) -> Unit,
    onApplyFilter: (TransactionFilter) -> Unit,
    onToggleExpand: () -> Unit,
    onClose: () -> Unit,
    isExpanded: Boolean,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainer
) {
    var localFilter by remember { mutableStateOf(filter) }

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
                    "Refine Transactions",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                TextButton(
                    onClick = { localFilter = TransactionFilter() },
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text("Reset All", style = MaterialTheme.typography.labelLarge)
                }
            }

            // Search (Note/Payee) - Now 1st Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CompactTextField(
                    value = localFilter.queryNote ?: "",
                    onValueChange = { localFilter = localFilter.copy(queryNote = it.ifBlank { null }) },
                    placeholder = "Note...",
                    modifier = Modifier.weight(1f),
                    leadingIcon = Icons.Default.Search
                )
                CompactTextField(
                    value = localFilter.queryPayee ?: "",
                    onValueChange = { localFilter = localFilter.copy(queryPayee = it.ifBlank { null }) },
                    placeholder = "Payee...",
                    modifier = Modifier.weight(1f),
                    leadingIcon = Icons.Default.Search
                )
            }

            // Transaction Type (Segmented Buttons)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "TYPE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val options = listOf("All", "Income", "Expense")
                    options.forEachIndexed { index, label ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                            onClick = {
                                localFilter = localFilter.copy(
                                    isIncome = when (index) {
                                        1 -> true
                                        2 -> false
                                        else -> null
                                    }
                                )
                            },
                            selected = when (index) {
                                1 -> localFilter.isIncome == true
                                2 -> localFilter.isIncome == false
                                else -> localFilter.isIncome == null
                            },
                            icon = {
                                when (index) {
                                    1 -> Icon(Icons.AutoMirrored.Filled.TrendingUp, null, Modifier.size(16.dp))
                                    2 -> Icon(Icons.AutoMirrored.Filled.TrendingDown, null, Modifier.size(16.dp))
                                    else -> SegmentedButtonDefaults.Icon(active = localFilter.isIncome == null)
                                }
                            }
                        ) {
                            Text(label)
                        }
                    }
                }
            }

            // Accounts & Categories (Dropdowns in a Row)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MultiSelectDropdown(
                    label = "Accounts",
                    selectedCount = localFilter.accountIds.size,
                    modifier = Modifier.weight(1f),
                    onClear = { localFilter = localFilter.copy(accountIds = emptyList()) }
                ) {
                    accounts.forEach { account ->
                        val isSelected = account.id in localFilter.accountIds
                        DropdownMenuItem(
                            text = { Text(account.name) },
                            onClick = {
                                val current = localFilter.accountIds.toMutableList()
                                if (isSelected) current.remove(account.id) else current.add(account.id)
                                localFilter = localFilter.copy(accountIds = current)
                            },
                            leadingIcon = {
                                Checkbox(checked = isSelected, onCheckedChange = null)
                            }
                        )
                    }
                }

                MultiSelectDropdown(
                    label = "Categories",
                    selectedCount = localFilter.categoryIds.size,
                    modifier = Modifier.weight(1f),
                    onClear = { localFilter = localFilter.copy(categoryIds = emptyList()) }
                ) {
                    allCategories.forEach { category ->
                        val isSelected = category.id in localFilter.categoryIds
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = {
                                val current = localFilter.categoryIds.toMutableList()
                                if (isSelected) current.remove(category.id) else current.add(category.id)
                                localFilter = localFilter.copy(categoryIds = current)
                            },
                            leadingIcon = {
                                Checkbox(checked = isSelected, onCheckedChange = null)
                            }
                        )
                    }
                }
            }

            // Amount Range (Min/Max)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "AMOUNT RANGE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactTextField(
                        value = localFilter.minAmount?.toString() ?: "",
                        onValueChange = { localFilter = localFilter.copy(minAmount = it.toBigDecimalOrNull()) },
                        placeholder = "Min",
                        modifier = Modifier.weight(1f),
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    )
                    Text("-", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    CompactTextField(
                        value = localFilter.maxAmount?.toString() ?: "",
                        onValueChange = { localFilter = localFilter.copy(maxAmount = it.toBigDecimalOrNull()) },
                        placeholder = "Max",
                        modifier = Modifier.weight(1f),
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    )
                }
            }

            // Apply Button
            Button(
                onClick = {
                    onApplyFilter(localFilter)
                    onClose()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Apply Filters", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiSelectDropdown(
    label: String,
    selectedCount: Int,
    modifier: Modifier = Modifier,
    onClear: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
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
                if (selectedCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            ),
            onClick = { expanded = !expanded }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (selectedCount == 0) label else "$label ($selectedCount)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selectedCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectedCount > 0) {
                        IconButton(
                            onClick = { 
                                onClear()
                                expanded = false
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, null, Modifier.size(16.dp))
                        }
                    }
                    Icon(
                        Icons.Default.ArrowDropDown,
                        null,
                        Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            content()
        }
    }
}

@Composable
fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text
) {
    Surface(
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(
                    leadingIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    innerTextField()
                }
            )
            if (value.isNotEmpty()) {
                IconButton(
                    onClick = { onValueChange("") },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Close, null, Modifier.size(16.dp))
                }
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
