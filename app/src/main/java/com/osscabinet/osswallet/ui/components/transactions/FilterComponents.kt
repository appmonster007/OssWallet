package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.rounded.FactCheck
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.ui.components.AeonSelectMode
import com.osscabinet.ossWallet.ui.components.entry.AeonAccountSelection
import com.osscabinet.ossWallet.ui.components.entry.AeonCalculatorSheet
import com.osscabinet.ossWallet.ui.components.entry.AeonCategorySelection
import com.osscabinet.ossWallet.ui.components.entry.AeonContextGrid
import com.osscabinet.ossWallet.ui.components.entry.AeonLinkedTransactionSelection
import com.osscabinet.ossWallet.ui.components.entry.AeonPaymentMethodSelection
import com.osscabinet.ossWallet.ui.components.entry.AeonSelectionSheet
import com.osscabinet.ossWallet.ui.components.entry.AeonStatusSelection
import com.osscabinet.ossWallet.ui.components.entry.AeonTagSelectionDialog
import com.osscabinet.ossWallet.ui.components.entry.EntryActionCard
import com.osscabinet.ossWallet.ui.components.entry.EntryActionRow
import com.osscabinet.ossWallet.ui.components.entry.EntrySection
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.ThemePreviews
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterFields(
    filter: TransactionFilter,
    onFilterChange: (TransactionFilter) -> Unit,
    accounts: List<Account>,
    allCategories: List<Category>,
    allTags: List<Tag> = emptyList(),
    onSearchTransactions: (String) -> Flow<List<Transaction>>,
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
                allTags = allTags,
                onLinkedTransactionsChanged = { ids ->
                    onFilterChange(filter.copy(linkedTransactionIds = ids.toSet()))
                },
                onDismiss = { activeSheet = null }
            )
        }

        "MinAmount" -> {
            AeonCalculatorSheet(
                currentAmount = filter.minAmount?.toString() ?: "",
                onAmountChanged = {
                    onFilterChange(filter.copy(minAmount = it.ifBlank { null }
                        ?.toBigDecimalOrNull()))
                },
                onDismiss = { activeSheet = null },
                title = "Minimum Amount"
            )
        }

        "MaxAmount" -> {
            AeonCalculatorSheet(
                currentAmount = filter.maxAmount?.toString() ?: "",
                onAmountChanged = {
                    onFilterChange(filter.copy(maxAmount = it.ifBlank { null }
                        ?.toBigDecimalOrNull()))
                },
                onDismiss = { activeSheet = null },
                title = "Maximum Amount"
            )
        }

        "WarrantyStart" -> {
            val datePickerState =
                rememberDatePickerState(initialSelectedDateMillis = filter.warrantyExpirationStart)
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
            val datePickerState =
                rememberDatePickerState(initialSelectedDateMillis = filter.warrantyExpirationEnd)
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

    val accountMap = remember(accounts) { accounts.associate { it.id to it.name } }

    Column(
        modifier = Modifier.padding(bottom = AeonTheme.dimensions.spacingLg),
        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg)
    ) {
        // ... (skipping some code for context)
        // Primary Context Grid (Type, Accounts, Categories, Currencies, Status, Methods)
        AeonContextGrid {
            EntryActionRow {
                // Type Card
                val isTypeLocked = filterRules.typeRule.isLocked
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
                        filterRules.typeRule.lockedValues.joinToString(" & ") { it.displayName }
                    } else when {
                        filter.transactionTypes.isEmpty() -> "All"
                        filter.transactionTypes.size == 1 -> filter.transactionTypes.first().displayName
                        else -> "${filter.transactionTypes.size} Selected"
                    },
                    isSelected = filter.transactionTypes.isNotEmpty() || isTypeLocked,
                    modifier = Modifier.weight(1f),
                    onClick = { if (!isTypeLocked) activeSheet = "Types" },
                    iconContainerColor = if (isTypeLocked) AppColors.formIconContainerPrimary.copy(
                        alpha = 0.05f
                    )
                    else AppColors.formIconContainerPrimary.copy(alpha = 0.1f),
                    iconTint = if (isTypeLocked) AppColors.primary.copy(alpha = 0.4f) else AppColors.primary
                )

                // Accounts Card
                val isAccountLocked =
                    !filterRules.allowAccountSelection && filter.accountIds.isNotEmpty()
                if (filterRules.allowAccountSelection || isAccountLocked) {
                    EntryActionCard(
                        icon = if (isAccountLocked) Icons.Default.Lock else Icons.Default.AccountBalance,
                        label = "ACCOUNTS",
                        value = if (isAccountLocked) {
                            filter.accountIds.mapNotNull { accountMap[it] }.joinToString(", ")
                        } else if (filter.accountIds.isEmpty()) "All" else "${filter.accountIds.size} Selected",
                        isSelected = filter.accountIds.isNotEmpty(),
                        enabled = !isAccountLocked,
                        modifier = Modifier.weight(1f),
                        onClick = { if (!isAccountLocked) activeSheet = "Accounts" },
                        iconContainerColor = if (isAccountLocked) AppColors.formIconContainerPrimary.copy(
                            alpha = 0.05f
                        )
                        else AppColors.formIconContainerPrimary.copy(alpha = 0.1f),
                        iconTint = if (isAccountLocked) AppColors.primary.copy(alpha = 0.4f) else AppColors.primary
                    )
                }
            }

            EntryActionRow {
                // Categories Card
                val isCategoryLocked =
                    !filterRules.allowCategorySelection && filter.categoryIds.isNotEmpty()
                if (filterRules.allowCategorySelection || isCategoryLocked) {
                    EntryActionCard(
                        icon = if (isCategoryLocked) Icons.Default.Lock else Icons.Default.Category,
                        label = "CATEGORIES",
                        value = if (isCategoryLocked) {
                            val categoryNames =
                                filter.categoryIds.mapNotNull { id -> allCategories.find { it.id == id }?.name }
                            if (categoryNames.size > 2) "${categoryNames.size} Categories" else categoryNames.joinToString(
                                ", "
                            )
                        } else if (filter.categoryIds.isEmpty()) "All" else "${filter.categoryIds.size} Selected",
                        isSelected = filter.categoryIds.isNotEmpty(),
                        enabled = !isCategoryLocked,
                        modifier = Modifier.weight(1f),
                        onClick = { if (!isCategoryLocked) activeSheet = "Categories" },
                        iconContainerColor = if (isCategoryLocked) AppColors.formIconContainerTertiary.copy(
                            alpha = 0.05f
                        )
                        else AppColors.formIconContainerTertiary.copy(alpha = 0.1f),
                        iconTint = if (isCategoryLocked) AppColors.tertiary.copy(alpha = 0.4f) else AppColors.tertiary
                    )
                }

                // Currency Card
                val isCurrencyLocked =
                    !filterRules.allowCurrencySelection && filter.currencies.isNotEmpty()
                if (filterRules.allowCurrencySelection || isCurrencyLocked) {
                    EntryActionCard(
                        icon = if (isCurrencyLocked) Icons.Default.Lock else Icons.Default.MonetizationOn,
                        label = "CURRENCIES",
                        value = if (isCurrencyLocked) {
                            filter.currencies.joinToString(", ")
                        } else if (filter.currencies.isEmpty()) "All" else "${filter.currencies.size} Selected",
                        isSelected = filter.currencies.isNotEmpty(),
                        enabled = !isCurrencyLocked,
                        modifier = Modifier.weight(1f),
                        onClick = { if (!isCurrencyLocked) activeSheet = "Currencies" },
                        iconContainerColor = if (isCurrencyLocked) AppColors.formIconContainerPrimary.copy(
                            alpha = 0.05f
                        )
                        else AppColors.formIconContainerPrimary.copy(alpha = 0.1f),
                        iconTint = if (isCurrencyLocked) AppColors.primary.copy(alpha = 0.4f) else AppColors.primary
                    )
                }
            }

            // Status & Methods
            val isStatusLocked = !filterRules.allowStatusSelection && filter.statuses.isNotEmpty()
            val isMethodLocked =
                !filterRules.allowPaymentMethodSelection && filter.paymentMethods.isNotEmpty()

            if (filterRules.allowStatusSelection || isStatusLocked || filterRules.allowPaymentMethodSelection || isMethodLocked) {
                EntryActionRow {
                    if (filterRules.allowStatusSelection || isStatusLocked) {
                        EntryActionCard(
                            icon = if (isStatusLocked) Icons.Default.Lock else Icons.AutoMirrored.Rounded.FactCheck,
                            label = "STATUS",
                            value = if (isStatusLocked) {
                                filter.statuses.joinToString(", ") { it.displayName }
                            } else if (filter.statuses.isEmpty()) "All Statuses" else "${filter.statuses.size} Selected",
                            isSelected = filter.statuses.isNotEmpty(),
                            enabled = !isStatusLocked,
                            modifier = Modifier.weight(1f),
                            onClick = { if (!isStatusLocked) activeSheet = "Statuses" },
                            iconContainerColor = if (isStatusLocked) AppColors.secondary.copy(alpha = 0.05f)
                            else AppColors.secondary.copy(alpha = 0.1f),
                            iconTint = if (isStatusLocked) AppColors.secondary.copy(alpha = 0.4f) else AppColors.secondary
                        )
                    }

                    if (filterRules.allowPaymentMethodSelection || isMethodLocked) {
                        EntryActionCard(
                            icon = if (isMethodLocked) Icons.Default.Lock else Icons.Default.Payments,
                            label = "METHODS",
                            value = if (isMethodLocked) {
                                filter.paymentMethods.joinToString(", ") { it.name }
                            } else if (filter.paymentMethods.isEmpty()) "Any Method" else "${filter.paymentMethods.size} Selected",
                            isSelected = filter.paymentMethods.isNotEmpty(),
                            enabled = !isMethodLocked,
                            modifier = Modifier.weight(1f),
                            onClick = { if (!isMethodLocked) activeSheet = "PaymentMethods" },
                            iconContainerColor = if (isMethodLocked) AppColors.primary.copy(alpha = 0.05f)
                            else AppColors.primary.copy(alpha = 0.1f),
                            iconTint = if (isMethodLocked) AppColors.primary.copy(alpha = 0.4f) else AppColors.primary
                        )
                    }
                }
            }
        }

        // Search & Tags
        val isSearchLocked =
            !filterRules.allowSearch && (filter.queryNote != null || filter.queryPayee != null)
        val isTagLocked = !filterRules.allowTagSelection && filter.tags.isNotEmpty()

        if (filterRules.allowSearch || isSearchLocked || filterRules.allowTagSelection || isTagLocked) {
            EntrySection(
                title = "SEARCH & TAGS",
                icon = Icons.Rounded.Search
            ) {
                if (filterRules.allowSearch || isSearchLocked) {
                    Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)) {
                        CompactTextField(
                            value = filter.queryNote ?: "",
                            onValueChange = { onFilterChange(filter.copy(queryNote = it.ifBlank { null })) },
                            placeholder = "Search note...",
                            leadingIcon = Icons.Default.Subtitles,
                            enabled = !isSearchLocked
                        )
                        CompactTextField(
                            value = filter.queryPayee ?: "",
                            onValueChange = { onFilterChange(filter.copy(queryPayee = it.ifBlank { null })) },
                            placeholder = "Search payee...",
                            leadingIcon = Icons.Default.Person,
                            enabled = !isSearchLocked
                        )
                    }
                }

                if (filterRules.allowTagSelection || isTagLocked) {
                    EntryActionCard(
                        icon = if (isTagLocked) Icons.Default.Lock else Icons.Default.LocalOffer,
                        label = "TAGS",
                        value = if (isTagLocked) {
                            filter.tags.joinToString(", ")
                        } else if (filter.tags.isEmpty()) "No Tags" else "${filter.tags.size} Tags",
                        isSelected = filter.tags.isNotEmpty(),
                        enabled = !isTagLocked,
                        onClick = { if (!isTagLocked) activeSheet = "Tags" },
                        iconContainerColor = if (isTagLocked) AppColors.formIconContainerPrimary.copy(
                            alpha = 0.05f
                        )
                        else AppColors.formIconContainerPrimary.copy(alpha = 0.1f),
                        iconTint = if (isTagLocked) AppColors.primary.copy(alpha = 0.4f) else AppColors.primary
                    )
                }
            }
        }

        // Amount & Records
        val isAmountLocked =
            !filterRules.allowAmountRange && (filter.minAmount != null || filter.maxAmount != null)
        val isLinkedLocked =
            !filterRules.allowLinkedRecords && filter.linkedTransactionIds.isNotEmpty()

        if (filterRules.allowAmountRange || isAmountLocked || filterRules.allowLinkedRecords || isLinkedLocked) {
            EntrySection(
                title = "AMOUNT & RECORDS",
                icon = Icons.Rounded.AttachMoney
            ) {
                if (filterRules.allowAmountRange || isAmountLocked) {
                    AeonContextGrid {
                        EntryActionRow {
                            EntryActionCard(
                                icon = if (isAmountLocked) Icons.Default.Lock else Icons.Rounded.Remove,
                                label = "MIN AMOUNT",
                                value = filter.minAmount?.toString() ?: "No Min",
                                isSelected = filter.minAmount != null,
                                enabled = !isAmountLocked,
                                modifier = Modifier.weight(1f),
                                onClick = { if (!isAmountLocked) activeSheet = "MinAmount" }
                            )
                            EntryActionCard(
                                icon = if (isAmountLocked) Icons.Default.Lock else Icons.Rounded.Add,
                                label = "MAX AMOUNT",
                                value = filter.maxAmount?.toString() ?: "No Max",
                                isSelected = filter.maxAmount != null,
                                enabled = !isAmountLocked,
                                modifier = Modifier.weight(1f),
                                onClick = { if (!isAmountLocked) activeSheet = "MaxAmount" }
                            )
                        }
                    }
                }

                if (filterRules.allowLinkedRecords || isLinkedLocked) {
                    EntryActionCard(
                        icon = if (isLinkedLocked) Icons.Default.Lock else Icons.Rounded.Link,
                        label = "LINKED RECORDS",
                        value = if (isLinkedLocked) {
                            "${filter.linkedTransactionIds.size} Linked"
                        } else if (filter.linkedTransactionIds.isEmpty()) "None Linked" else "${filter.linkedTransactionIds.size} Linked",
                        isSelected = filter.linkedTransactionIds.isNotEmpty(),
                        enabled = !isLinkedLocked,
                        onClick = { if (!isLinkedLocked) activeSheet = "LinkedTransactions" },
                        iconContainerColor = if (isLinkedLocked) AppColors.tertiary.copy(alpha = 0.05f)
                        else AppColors.tertiary.copy(alpha = 0.1f),
                        iconTint = if (isLinkedLocked) AppColors.tertiary.copy(alpha = 0.4f) else AppColors.tertiary
                    )
                }
            }
        }

        // Warranty Section
        val isWarrantyLocked = !filterRules.allowWarrantySelection && filter.hasWarranty != null

        if (filterRules.allowWarrantySelection || isWarrantyLocked) {
            EntrySection(
                title = "WARRANTY",
                icon = Icons.Rounded.VerifiedUser
            ) {
                AeonContextGrid {
                    EntryActionCard(
                        icon = if (isWarrantyLocked) Icons.Default.Lock else Icons.Rounded.Verified,
                        label = "PROTECTION STATUS",
                        value = when (filter.hasWarranty) {
                            null -> "Any Status"
                            true -> "Has Warranty"
                            false -> "No Warranty"
                        },
                        isSelected = filter.hasWarranty != null,
                        enabled = !isWarrantyLocked,
                        onClick = {
                            if (!isWarrantyLocked) {
                                val next = when (filter.hasWarranty) {
                                    null -> true
                                    true -> false
                                    false -> null
                                }
                                onFilterChange(filter.copy(hasWarranty = next))
                            }
                        },
                        iconContainerColor = if (isWarrantyLocked) AppColors.secondary.copy(alpha = 0.05f)
                        else AppColors.secondary.copy(alpha = 0.1f),
                        iconTint = if (isWarrantyLocked) AppColors.secondary.copy(alpha = 0.4f) else AppColors.secondary
                    )

                    if (filter.hasWarranty == true) {
                        val isWarrantyDatesLocked =
                            !filterRules.allowWarrantySelection && (filter.warrantyExpirationStart != null || filter.warrantyExpirationEnd != null)
                        EntryActionRow {
                            EntryActionCard(
                                icon = if (isWarrantyDatesLocked) Icons.Default.Lock else Icons.Rounded.CalendarToday,
                                label = "WARRANTY START",
                                value = filter.warrantyExpirationStart?.let {
                                    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(
                                        Date(it)
                                    )
                                } ?: "Select",
                                enabled = !isWarrantyDatesLocked,
                                onClick = {
                                    if (!isWarrantyDatesLocked) activeSheet = "WarrantyStart"
                                },
                                modifier = Modifier.weight(1f)
                            )
                            EntryActionCard(
                                icon = if (isWarrantyDatesLocked) Icons.Default.Lock else Icons.Rounded.CalendarToday,
                                label = "WARRANTY END",
                                value = filter.warrantyExpirationEnd?.let {
                                    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(
                                        Date(it)
                                    )
                                } ?: "Select",
                                enabled = !isWarrantyDatesLocked,
                                onClick = {
                                    if (!isWarrantyDatesLocked) activeSheet = "WarrantyEnd"
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
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
