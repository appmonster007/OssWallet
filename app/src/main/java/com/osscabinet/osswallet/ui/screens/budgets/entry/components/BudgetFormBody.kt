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
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.BudgetPeriod
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.ui.components.getAccountIconByName
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.components.entry.*

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
    onPaymentMethodClick: () -> Unit,
    onStatusClick: () -> Unit,
    onLinkRecordClick: () -> Unit,
    noteSuggestions: List<String>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // IMPACT: Header card
        AeonImpactHeaderCard(
            transactionType = TransactionType.INCOME,
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
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Row 1: Period (Full Width)
            EntryActionCard(
                icon = Icons.Rounded.DateRange,
                label = "PERIOD",
                value = period.name.lowercase().replaceFirstChar { it.uppercase() },
                onClick = onPeriodClick,
                modifier = Modifier.fillMaxWidth(),
                iconContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                iconTint = MaterialTheme.colorScheme.secondary
            )

            // Row 2: Account and Tracks
            AeonContextGrid {
                val accountObj = if (selectedAccountIds.size == 1) accounts.find { it.id == selectedAccountIds.first() } else null
                EntryActionCard(
                    icon = getAccountIconByName(accountObj?.icon),
                    label = "ACCOUNT",
                    value = when {
                        selectedAccountIds.isEmpty() -> "All Accounts"
                        accountObj != null -> accountObj.name
                        else -> "${selectedAccountIds.size} Accounts"
                    },
                    onClick = onAccountClick,
                    modifier = Modifier.weight(1f),
                    iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    iconTint = MaterialTheme.colorScheme.primary
                )

                EntryActionCard(
                    icon = Icons.Rounded.ShowChart,
                    label = "TRACKS",
                    value = if (selectedTrackTypes.size == TransactionType.entries.size) "All Types"
                            else selectedTrackTypes.joinToString { it.displayName },
                    onClick = onTrackTypeClick,
                    modifier = Modifier.weight(1f),
                    iconContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                    iconTint = MaterialTheme.colorScheme.tertiary
                )
            }

            // Row 3: Filters / Categories (Full Width)
            val categoryObj = if (selectedCategoryIds.size == 1) categories.find { it.id == selectedCategoryIds.first() } else null
            EntryActionCard(
                icon = categoryObj?.let { getIconByName(it.iconName) } ?: Icons.Rounded.FilterList,
                label = "FILTERS",
                value = when {
                    selectedCategoryIds.isEmpty() -> "All Categories"
                    categoryObj != null -> categoryObj.name
                    else -> "${selectedCategoryIds.size} Categories"
                },
                onClick = onCategoryClick,
                modifier = Modifier.fillMaxWidth(),
                iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                iconTint = MaterialTheme.colorScheme.primary
            )
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
                AeonEntryMetadataSection(
                    state = AeonMetadataState(
                        paymentMethod = com.osscabinet.ossWallet.domain.model.PaymentMethod.CASH, // Default or first selected
                        linkedTransactions = emptyList() // Need to load these or just show count
                    ),
                    accounts = accounts,
                    categories = categories,
                    is24Hour = false,
                    onShowLinkedTransactions = onLinkRecordClick,
                    onRemoveLink = { /* handled by linking sheet */ },
                    onPaymentMethodClick = onPaymentMethodClick,
                    onStatusClick = onStatusClick,
                    onWarrantyToggle = {},
                    onWarrantyDateClick = {},
                    onLocationClick = {},
                    additionalBottomContent = {
                        // Alert threshold
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Rounded.Shield,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
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
        )
    }
}
