package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.ui.components.AeonHierarchicalHybridSelector
import com.osscabinet.ossWallet.ui.components.AeonHybridSelector
import com.osscabinet.ossWallet.ui.components.AeonSelectMode
import com.osscabinet.ossWallet.ui.components.AppIconMap
import com.osscabinet.ossWallet.ui.components.AeonFullScreenDialog
import com.osscabinet.ossWallet.ui.components.AeonSelectionScreen
import com.osscabinet.ossWallet.ui.components.entry.*
import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.util.fuzzyMatch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterFields(
    filter: TransactionFilter,
    onFilterChange: (TransactionFilter) -> Unit,
    accounts: List<Account>,
    allCategories: List<Category>,
    allTransactions: List<Transaction> = emptyList(),
    showAccountFilter: Boolean = true,
    // Unfiltered data for dropdowns
    allAvailableCurrencies: List<String> = emptyList(),
    // Callback for immediate apply with custom debounce time
    onImmediateApply: ((TransactionFilter, Long) -> Unit)? = null,
    filterRules: CardFilterRules = CardFilterRules.DEFAULT
) {
    var activeSheet by remember { mutableStateOf<String?>(null) }

    // Sheets for multi-selection
    when (activeSheet) {
        "Accounts" -> {
            AeonAccountSelection(
                accounts = accounts,
                selectedAccountIds = filter.accountIds.toSet(),
                onSelectionChange = { ids ->
                    onFilterChange(filter.copy(accountIds = ids.toList()))
                },
                onDismiss = { activeSheet = null },
                mode = AeonSelectMode.MULTI,
                title = "Filter Accounts"
            )
        }
        "Categories" -> {
            AeonCategorySelection(
                categories = allCategories,
                selectedCategoryIds = filter.categoryIds.toSet(),
                onSelectionChange = { ids ->
                    onFilterChange(filter.copy(categoryIds = ids.toList()))
                },
                onDismiss = { activeSheet = null },
                mode = AeonSelectMode.MULTI,
                title = "Filter Categories"
            )
        }
        "Currencies" -> {
            val availableCurrencies = if (allAvailableCurrencies.isNotEmpty()) {
                allAvailableCurrencies
            } else {
                accounts.flatMap { account ->
                    if (account.allowedCurrencies.isEmpty()) {
                        listOf(account.currencyCode)
                    } else {
                        account.allowedCurrencies
                    }
                }.distinct().sorted()
            }
            AeonSelectionSheet(
                title = "Filter Currencies",
                onDismiss = { activeSheet = null },
                items = availableCurrencies,
                selectedIds = filter.currencies.toSet(),
                onSelectionChange = { ids ->
                    onFilterChange(filter.copy(currencies = ids.toList()))
                },
                idMapper = { it },
                labelMapper = { it },
                mode = AeonSelectMode.MULTI
            )
        }
        "PaymentMethods" -> {
            AeonPaymentMethodSelection(
                selectedMethods = filter.paymentMethods.toSet(),
                onSelectionChange = { methods ->
                    onFilterChange(filter.copy(paymentMethods = methods.toList()))
                },
                onDismiss = { activeSheet = null },
                mode = AeonSelectMode.MULTI,
                title = "Filter Payment Methods"
            )
        }
        "Statuses" -> {
            AeonStatusSelection(
                selectedStatuses = filter.statuses.toSet(),
                onSelectionChange = { statuses ->
                    onFilterChange(filter.copy(statuses = statuses.toList()))
                },
                onDismiss = { activeSheet = null },
                mode = AeonSelectMode.MULTI,
                title = "Filter Statuses"
            )
        }
        "LinkedTransactions" -> {
            AeonLinkedTransactionSelection(
                transactions = allTransactions,
                currentTransactionId = null,
                linkedIds = filter.linkedTransactionIds.toSet(),
                accounts = accounts,
                categories = allCategories,
                onLinkedTransactionsChanged = { ids ->
                    onFilterChange(filter.copy(linkedTransactionIds = ids.toList()))
                },
                onDismiss = { activeSheet = null }
            )
        }
        "MinAmount" -> {
            AeonCalculatorSheet(
                currentAmount = filter.minAmount?.toString() ?: "",
                onAmountChanged = { onFilterChange(filter.copy(minAmount = it.ifBlank { null }?.toBigDecimalOrNull())) },
                onDismiss = { activeSheet = null },
                title = "Minimum Amount"
            )
        }
        "MaxAmount" -> {
            AeonCalculatorSheet(
                currentAmount = filter.maxAmount?.toString() ?: "",
                onAmountChanged = { onFilterChange(filter.copy(maxAmount = it.ifBlank { null }?.toBigDecimalOrNull())) },
                onDismiss = { activeSheet = null },
                title = "Maximum Amount"
            )
        }
        "WarrantyStart" -> {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = filter.warrantyExpirationStart)
            DatePickerDialog(
                onDismissRequest = { activeSheet = null },
                confirmButton = {
                    TextButton(onClick = {
                        onFilterChange(filter.copy(warrantyExpirationStart = datePickerState.selectedDateMillis))
                        activeSheet = null
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { activeSheet = null }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
        "WarrantyEnd" -> {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = filter.warrantyExpirationEnd)
            DatePickerDialog(
                onDismissRequest = { activeSheet = null },
                confirmButton = {
                    TextButton(onClick = {
                        onFilterChange(filter.copy(warrantyExpirationEnd = datePickerState.selectedDateMillis))
                        activeSheet = null
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { activeSheet = null }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        // Search (Note/Payee)
        if (filterRules.allowSearch) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "SEARCH",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CompactTextField(
                        value = filter.queryNote ?: "",
                        onValueChange = {
                            val newFilter = filter.copy(queryNote = it.ifBlank { null })
                            onFilterChange(newFilter)
                            onImmediateApply?.invoke(newFilter, 500)
                        },
                        placeholder = "Note...",
                        modifier = Modifier.weight(1f),
                        leadingIcon = Icons.Default.Search
                    )
                    CompactTextField(
                        value = filter.queryPayee ?: "",
                        onValueChange = {
                            val newFilter = filter.copy(queryPayee = it.ifBlank { null })
                            onFilterChange(newFilter)
                            onImmediateApply?.invoke(newFilter, 500)
                        },
                        placeholder = "Payee...",
                        modifier = Modifier.weight(1f),
                        leadingIcon = Icons.Default.Search
                    )
                }
            }
        }

        // Primary Context Grid (Type and Accounts/Categories)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "CONTEXT",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
            
            // 2-Column Grid for Primary Dimensions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Type Card
                ContextCard(
                    title = "Type",
                    value = filter.transactionType?.displayName ?: "All Types",
                    icon = when (filter.transactionType) {
                        TransactionType.INCOME -> Icons.AutoMirrored.Filled.TrendingUp
                        TransactionType.EXPENSE -> Icons.AutoMirrored.Filled.TrendingDown
                        else -> Icons.Default.FilterList
                    },
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val nextType = when (filter.transactionType) {
                            null -> TransactionType.EXPENSE
                            TransactionType.EXPENSE -> TransactionType.INCOME
                            TransactionType.INCOME -> TransactionType.TRANSFER
                            else -> null
                        }
                        onFilterChange(filter.copy(transactionType = nextType))
                    }
                )

                // Accounts Card
                if (filterRules.allowAccountSelection && showAccountFilter) {
                    ContextCard(
                        title = "Accounts",
                        value = if (filter.accountIds.isEmpty()) "All" else "${filter.accountIds.size} Selected",
                        icon = Icons.Default.AccountBalance,
                        modifier = Modifier.weight(1f),
                        onClick = { activeSheet = "Accounts" }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Categories Card
                if (filterRules.allowCategorySelection) {
                    ContextCard(
                        title = "Categories",
                        value = if (filter.categoryIds.isEmpty()) "All" else "${filter.categoryIds.size} Selected",
                        icon = Icons.Default.Category,
                        modifier = Modifier.weight(1f),
                        onClick = { activeSheet = "Categories" }
                    )
                }

                // Currency Card
                if (filterRules.allowCurrencySelection) {
                    ContextCard(
                        title = "Currencies",
                        value = if (filter.currencies.isEmpty()) "All" else "${filter.currencies.size} Selected",
                        icon = Icons.Default.MonetizationOn,
                        modifier = Modifier.weight(1f),
                        onClick = { activeSheet = "Currencies" }
                    )
                }
            }

            // Future Transactions Toggle
            Surface(
                onClick = { onFilterChange(filter.copy(excludeFuture = !filter.excludeFuture)) },
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = androidx.compose.foundation.BorderStroke(
                    0.5.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Update,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Show Future Transactions",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                    Switch(
                        checked = !filter.excludeFuture,
                        onCheckedChange = { onFilterChange(filter.copy(excludeFuture = !it)) }
                    )
                }
            }
        }

        // Status
        if (filterRules.allowStatusSelection) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "STATUS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
                ContextCard(
                    title = "Statuses",
                    value = if (filter.statuses.isEmpty()) "All Statuses" else "${filter.statuses.size} Selected",
                    icon = Icons.AutoMirrored.Rounded.FactCheck,
                    onClick = { activeSheet = "Statuses" }
                )
            }
        }

        // Amount Range
        if (filterRules.allowAmountRange) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "AMOUNT RANGE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ContextCard(
                        title = "Min Amount",
                        value = filter.minAmount?.toString() ?: "No Min",
                        icon = Icons.Rounded.Remove,
                        modifier = Modifier.weight(1f),
                        onClick = { activeSheet = "MinAmount" }
                    )
                    ContextCard(
                        title = "Max Amount",
                        value = filter.maxAmount?.toString() ?: "No Max",
                        icon = Icons.Rounded.Add,
                        modifier = Modifier.weight(1f),
                        onClick = { activeSheet = "MaxAmount" }
                    )
                }
            }
        }

        // Methods Card (Full Width)
        if (filterRules.allowPaymentMethodSelection) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "PAYMENT METHODS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
                ContextCard(
                    title = "Methods",
                    value = if (filter.paymentMethods.isEmpty()) "Any Method" else "${filter.paymentMethods.size} Selected",
                    icon = Icons.Default.Payments,
                    onClick = { activeSheet = "PaymentMethods" }
                )
            }
        }

        // Linked Records Card (Full Width)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "LINKED RECORDS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
            ContextCard(
                title = "Linked",
                value = if (filter.linkedTransactionIds.isEmpty()) "None" else "${filter.linkedTransactionIds.size} Linked",
                icon = Icons.Rounded.Link,
                onClick = { activeSheet = "LinkedTransactions" }
            )
        }

        // Warranty Expiry Range
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "WARRANTY EXPIRY",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
            AeonDateRangeCard(
                startDateMillis = filter.warrantyExpirationStart,
                endDateMillis = filter.warrantyExpirationEnd,
                onStartClick = { activeSheet = "WarrantyStart" },
                onEndClick = { activeSheet = "WarrantyEnd" },
                startLabel = "EXPIRY FROM",
                endLabel = "EXPIRY TO"
            )
        }
    }
}

@Composable
fun ContextCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(84.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    title.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                        fontSize = 10.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Text(
                    value,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
    enabled: Boolean = true
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
                enabled = enabled,
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
            if (value.isNotEmpty() && enabled) {
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
