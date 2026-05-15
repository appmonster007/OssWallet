package com.osscabinet.ossWallet.ui.screens.budgets.entry.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.ui.components.getAccountIconByName
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.components.entry.*
import com.osscabinet.ossWallet.ui.theme.AeonTheme

@Composable
fun BudgetFormBody(
    name: String,
    onNameChange: (String) -> Unit,
    amount: String,
    currency: String,
    onCurrencyChange: (String) -> Unit,
    supportedCurrencies: List<String>,
    onAmountClick: () -> Unit,
    period: BudgetPeriod,
    onPeriodClick: () -> Unit,
    selectedAccountIds: Set<String>,
    accounts: List<Account>,
    onAccountClick: () -> Unit,
    selectedTrackTypes: Set<TransactionType>,
    onTrackTypeClick: () -> Unit,
    selectedCurrencies: Set<String>,
    onCurrencyFilterClick: () -> Unit,
    selectedCategoryIds: Set<Long>,
    categories: List<Category>,
    onCategoryClick: () -> Unit,
    note: String,
    onNoteChange: (String) -> Unit,
    alertThreshold: Float,
    onAlertThresholdChange: (Float) -> Unit,
    rollover: Boolean,
    onRolloverChange: (Boolean) -> Unit,
    selectedTags: Set<String>,
    onShowTagSheet: () -> Unit,
    onRemoveTag: (String) -> Unit,
    selectedPaymentMethods: Set<PaymentMethod>,
    onPaymentMethodClick: () -> Unit,
    selectedStatuses: Set<TransactionStatus>,
    onStatusClick: () -> Unit,
    linkedTransactionIds: Set<Long>,
    onLinkRecordClick: () -> Unit,
    filterId: Long?,
    onFilterClick: () -> Unit,
    availableFilters: List<Filter>,
    noteSuggestions: List<String>,
    modifier: Modifier = Modifier,
    isFilterShared: Boolean = false,
    filterUsageCount: Int = 0,
    onMakeFilterUnique: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = AeonTheme.dimensions.screenPadding,
                vertical = AeonTheme.dimensions.spacingLg
            ),
        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg)
    ) {
        // IMPACT: Header card
        AeonImpactHeaderCard(
            transactionType = TransactionType.EXPENSE,
            onTypeSelected = { },
            amount = amount,
            currency = currency,
            onCurrencyClick = { onCurrencyChange(currency) },
            onAmountClick = onAmountClick,
            name = name,
            onNameChange = onNameChange,
            nameLabel = "BUDGET NAME",
            showTypeSelector = false,
            amountColor = Color(0xFF004D40), // Teal for budget healthy
            supportedCurrencies = supportedCurrencies
        )

        // CONTEXT: Action cards grid
        Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)) {
            // Row 1: Filter and Period
            AeonContextGrid {
                val filterObj = availableFilters.find { it.id == filterId }
                EntryActionCard(
                    icon = Icons.Rounded.FilterList,
                    label = "FILTER",
                    value = filterObj?.name ?: "None",
                    onClick = onFilterClick,
                    modifier = Modifier.weight(1f),
                    iconContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                    iconTint = MaterialTheme.colorScheme.tertiary
                )

                EntryActionCard(
                    icon = Icons.Rounded.DateRange,
                    label = "PERIOD",
                    value = period.name.lowercase().replaceFirstChar { it.uppercase() },
                    onClick = onPeriodClick,
                    modifier = Modifier.weight(1f),
                    iconContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                    iconTint = MaterialTheme.colorScheme.secondary
                )
            }

            // Row 2: Account and Categories
            AeonContextGrid {
                val accountObj = if (selectedAccountIds.size == 1) accounts.find { it.id == selectedAccountIds.first() } else null
                EntryActionCard(
                    icon = getAccountIconByName(accountObj?.icon),
                    label = "ACCOUNT",
                    value = when {
                        selectedAccountIds.isEmpty() -> "0 Selected"
                        (accounts.isNotEmpty() && selectedAccountIds.size == accounts.size) -> "All Accounts"
                        accountObj != null -> accountObj.name
                        else -> "${selectedAccountIds.size} Selected"
                    },
                    onClick = onAccountClick,
                    modifier = Modifier.weight(1f),
                    iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    iconTint = MaterialTheme.colorScheme.primary
                )

                val categoryObj = if (selectedCategoryIds.size == 1) categories.find { it.id == selectedCategoryIds.first() } else null
                EntryActionCard(
                    icon = categoryObj?.let { getIconByName(it.iconName) } ?: Icons.Rounded.Category,
                    label = "CATEGORIES",
                    value = when {
                        selectedCategoryIds.isEmpty() -> "0 Selected"
                        (categories.isNotEmpty() && selectedCategoryIds.size == categories.size) -> "All Categories"
                        categoryObj != null -> categoryObj.name
                        else -> "${selectedCategoryIds.size} Selected"
                    },
                    onClick = onCategoryClick,
                    modifier = Modifier.weight(1f),
                    iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    iconTint = MaterialTheme.colorScheme.primary
                )
            }

            // Row 3: Flow & Currencies
            AeonContextGrid {
                EntryActionCard(
                    icon = Icons.Rounded.ShowChart,
                    label = "FLOW",
                    value = when {
                        selectedTrackTypes.isEmpty() -> "0 Selected"
                        selectedTrackTypes.size >= 2 -> "Both"
                        else -> selectedTrackTypes.first().displayName
                    },
                    onClick = onTrackTypeClick,
                    modifier = Modifier.weight(1f),
                    iconContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                    iconTint = MaterialTheme.colorScheme.tertiary
                )

                EntryActionCard(
                    icon = Icons.Rounded.CurrencyExchange,
                    label = "CURRENCIES",
                    value = when {
                        selectedCurrencies.isEmpty() -> "All Currencies"
                        selectedCurrencies.size == 1 -> selectedCurrencies.first()
                        else -> "${selectedCurrencies.size} Selected"
                    },
                    onClick = onCurrencyFilterClick,
                    modifier = Modifier.weight(1f),
                    iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    iconTint = MaterialTheme.colorScheme.primary
                )
            }

            if (filterId != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = AeonTheme.dimensions.spacingXs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isFilterShared) "Shared with ${filterUsageCount - 1} other(s)" else "Unique Filter",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    if (isFilterShared) {
                        TextButton(
                            onClick = onMakeFilterUnique,
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.height(AeonTheme.dimensions.spacingXl)
                        ) {
                            Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(AeonTheme.dimensions.iconSizeXs))
                            Spacer(modifier = Modifier.width(AeonTheme.dimensions.spacingXs))
                            Text("Make Unique", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // DETAIL: Metadata section
        AeonDetailSection(
            note = note,
            onNoteChange = onNoteChange,
            showPayee = false,
            selectedTags = selectedTags,
            onTagClick = onShowTagSheet,
            onRemoveTag = onRemoveTag,
            noteSuggestions = noteSuggestions,
            additionalContent = {
                // Payment Method and Status row
                EntryPaymentMethodStatusRow(
                    paymentMethod = when {
                        selectedPaymentMethods.isEmpty() -> "0 Selected"
                        selectedPaymentMethods.size == PaymentMethod.entries.size -> "All Methods"
                        else -> "${selectedPaymentMethods.size} Selected"
                    },
                    onPaymentMethodClick = onPaymentMethodClick,
                    statusLabel = when {
                        selectedStatuses.isEmpty() -> "0 Selected"
                        selectedStatuses.size == TransactionStatus.entries.size -> "All Statuses"
                        else -> "${selectedStatuses.size} Selected"
                    },
                    onStatusClick = onStatusClick
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

                // Alert threshold
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = AeonTheme.dimensions.spacingSm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
                ) {
                    Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                        modifier = Modifier.size(AeonTheme.dimensions.spacing2xl)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.Shield,
                                contentDescription = null,
                                modifier = Modifier.size(AeonTheme.dimensions.spacingLg),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Alert threshold",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "at ${(alertThreshold * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Slider(
                            value = alertThreshold,
                            onValueChange = onAlertThresholdChange,
                            valueRange = 0.5f..1.0f,
                            steps = 9,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Rollover toggle
                EntrySwitchRow(
                    label = "Roll over balance",
                    checked = rollover,
                    onCheckedChange = onRolloverChange,
                    icon = Icons.Rounded.Autorenew,
                    subtitle = "Carry unused budget forward"
                )
            }
        )
    }
}
