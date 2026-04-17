package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.ui.components.AeonDateTimePicker
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
        modifier = Modifier.fillMaxSize()
    ) {
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
                .weight(1f)
                .fillMaxWidth()
                .background(backgroundColor)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Filter Records", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                TextButton(onClick = { localFilter = TransactionFilter() }) {
                    Text("Clear All", color = MaterialTheme.colorScheme.primary)
                }
            }

            // Text Search: Note & Payee
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("SEARCH", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
                OutlinedTextField(
                    value = localFilter.queryNote ?: "",
                    onValueChange = { localFilter = localFilter.copy(queryNote = it.ifBlank { null }) },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    ),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                            Text("Regex", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Checkbox(
                                checked = localFilter.isNoteRegex,
                                onCheckedChange = { localFilter = localFilter.copy(isNoteRegex = it) }
                            )
                        }
                    }
                )

                OutlinedTextField(
                    value = localFilter.queryPayee ?: "",
                    onValueChange = { localFilter = localFilter.copy(queryPayee = it.ifBlank { null }) },
                    label = { Text("Payee") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    ),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                            Text("Regex", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Checkbox(
                                checked = localFilter.isPayeeRegex,
                                onCheckedChange = { localFilter = localFilter.copy(isPayeeRegex = it) }
                            )
                        }
                    }
                )
            }

            // Type (Income/Expense)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("TRANSACTION TYPE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = localFilter.isIncome == null,
                        onClick = { localFilter = localFilter.copy(isIncome = null) },
                        label = { Text("All") },
                        shape = RoundedCornerShape(12.dp)
                    )
                    FilterChip(
                        selected = localFilter.isIncome == true,
                        onClick = { localFilter = localFilter.copy(isIncome = true) },
                        label = { Text("Income") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.TrendingUp, null, Modifier.size(16.dp)) },
                        shape = RoundedCornerShape(12.dp)
                    )
                    FilterChip(
                        selected = localFilter.isIncome == false,
                        onClick = { localFilter = localFilter.copy(isIncome = false) },
                        label = { Text("Expense") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.TrendingDown, null, Modifier.size(16.dp)) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Date Range
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("DATE RANGE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    var showStartDatePicker by remember { mutableStateOf(false) }
                    var showEndDatePicker by remember { mutableStateOf(false) }

                    OutlinedButton(
                        onClick = { showStartDatePicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Text(localFilter.startDate?.let { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(it)) } ?: "Start Date", color = MaterialTheme.colorScheme.onSurface)
                    }
                    Text("to", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(
                        onClick = { showEndDatePicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Text(localFilter.endDate?.let { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(it)) } ?: "End Date", color = MaterialTheme.colorScheme.onSurface)
                    }

                    if (showStartDatePicker) {
                        AeonDateTimePicker(
                            initialDateTime = localFilter.startDate ?: System.currentTimeMillis(),
                            onDateTimeSelected = { localFilter = localFilter.copy(startDate = it) },
                            onDismiss = { showStartDatePicker = false }
                        )
                    }
                    if (showEndDatePicker) {
                        AeonDateTimePicker(
                            initialDateTime = localFilter.endDate ?: System.currentTimeMillis(),
                            onDateTimeSelected = { localFilter = localFilter.copy(endDate = it) },
                            onDismiss = { showEndDatePicker = false }
                        )
                    }
                }
            }

            // Amount Range
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("AMOUNT RANGE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = localFilter.minAmount?.toString() ?: "",
                        onValueChange = { localFilter = localFilter.copy(minAmount = it.toBigDecimalOrNull()) },
                        label = { Text("Min") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = localFilter.maxAmount?.toString() ?: "",
                        onValueChange = { localFilter = localFilter.copy(maxAmount = it.toBigDecimalOrNull()) },
                        label = { Text("Max") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                    )
                }
            }

            // Categories (Multi-select)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("CATEGORIES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allCategories.forEach { category ->
                        FilterChip(
                            selected = category.id in localFilter.categoryIds,
                            onClick = {
                                val current = localFilter.categoryIds.toMutableList()
                                if (category.id in current) current.remove(category.id) else current.add(category.id)
                                localFilter = localFilter.copy(categoryIds = current)
                            },
                            label = { Text(category.name) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Accounts (Multi-select)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("ACCOUNTS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    accounts.forEach { account ->
                        FilterChip(
                            selected = account.id in localFilter.accountIds,
                            onClick = {
                                val current = localFilter.accountIds.toMutableList()
                                if (account.id in current) current.remove(account.id) else current.add(account.id)
                                localFilter = localFilter.copy(accountIds = current)
                            },
                            label = { Text(account.name) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
                Button(
                    onClick = {
                        onApplyFilter(localFilter)
                        onClose()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Apply Filter")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
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
