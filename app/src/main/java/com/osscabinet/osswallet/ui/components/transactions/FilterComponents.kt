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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
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
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.util.fuzzyMatch
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.ThemePreviews
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterFields(
    filter: TransactionFilter,
    onFilterChange: (TransactionFilter) -> Unit,
    accounts: List<Account>,
    allCategories: List<Category>,
    allTags: List<Tag> = emptyList(),
    onSearchTransactions: (String) -> Flow<List<Transaction>>,
    showAccountFilter: Boolean = true,
    // Unfiltered data for dropdowns
    allAvailableCurrencies: List<String> = emptyList(),
    // Callback for immediate apply with custom debounce time
    onImmediateApply: ((TransactionFilter, Long) -> Unit)? = null,
    onAddTag: (String, Int) -> Unit = { _, _ -> },
    filterRules: CardFilterRules = CardFilterRules.DEFAULT
) {
    var activeSheet by rememberSaveable { mutableStateOf<String?>(null) }

    // Sheets for multi-selection
    when (activeSheet) {
        "Accounts" -> {
            AeonAccountSelection(
                accounts = accounts,
                selectedAccountIds = filter.accountIds.toSet(),
                onSelectionChange = { ids ->
                    onFilterChange(filter.copy(accountIds = ids.toSet()))
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
                    onFilterChange(filter.copy(categoryIds = ids.toSet()))
                },
                onDismiss = { activeSheet = null },
                mode = AeonSelectMode.MULTI,
                title = "Filter Categories",
                startExpanded = false
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
                    onFilterChange(filter.copy(currencies = ids.toSet()))
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
                    onFilterChange(filter.copy(paymentMethods = methods.toSet()))
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
                    onFilterChange(filter.copy(statuses = statuses.toSet()))
                },
                onDismiss = { activeSheet = null },
                mode = AeonSelectMode.MULTI,
                title = "Filter Statuses"
            )
        }
        "Types" -> {
            AeonSelectionSheet(
                title = "Transaction Types",
                onDismiss = { activeSheet = null },
                items = TransactionType.entries.filter { it != TransactionType.TRANSFER },
                selectedIds = filter.transactionTypes.toSet(),
                onSelectionChange = { ids ->
                    onFilterChange(filter.copy(transactionTypes = ids.toSet()))
                },
                idMapper = { it },
                labelMapper = { it.displayName },
                mode = AeonSelectMode.MULTI
            )
        }
        "LinkedTransactions" -> {
            AeonLinkedTransactionSelection(
                onSearch = onSearchTransactions,
                currentTransactionId = null,
                linkedIds = filter.linkedTransactionIds.toSet(),
                accounts = accounts,
                categories = allCategories,
                    onLinkedTransactionsChanged = { ids ->
                    onFilterChange(filter.copy(linkedTransactionIds = ids.toSet()))
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            onFilterChange(filter.copy(warrantyExpirationStart = null))
                            activeSheet = null
                        }) { Text("Clear") }
                        TextButton(onClick = {
                            onFilterChange(filter.copy(warrantyExpirationStart = datePickerState.selectedDateMillis))
                            activeSheet = null
                        }) { Text("OK") }
                    }
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            onFilterChange(filter.copy(warrantyExpirationEnd = null))
                            activeSheet = null
                        }) { Text("Clear") }
                        TextButton(onClick = {
                            onFilterChange(filter.copy(warrantyExpirationEnd = datePickerState.selectedDateMillis))
                            activeSheet = null
                        }) { Text("OK") }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { activeSheet = null }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
        "Tags" -> {
            AeonTagSelectionDialog(
                tags = allTags,
                selectedTags = filter.tags.toSet(),
                onTagsSelected = { tags ->
                    onFilterChange(filter.copy(tags = tags.toSet()))
                },
                onAddTag = onAddTag,
                onDismiss = { activeSheet = null },
                allowCreation = filterRules.allowTagCreation
            )
        }
    }

    Column(
        modifier = Modifier.padding(bottom = AeonTheme.dimensions.spacingLg),
        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg)
    ) {
        // Search (Note/Payee/Tags)
        if (filterRules.allowSearch) {
            EntrySection(
                title = "SEARCH",
                icon = Icons.Default.Search
            ) {
                // Note Row
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "NOTE",
                            style = AppTextStyles.transactionSwipeAction.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                fontSize = 10.sp
                            ),
                            color = AppColors.contentSecondary.copy(alpha = 0.6f)
                        )
                        
                        Surface(
                            onClick = { onFilterChange(filter.copy(isNoteRegex = !filter.isNoteRegex)) },
                            shape = RoundedCornerShape(6.dp),
                            color = if (filter.isNoteRegex) AppColors.primary.copy(alpha = 0.12f) else Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(
                                0.5.dp,
                                if (filter.isNoteRegex) AppColors.primary.copy(alpha = 0.3f) else AppColors.divider.copy(alpha = 0.1f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.Code, 
                                    null, 
                                    modifier = Modifier.size(12.dp), 
                                    tint = if (filter.isNoteRegex) AppColors.primary else AppColors.contentSecondary
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "REGEX", 
                                    style = AppTextStyles.transactionStatusMicro, 
                                    color = if (filter.isNoteRegex) AppColors.primary else AppColors.contentSecondary
                                )
                            }
                        }
                    }
                    
                    CompactTextField(
                        value = filter.queryNote ?: "",
                        onValueChange = {
                            val newFilter = filter.copy(queryNote = it.ifBlank { null })
                            onFilterChange(newFilter)
                            onImmediateApply?.invoke(newFilter, 500)
                        },
                        placeholder = if (filter.isNoteRegex) "Enter note regex pattern..." else "Search by note keyword...",
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = Icons.Default.Description
                    )
                }

                // Payee Row
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "PAYEE",
                            style = AppTextStyles.transactionSwipeAction.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                fontSize = 10.sp
                            ),
                            color = AppColors.contentSecondary.copy(alpha = 0.6f)
                        )
                        
                        Surface(
                            onClick = { onFilterChange(filter.copy(isPayeeRegex = !filter.isPayeeRegex)) },
                            shape = RoundedCornerShape(6.dp),
                            color = if (filter.isPayeeRegex) AppColors.secondary.copy(alpha = 0.12f) else Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(
                                0.5.dp,
                                if (filter.isPayeeRegex) AppColors.secondary.copy(alpha = 0.3f) else AppColors.divider.copy(alpha = 0.1f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.Code, 
                                    null, 
                                    modifier = Modifier.size(12.dp), 
                                    tint = if (filter.isPayeeRegex) AppColors.secondary else AppColors.contentSecondary
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "REGEX", 
                                    style = AppTextStyles.transactionStatusMicro, 
                                    color = if (filter.isPayeeRegex) AppColors.secondary else AppColors.contentSecondary
                                )
                            }
                        }
                    }
                    
                    CompactTextField(
                        value = filter.queryPayee ?: "",
                        onValueChange = {
                            val newFilter = filter.copy(queryPayee = it.ifBlank { null })
                            onFilterChange(newFilter)
                            onImmediateApply?.invoke(newFilter, 500)
                        },
                        placeholder = if (filter.isPayeeRegex) "Enter payee regex pattern..." else "Search by payee keyword...",
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = Icons.Rounded.Person
                    )
                }

                // Tags (Moved from Context) - Similar to Transaction Entry Screen
                if (filterRules.allowTagSelection) {
                    EntryTagSection(
                        selectedNames = filter.tags,
                        allAvailableTags = allTags,
                        onShowTagSheet = { activeSheet = "Tags" },
                        onRemoveTag = { tagName ->
                            onFilterChange(filter.copy(tags = filter.tags - tagName))
                        },
                        label = "Tags"
                    )
                }
            }
        }

        // Primary Context Grid (Type, Accounts, Categories, Currencies, Status, Methods)
        AeonContextGrid {
            EntryActionRow {
                // Type Card
                val isTypeLocked = filterRules.forcedTransactionTypes != null
                EntryActionCard(
                    icon = if (isTypeLocked) Icons.Default.Lock
                    else if (filter.transactionTypes.size == 1) {
                        when (filter.transactionTypes.first()) {
                            TransactionType.INCOME -> Icons.AutoMirrored.Filled.TrendingUp
                            TransactionType.EXPENSE -> Icons.AutoMirrored.Filled.TrendingDown
                            else -> Icons.Default.FilterList
                        }
                    } else Icons.Default.FilterList,
                    label = "TYPES",
                    value = if (isTypeLocked) {
                        filterRules.forcedTransactionTypes!!.joinToString(" & ") { it.replaceFirstChar { c -> c.uppercaseChar() } }
                    } else when {
                        filter.transactionTypes.isEmpty() -> "All"
                        filter.transactionTypes.size == 1 -> filter.transactionTypes.first().displayName
                        else -> "${filter.transactionTypes.size} Selected"
                    },
                    isSelected = filter.transactionTypes.isNotEmpty() || isTypeLocked,
                    modifier = Modifier.weight(1f),
                    onClick = { if (!isTypeLocked) activeSheet = "Types" },
                    iconContainerColor = if (isTypeLocked) AppColors.formIconContainerPrimary.copy(alpha = 0.05f)
                                         else AppColors.formIconContainerPrimary.copy(alpha = 0.1f),
                    iconTint = if (isTypeLocked) AppColors.primary.copy(alpha = 0.4f) else AppColors.primary
                )

                // Accounts Card
                if (filterRules.allowAccountSelection && showAccountFilter) {
                    EntryActionCard(
                        icon = Icons.Default.AccountBalance,
                        label = "ACCOUNTS",
                        value = if (filter.accountIds.isEmpty()) "All" else "${filter.accountIds.size} Selected",
                        isSelected = filter.accountIds.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        onClick = { activeSheet = "Accounts" },
                        iconContainerColor = AppColors.formIconContainerPrimary.copy(alpha = 0.1f),
                        iconTint = AppColors.primary
                    )
                }
            }

            EntryActionRow {
                // Categories Card
                if (filterRules.allowCategorySelection) {
                    EntryActionCard(
                        icon = Icons.Default.Category,
                        label = "CATEGORIES",
                        value = if (filter.categoryIds.isEmpty()) "All" else "${filter.categoryIds.size} Selected",
                        isSelected = filter.categoryIds.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        onClick = { activeSheet = "Categories" },
                        iconContainerColor = AppColors.formIconContainerTertiary.copy(alpha = 0.1f),
                        iconTint = AppColors.tertiary
                    )
                }

                // Currency Card
                if (filterRules.allowCurrencySelection) {
                    EntryActionCard(
                        icon = Icons.Default.MonetizationOn,
                        label = "CURRENCIES",
                        value = if (filter.currencies.isEmpty()) "All" else "${filter.currencies.size} Selected",
                        isSelected = filter.currencies.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        onClick = { activeSheet = "Currencies" },
                        iconContainerColor = AppColors.formIconContainerPrimary.copy(alpha = 0.1f),
                        iconTint = AppColors.primary
                    )
                }
            }

            // Status & Methods (Moved from separate section)
            if (filterRules.allowStatusSelection || filterRules.allowPaymentMethodSelection) {
                EntryActionRow {
                    if (filterRules.allowStatusSelection) {
                        EntryActionCard(
                            icon = Icons.AutoMirrored.Rounded.FactCheck,
                            label = "STATUS",
                            value = if (filter.statuses.isEmpty()) "All Statuses" else "${filter.statuses.size} Selected",
                            isSelected = filter.statuses.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                            onClick = { activeSheet = "Statuses" },
                            iconContainerColor = AppColors.secondary.copy(alpha = 0.1f),
                            iconTint = AppColors.secondary
                        )
                    }

                    if (filterRules.allowPaymentMethodSelection) {
                        EntryActionCard(
                            icon = Icons.Default.Payments,
                            label = "METHODS",
                            value = if (filter.paymentMethods.isEmpty()) "Any Method" else "${filter.paymentMethods.size} Selected",
                            isSelected = filter.paymentMethods.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                            onClick = { activeSheet = "PaymentMethods" },
                            iconContainerColor = AppColors.primary.copy(alpha = 0.1f),
                            iconTint = AppColors.primary
                        )
                    }
                }
            }
        }

        // Amount & Records
        EntrySection(
            title = "AMOUNT & RECORDS",
            icon = Icons.Rounded.AttachMoney
        ) {
            if (filterRules.allowAmountRange) {
                AeonContextGrid {
                    EntryActionRow {
                        EntryActionCard(
                            icon = Icons.Rounded.Remove,
                            label = "MIN AMOUNT",
                            value = filter.minAmount?.toString() ?: "No Min",
                            isSelected = filter.minAmount != null,
                            modifier = Modifier.weight(1f),
                            onClick = { activeSheet = "MinAmount" }
                        )
                        EntryActionCard(
                            icon = Icons.Rounded.Add,
                            label = "MAX AMOUNT",
                            value = filter.maxAmount?.toString() ?: "No Max",
                            isSelected = filter.maxAmount != null,
                            modifier = Modifier.weight(1f),
                            onClick = { activeSheet = "MaxAmount" }
                        )
                    }
                }
            }

            EntryActionCard(
                icon = Icons.Rounded.Link,
                label = "LINKED RECORDS",
                value = if (filter.linkedTransactionIds.isEmpty()) "None Linked" else "${filter.linkedTransactionIds.size} Linked",
                isSelected = filter.linkedTransactionIds.isNotEmpty(),
                onClick = { activeSheet = "LinkedTransactions" },
                iconContainerColor = AppColors.tertiary.copy(alpha = 0.1f),
                iconTint = AppColors.tertiary
            )
        }

        // Warranty Section
        EntrySection(
            title = "WARRANTY",
            icon = Icons.Rounded.VerifiedUser
        ) {
            AeonContextGrid {
                EntryActionCard(
                    icon = Icons.Rounded.Verified,
                    label = "PROTECTION STATUS",
                    value = when (filter.hasWarranty) {
                        null -> "Any Status"
                        true -> "Has Warranty"
                        false -> "No Warranty"
                    },
                    isSelected = filter.hasWarranty != null,
                    onClick = {
                        val next = when (filter.hasWarranty) {
                            null -> true
                            true -> false
                            false -> null
                        }
                        onFilterChange(filter.copy(hasWarranty = next))
                    },
                    iconContainerColor = AppColors.secondary.copy(alpha = 0.1f),
                    iconTint = AppColors.secondary
                )

                if (filter.hasWarranty == true) {
                    EntryActionRow {
                        EntryActionCard(
                            icon = Icons.Rounded.CalendarToday,
                            label = "WARRANTY START",
                            value = filter.warrantyExpirationStart?.let { 
                                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(it))
                            } ?: "Select",
                            onClick = { activeSheet = "WarrantyStart" },
                            modifier = Modifier.weight(1f)
                        )
                        EntryActionCard(
                            icon = Icons.Rounded.CalendarToday,
                            label = "WARRANTY END",
                            value = filter.warrantyExpirationEnd?.let { 
                                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(it))
                            } ?: "Select",
                            onClick = { activeSheet = "WarrantyEnd" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@ThemePreviews
@Composable
fun FilterFieldsPreview() {
    OssWalletTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            FilterFields(
                filter = TransactionFilter(),
                onFilterChange = {},
                accounts = emptyList(),
                allCategories = emptyList(),
                onSearchTransactions = { _ -> flowOf(emptyList()) }
            )
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
        modifier = modifier.height(AeonTheme.dimensions.iconSize2xl),
        shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
        color = AppColors.insetSurface.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AeonTheme.dimensions.spacingMd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(
                    leadingIcon,
                    contentDescription = null,
                    tint = AppColors.contentSecondary,
                    modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm)
                )
                Spacer(modifier = Modifier.width(AeonTheme.dimensions.spacingSm))
            }
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                enabled = enabled,
                textStyle = AppTextStyles.transactionTitle.copy(color = AppColors.contentPrimary),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(AppColors.primary),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            style = AppTextStyles.transactionTitle,
                            color = AppColors.contentSecondary.copy(alpha = 0.6f)
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
