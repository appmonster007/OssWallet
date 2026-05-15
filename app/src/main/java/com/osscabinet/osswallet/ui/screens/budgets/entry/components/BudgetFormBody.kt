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
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.ui.components.getAccountIconByName
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.components.entry.*
import com.osscabinet.ossWallet.ui.screens.budgets.entry.BudgetEntryEvent
import com.osscabinet.ossWallet.ui.screens.budgets.entry.BudgetEntryState
import com.osscabinet.ossWallet.ui.theme.AeonTheme

@Composable
fun BudgetFormBody(
    state: BudgetEntryState,
    onEvent: (BudgetEntryEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
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
                amount = state.limitAmount,
                currency = state.currency,
                onCurrencyClick = { onEvent(BudgetEntryEvent.ShowNumpad(true)) },
                onAmountClick = { onEvent(BudgetEntryEvent.ShowNumpad(true)) },
                name = state.name,
                onNameChange = { onEvent(BudgetEntryEvent.SetName(it)) },
                nameLabel = "BUDGET NAME",
                showTypeSelector = false,
                amountColor = Color(0xFF004D40) // Teal for budget healthy
            )

            // CONTEXT: Action cards grid
            Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)) {
                // Row 1: Period (Full Width Temporal)
                EntryActionCard(
                    icon = Icons.Rounded.DateRange,
                    label = "PERIOD",
                    value = state.period.name.lowercase().replaceFirstChar { it.uppercase() },
                    onClick = { onEvent(BudgetEntryEvent.ShowPeriodSheet(true)) },
                    modifier = Modifier.fillMaxWidth(),
                    iconContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                    iconTint = MaterialTheme.colorScheme.secondary
                )

                // Row 2: Account and Flow
                AeonContextGrid {
                    val accountObj = if (state.selectedAccountIds.size == 1) state.accounts.find { it.id == state.selectedAccountIds.first() } else null
                    EntryActionCard(
                        icon = getAccountIconByName(accountObj?.icon),
                        label = "ACCOUNT",
                        value = when {
                            state.selectedAccountIds.isEmpty() -> "0 Selected"
                            (state.accounts.isNotEmpty() && state.selectedAccountIds.size == state.accounts.size) -> "All Accounts"
                            accountObj != null -> accountObj.name
                            else -> "${state.selectedAccountIds.size} Selected"
                        },
                        onClick = { onEvent(BudgetEntryEvent.ShowAccountSheet(true)) },
                        modifier = Modifier.weight(1f),
                        iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        iconTint = MaterialTheme.colorScheme.primary
                    )

                    EntryActionCard(
                        icon = Icons.Rounded.ShowChart,
                        label = "FLOW",
                        value = when {
                            state.selectedTrackTypes.isEmpty() -> "0 Selected"
                            state.selectedTrackTypes.size >= 2 -> "Both"
                            else -> state.selectedTrackTypes.first().displayName
                        },
                        onClick = { onEvent(BudgetEntryEvent.ShowTrackTypeSheet(true)) },
                        modifier = Modifier.weight(1f),
                        iconContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                        iconTint = MaterialTheme.colorScheme.tertiary
                    )
                }

                // Row 3: Categories (Full Width)
                val categoryObj = if (state.selectedCategoryIds.size == 1) state.categories.find { it.id == state.selectedCategoryIds.first() } else null
                EntryActionCard(
                    icon = categoryObj?.let { getIconByName(it.iconName) } ?: Icons.Rounded.Category,
                    label = "CATEGORIES",
                    value = when {
                        state.selectedCategoryIds.isEmpty() -> "0 Selected"
                        (state.categories.isNotEmpty() && state.selectedCategoryIds.size == state.categories.size) -> "All Categories"
                        categoryObj != null -> categoryObj.name
                        else -> "${state.selectedCategoryIds.size} Selected"
                    },
                    onClick = { onEvent(BudgetEntryEvent.ShowCategorySheet(true)) },
                    modifier = Modifier.fillMaxWidth(),
                    iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    iconTint = MaterialTheme.colorScheme.primary
                )

                // Row 4: Filter (Auxiliary but consistent)
                val filterObj = state.availableFilters.find { it.id == state.filterId }
                EntryActionCard(
                    icon = Icons.Rounded.FilterList,
                    label = "LINKED FILTER",
                    value = filterObj?.name ?: "None",
                    onClick = { onEvent(BudgetEntryEvent.ShowFilterSheet(true)) },
                    modifier = Modifier.fillMaxWidth(),
                    iconContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                    iconTint = MaterialTheme.colorScheme.tertiary
                )

                if (state.filterId != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = AeonTheme.dimensions.spacingXs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (state.filterUsageCount > 1) "Shared with ${state.filterUsageCount - 1} other(s)" else "Unique Filter",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        if (state.filterUsageCount > 1) {
                            TextButton(
                                onClick = { onEvent(BudgetEntryEvent.MakeFilterUnique) },
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
                note = state.note,
                onNoteChange = { onEvent(BudgetEntryEvent.SetNote(it)) },
                showPayee = false,
                selectedTags = state.selectedTags,
                onTagClick = { onEvent(BudgetEntryEvent.ShowTagSheet(true)) },
                onRemoveTag = { tag -> onEvent(BudgetEntryEvent.SetTags(state.selectedTags - tag)) },
                noteSuggestions = state.uniqueNotes,
                additionalContent = {
                    // Currencies Filter
                    EntryActionCard(
                        icon = Icons.Rounded.CurrencyExchange,
                        label = "FILTER CURRENCIES",
                        value = when {
                            state.selectedCurrencies.isEmpty() -> "All Currencies"
                            state.selectedCurrencies.size == 1 -> state.selectedCurrencies.first()
                            else -> "${state.selectedCurrencies.size} Selected"
                        },
                        onClick = { onEvent(BudgetEntryEvent.ShowCurrencyFilterSheet(true)) },
                        modifier = Modifier.fillMaxWidth(),
                        iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        iconTint = MaterialTheme.colorScheme.primary
                    )

                    // Payment Method and Status row
                    EntryPaymentMethodStatusRow(
                        paymentMethod = when {
                            state.selectedPaymentMethods.isEmpty() -> "0 Selected"
                            state.selectedPaymentMethods.size == PaymentMethod.entries.size -> "All Methods"
                            else -> "${state.selectedPaymentMethods.size} Selected"
                        },
                        onPaymentMethodClick = { onEvent(BudgetEntryEvent.ShowPaymentMethodSheet(true)) },
                        statusLabel = when {
                            state.selectedStatuses.isEmpty() -> "0 Selected"
                            state.selectedStatuses.size == TransactionStatus.entries.size -> "All Statuses"
                            else -> "${state.selectedStatuses.size} Selected"
                        },
                        onStatusClick = { onEvent(BudgetEntryEvent.ShowStatusSheet(true)) }
                    )

                    // Linked Transactions
                    EntryActionCard(
                        icon = Icons.Rounded.Link,
                        label = "LINKED TRANSACTIONS",
                        value = if (state.linkedTransactionIds.isEmpty()) "None" else "${state.linkedTransactionIds.size} Linked",
                        onClick = { onEvent(BudgetEntryEvent.ShowLinkSheet(true)) },
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
                                text = "at ${(state.alertThreshold * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Slider(
                                value = state.alertThreshold,
                                onValueChange = { onEvent(BudgetEntryEvent.SetAlertThreshold(it)) },
                                valueRange = 0.5f..1.0f,
                                steps = 9,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Rollover toggle
                    EntrySwitchRow(
                        label = "Roll over balance",
                        checked = state.rollover,
                        onCheckedChange = { onEvent(BudgetEntryEvent.SetRollover(it)) },
                        icon = Icons.Rounded.Autorenew,
                        subtitle = "Carry unused budget forward"
                    )
                }
            )
        }

        BudgetEntrySheets(state = state, onEvent = onEvent)
    }
}
