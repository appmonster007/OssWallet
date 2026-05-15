package com.osscabinet.ossWallet.ui.screens.budgets.entry.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
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

/**
 * Refactored Budget Entry Form Body.
 * Standardized with other entry screens to use reusable components and a consistent layout.
 * Filter-centric design where the linked filter is a primary context item.
 */
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
            // IMPACT: Header card containing budget name and limit amount
            AeonImpactHeaderCard(
                transactionType = TransactionType.EXPENSE,
                onTypeSelected = { },
                amount = state.limitAmount,
                currency = state.currency,
                onCurrencyClick = { onEvent(BudgetEntryEvent.ShowNumpad(show = true)) },
                onAmountClick = { onEvent(BudgetEntryEvent.ShowNumpad(show = true)) },
                name = state.name,
                onNameChange = { onEvent(BudgetEntryEvent.SetName(it)) },
                nameLabel = "BUDGET NAME",
                showTypeSelector = false,
                amountColor = Color(0xFF004D40) // Teal for budget healthy
            )

            // CONTEXT: Action cards grid for primary budget parameters
            AeonContextGrid {
                // Row 1: Filter and Period
                EntryActionRow {
                    val filterObj = state.availableFilters.find { it.id == state.filterId }
                    EntryActionCard(
                        icon = Icons.Rounded.FilterList,
                        label = "LINKED FILTER",
                        value = filterObj?.name ?: "None",
                        onClick = { onEvent(BudgetEntryEvent.ShowFilterSheet(show = true)) },
                        modifier = Modifier.weight(1f),
                        iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        iconTint = MaterialTheme.colorScheme.primary
                    )

                    EntryActionCard(
                        icon = Icons.Rounded.DateRange,
                        label = "PERIOD",
                        value = state.period.name.lowercase().replaceFirstChar { it.uppercase() },
                        onClick = { onEvent(BudgetEntryEvent.ShowPeriodSheet(show = true)) },
                        modifier = Modifier.weight(1f),
                        iconContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                        iconTint = MaterialTheme.colorScheme.secondary
                    )
                }

                // Row 2: Accounts and Categories (Filterable)
                EntryActionRow {
                    val accountObj = if (state.selectedAccountIds.size == 1) state.accounts.find { it.id == state.selectedAccountIds.first() } else null
                    EntryActionCard(
                        icon = getAccountIconByName(accountObj?.icon),
                        label = "ACCOUNTS",
                        value = when {
                            state.selectedAccountIds.isEmpty() -> "0 Selected"
                            (state.accounts.isNotEmpty() && (state.selectedAccountIds.size == state.accounts.size)) -> "All Accounts"
                            accountObj != null -> accountObj.name
                            else -> "${state.selectedAccountIds.size} Selected"
                        },
                        onClick = { onEvent(BudgetEntryEvent.ShowAccountSheet(show = true)) },
                        modifier = Modifier.weight(1f),
                        iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        iconTint = MaterialTheme.colorScheme.primary
                    )

                    val categoryObj = if (state.selectedCategoryIds.size == 1) state.categories.find { it.id == state.selectedCategoryIds.first() } else null
                    EntryActionCard(
                        icon = categoryObj?.let { getIconByName(it.iconName) } ?: Icons.Rounded.Category,
                        label = "CATEGORIES",
                        value = when {
                            state.selectedCategoryIds.isEmpty() -> "0 Selected"
                            (state.categories.isNotEmpty() && (state.selectedCategoryIds.size == state.categories.size)) -> "All Categories"
                            categoryObj != null -> categoryObj.name
                            else -> "${state.selectedCategoryIds.size} Selected"
                        },
                        onClick = { onEvent(BudgetEntryEvent.ShowCategorySheet(show = true)) },
                        modifier = Modifier.weight(1f),
                        iconContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                        iconTint = MaterialTheme.colorScheme.tertiary
                    )
                }

                // Row 3: Flows and Currencies
                EntryActionRow {
                    EntryActionCard(
                        icon = Icons.AutoMirrored.Rounded.ShowChart,
                        label = "FLOWS",
                        value = when {
                            state.selectedTrackTypes.isEmpty() -> "0 Selected"
                            state.selectedTrackTypes.size >= 2 -> "All Flows"
                            else -> state.selectedTrackTypes.first().displayName
                        },
                        onClick = { onEvent(BudgetEntryEvent.ShowTrackTypeSheet(show = true)) },
                        modifier = Modifier.weight(1f),
                        iconContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                        iconTint = MaterialTheme.colorScheme.tertiary
                    )

                    EntryActionCard(
                        icon = Icons.Rounded.CurrencyExchange,
                        label = "CURRENCIES",
                        value = when {
                            state.selectedCurrencies.isEmpty() -> "All Currencies"
                            state.selectedCurrencies.size == 1 -> state.selectedCurrencies.first()
                            else -> "${state.selectedCurrencies.size} Selected"
                        },
                        onClick = { onEvent(BudgetEntryEvent.ShowCurrencyFilterSheet(show = true)) },
                        modifier = Modifier.weight(1f),
                        iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        iconTint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // DETAIL: Metadata and extended filter criteria
            AeonDetailSection(
                note = state.note,
                onNoteChange = { onEvent(BudgetEntryEvent.SetNote(it)) },
                showPayee = false,
                selectedTags = state.selectedTags,
                onTagClick = { onEvent(BudgetEntryEvent.ShowTagSheet(show = true)) },
                onRemoveTag = { tag -> onEvent(BudgetEntryEvent.SetTags(state.selectedTags - tag)) },
                noteSuggestions = state.uniqueNotes,
                additionalContent = {
                    Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg)) {
                        // Sharing info and Make Unique action
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

                        // Payment Method and Status multi-selection
                        EntryPaymentMethodStatusRow(
                            paymentMethod = when {
                                state.selectedPaymentMethods.isEmpty() -> "0 Selected"
                                state.selectedPaymentMethods.size == PaymentMethod.entries.size -> "All Methods"
                                else -> "${state.selectedPaymentMethods.size} Selected"
                            },
                            onPaymentMethodClick = { onEvent(BudgetEntryEvent.ShowPaymentMethodSheet(show = true)) },
                            statusLabel = when {
                                state.selectedStatuses.isEmpty() -> "0 Selected"
                                state.selectedStatuses.size == TransactionStatus.entries.size -> "All Statuses"
                                else -> "${state.selectedStatuses.size} Selected"
                            },
                            onStatusClick = { onEvent(BudgetEntryEvent.ShowStatusSheet(show = true)) }
                        )

                        // Linked Transactions
                        EntryActionCard(
                            icon = Icons.Rounded.Link,
                            label = "LINKED TRANSACTIONS",
                            value = if (state.linkedTransactionIds.isEmpty()) "None" else "${state.linkedTransactionIds.size} Linked",
                            onClick = { onEvent(BudgetEntryEvent.ShowLinkSheet(show = true)) },
                            modifier = Modifier.fillMaxWidth(),
                            iconContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                            iconTint = MaterialTheme.colorScheme.tertiary
                        )

                        // Management settings for the budget itself
                        EntrySection(title = "MANAGEMENT", icon = Icons.Rounded.Settings) {
                            Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)) {
                                // Alert threshold control
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
                                                Icons.Rounded.NotificationsActive,
                                                contentDescription = null,
                                                modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm),
                                                tint = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Alert threshold at ${(state.alertThreshold * 100).toInt()}%",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
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

                                // Rollover behavior toggle
                                EntrySwitchRow(
                                    label = "Roll over balance",
                                    checked = state.rollover,
                                    onCheckedChange = { onEvent(BudgetEntryEvent.SetRollover(it)) },
                                    icon = Icons.Rounded.Autorenew,
                                    subtitle = "Carry unused budget forward"
                                )
                            }
                        }
                    }
                }
            )
        }

        // Dedicated sheets for selecting multi-value parameters
        BudgetEntrySheets(state = state, onEvent = onEvent)
    }
}
