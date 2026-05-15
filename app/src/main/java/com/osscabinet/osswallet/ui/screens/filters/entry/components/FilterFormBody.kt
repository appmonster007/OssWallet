package com.osscabinet.ossWallet.ui.screens.filters.entry.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.ui.components.getAccountIconByName
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.components.entry.*

@Composable
fun FilterFormBody(
    name: String,
    onNameChange: (String) -> Unit,
    selectedTransactionTypes: Set<com.osscabinet.ossWallet.domain.model.TransactionType>,
    onTypeClick: () -> Unit,
    selectedAccountIds: Set<String>,
    accounts: List<Account>,
    onAccountClick: () -> Unit,
    selectedCategoryIds: Set<Long>,
    categories: List<Category>,
    onCategoryClick: () -> Unit,
    selectedPaymentMethods: Set<PaymentMethod>,
    onPaymentMethodClick: () -> Unit,
    selectedStatuses: Set<TransactionStatus>,
    onStatusClick: () -> Unit,
    linkedTransactionIds: Set<Long>,
    onLinkRecordClick: () -> Unit,
    selectedTags: Set<String>,
    onShowTagSheet: () -> Unit,
    onRemoveTag: (String) -> Unit,
    selectedCurrencies: Set<String>,
    onCurrencyClick: () -> Unit,
    queryNote: String,
    onQueryNoteChange: (String) -> Unit,
    isNoteRegex: Boolean,
    onNoteRegexChange: (Boolean) -> Unit,
    queryPayee: String,
    onQueryPayeeChange: (String) -> Unit,
    isPayeeRegex: Boolean,
    onPayeeRegexChange: (Boolean) -> Unit,
    minAmount: String,
    onMinAmountClick: () -> Unit,
    maxAmount: String,
    onMaxAmountClick: () -> Unit,
    hasWarranty: Boolean?,
    onWarrantyClick: () -> Unit,
    warrantyExpirationStart: Long?,
    onWarrantyStartClick: () -> Unit,
    warrantyExpirationEnd: Long?,
    onWarrantyEndClick: () -> Unit,
    excludeFuture: Boolean,
    onExcludeFutureChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val dims = AeonTheme.dimensions
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dims.screenPadding, vertical = dims.spacingLg),
        verticalArrangement = Arrangement.spacedBy(dims.spacingLg)
    ) {
        // IMPACT: Header card (Filter Name)
        AeonImpactHeaderCard(
            transactionType = if (selectedTransactionTypes.size == 1) selectedTransactionTypes.first() else com.osscabinet.ossWallet.domain.model.TransactionType.EXPENSE,
            onTypeSelected = { onTypeClick() },
            amount = "",
            currency = "",
            onCurrencyClick = { },
            onAmountClick = { },
            name = name,
            onNameChange = onNameChange,
            nameLabel = "FILTER NAME",
            showTypeSelector = true,
            showAmount = false,
            showCurrency = false
        )

        // CONTEXT: Action cards grid
        Column(verticalArrangement = Arrangement.spacedBy(dims.spacingMd)) {
            Text(
                "CONTEXT",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
            
            AeonContextGrid {
                // Accounts
                val accountObj = if (selectedAccountIds.size == 1) accounts.find { it.id == selectedAccountIds.first() } else null
                EntryActionCard(
                    icon = getAccountIconByName(accountObj?.icon),
                    label = "ACCOUNTS",
                    value = when {
                        selectedAccountIds.isEmpty() || (accounts.isNotEmpty() && selectedAccountIds.size == accounts.size) -> "All Accounts"
                        accountObj != null -> accountObj.name
                        else -> "${selectedAccountIds.size} Selected"
                    },
                    onClick = onAccountClick,
                    modifier = Modifier.weight(1f),
                    iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    iconTint = MaterialTheme.colorScheme.primary
                )

                // Categories
                val categoryObj = if (selectedCategoryIds.size == 1) categories.find { it.id == selectedCategoryIds.first() } else null
                EntryActionCard(
                    icon = categoryObj?.let { getIconByName(it.iconName) } ?: Icons.Rounded.Category,
                    label = "CATEGORIES",
                    value = when {
                        selectedCategoryIds.isEmpty() || (categories.isNotEmpty() && selectedCategoryIds.size == categories.size) -> "All Categories"
                        categoryObj != null -> categoryObj.name
                        else -> "${selectedCategoryIds.size} Selected"
                    },
                    onClick = onCategoryClick,
                    modifier = Modifier.weight(1f),
                    iconContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                    iconTint = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // FILTERS: Multi-selection filters
        Column(verticalArrangement = Arrangement.spacedBy(dims.spacingMd)) {
            Text(
                "FILTERS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )

            // Payment Methods
            EntryActionCard(
                icon = Icons.Rounded.Payments,
                label = "PAYMENT METHODS",
                value = if (selectedPaymentMethods.isEmpty() || selectedPaymentMethods.size == PaymentMethod.entries.size) "All Methods" else "${selectedPaymentMethods.size} Selected",
                onClick = onPaymentMethodClick,
                modifier = Modifier.fillMaxWidth(),
                iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                iconTint = MaterialTheme.colorScheme.primary
            )

            // Transaction Status
            EntryActionCard(
                icon = Icons.Rounded.CheckCircle,
                label = "TRANSACTION STATUS",
                value = if (selectedStatuses.isEmpty() || selectedStatuses.size == TransactionStatus.entries.size) "All Statuses" else "${selectedStatuses.size} Selected",
                onClick = onStatusClick,
                modifier = Modifier.fillMaxWidth(),
                iconContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                iconTint = MaterialTheme.colorScheme.secondary
            )

            // Linked Transactions
            EntryActionCard(
                icon = Icons.Rounded.Link,
                label = "LINKED TRANSACTIONS",
                value = if (linkedTransactionIds.isEmpty()) "None" else "${linkedTransactionIds.size} Linked",
                onClick = onLinkRecordClick,
                modifier = Modifier.fillMaxWidth(),
                iconContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                iconTint = MaterialTheme.colorScheme.tertiary
            )

            // Currencies
            EntryActionCard(
                icon = Icons.Rounded.AttachMoney,
                label = "CURRENCIES",
                value = if (selectedCurrencies.isEmpty()) "All Currencies" else "${selectedCurrencies.size} Selected",
                onClick = onCurrencyClick,
                modifier = Modifier.fillMaxWidth(),
                iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                iconTint = MaterialTheme.colorScheme.primary
            )
        }

        // SEARCH: Query filters
        Column(verticalArrangement = Arrangement.spacedBy(dims.spacingMd)) {
            Text(
                "SEARCH",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )

            // Note Query
            EntryEditorialInput(
                label = "NOTE QUERY",
                value = queryNote,
                onValueChange = onQueryNoteChange,
                icon = Icons.Rounded.Note,
                placeholder = "Search in notes...",
                additionalContent = {
                    EntrySwitchRow(
                        label = "Regex",
                        checked = isNoteRegex,
                        onCheckedChange = onNoteRegexChange,
                        icon = Icons.Rounded.Code
                    )
                }
            )

            // Payee Query
            EntryEditorialInput(
                label = "PAYEE QUERY",
                value = queryPayee,
                onValueChange = onQueryPayeeChange,
                icon = Icons.Rounded.Person,
                placeholder = "Search in payees...",
                additionalContent = {
                    EntrySwitchRow(
                        label = "Regex",
                        checked = isPayeeRegex,
                        onCheckedChange = onPayeeRegexChange,
                        icon = Icons.Rounded.Code
                    )
                }
            )
        }

        // RANGE: Amount and date ranges
        Column(verticalArrangement = Arrangement.spacedBy(dims.spacingMd)) {
            Text(
                "RANGE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )

            AeonContextGrid {
                // Min Amount
                EntryActionCard(
                    icon = Icons.Rounded.ArrowDownward,
                    label = "MIN AMOUNT",
                    value = if (minAmount.isEmpty()) "Any" else minAmount,
                    onClick = onMinAmountClick,
                    modifier = Modifier.weight(1f),
                    iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    iconTint = MaterialTheme.colorScheme.primary
                )

                // Max Amount
                EntryActionCard(
                    icon = Icons.Rounded.ArrowUpward,
                    label = "MAX AMOUNT",
                    value = if (maxAmount.isEmpty()) "Any" else maxAmount,
                    onClick = onMaxAmountClick,
                    modifier = Modifier.weight(1f),
                    iconContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                    iconTint = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // OPTIONS: Additional options
        Column(verticalArrangement = Arrangement.spacedBy(dims.spacingMd)) {
            Text(
                "OPTIONS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )

            // Warranty
            EntryActionCard(
                icon = Icons.Rounded.Verified,
                label = "WARRANTY",
                value = when (hasWarranty) {
                    null -> "Any"
                    true -> "Has Warranty"
                    false -> "No Warranty"
                },
                onClick = onWarrantyClick,
                modifier = Modifier.fillMaxWidth(),
                iconContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                iconTint = MaterialTheme.colorScheme.tertiary
            )

            if (hasWarranty == true) {
                AeonContextGrid {
                    EntryDateCard(
                        icon = Icons.Rounded.CalendarToday,
                        label = "WARRANTY START",
                        dateMillis = warrantyExpirationStart ?: System.currentTimeMillis(),
                        onClick = onWarrantyStartClick,
                        modifier = Modifier.weight(1f)
                    )

                    EntryDateCard(
                        icon = Icons.Rounded.CalendarToday,
                        label = "WARRANTY END",
                        dateMillis = warrantyExpirationEnd ?: System.currentTimeMillis(),
                        onClick = onWarrantyEndClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Exclude Future
            EntrySwitchRow(
                label = "Exclude Future",
                checked = excludeFuture,
                onCheckedChange = onExcludeFutureChange,
                icon = Icons.Rounded.Schedule,
                subtitle = "Don't include future transactions"
            )
        }

        // DETAIL: Metadata section
        AeonDetailSection(
            note = "",
            onNoteChange = { },
            showPayee = false,
            selectedTags = selectedTags,
            onTagClick = onShowTagSheet,
            onRemoveTag = onRemoveTag,
            noteSuggestions = emptyList()
        )
    }
}
