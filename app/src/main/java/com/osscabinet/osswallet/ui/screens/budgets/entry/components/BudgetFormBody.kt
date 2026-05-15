package com.osscabinet.ossWallet.ui.screens.budgets.entry.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.components.getAccountIconByName
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.components.entry.*
import com.osscabinet.ossWallet.ui.screens.budgets.entry.BudgetEntryEvent
import com.osscabinet.ossWallet.ui.screens.budgets.entry.BudgetEntryState
import com.osscabinet.ossWallet.ui.theme.AeonTheme

/**
 * Refactored Budget Entry Form Body.
 * Standardized with other entry screens to use reusable components and a consistent layout.
 * Filter-centric design where the FILTER is a primary context item.
 */
@Composable
fun BudgetFormBody(
    state: BudgetEntryState,
    onEvent: (BudgetEntryEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val dims = AeonTheme.dimensions
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = dims.screenPadding,
                    vertical = dims.spacingLg
                ),
            verticalArrangement = Arrangement.spacedBy(dims.spacingLg)
        ) {
            // IMPACT: Header card containing budget name and limit amount
            AeonImpactHeaderCard(
                transactionType = if (state.selectedTrackTypes.contains(TransactionType.EXPENSE)) TransactionType.EXPENSE else TransactionType.INCOME,
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
                // Row 1: FILTER and Period
                EntryActionRow {
                    val filterObj = state.availableFilters.find { it.id == state.filterId }
                    EntryActionCard(
                        icon = Icons.Rounded.FilterList,
                        label = "FILTER",
                        value = filterObj?.name ?: "None",
                        onClick = { onEvent(BudgetEntryEvent.ShowFilterSheet(show = true)) },
                        modifier = Modifier.weight(1f),
                        iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        iconTint = MaterialTheme.colorScheme.primary
                    )

                    EntryActionCard(
                        icon = Icons.Rounded.DateRange,
                        label = "PERIOD",
                        value = when (val range = state.timelineRange) {
                            is TimelineRange.AllTime -> "All Time"
                            is TimelineRange.ThisPeriod -> "This ${range.period.label}"
                            is TimelineRange.LastPeriod -> range.period.label
                            is TimelineRange.CustomRange -> "Custom"
                        },
                        onClick = { onEvent(BudgetEntryEvent.ShowTimelineSheet(show = true)) },
                        modifier = Modifier.weight(1f),
                        iconContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                        iconTint = MaterialTheme.colorScheme.secondary
                    )
                }

                // Row 2: Accounts and Categories
                EntryActionRow {
                    val accountObj = if (state.selectedAccountIds.size == 1) state.accounts.find { it.id == state.selectedAccountIds.first() } else null
                    EntryActionCard(
                        icon = getAccountIconByName(accountObj?.icon),
                        label = "ACCOUNTS",
                        value = when {
                            state.selectedAccountIds.isEmpty() -> "All Accounts"
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
                            state.selectedCategoryIds.isEmpty() -> "All Categories"
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
                            state.selectedTrackTypes.isEmpty() -> "All Flows"
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
                showNote = false,
                showPayee = false,
                showTags = false,
                selectedTags = emptyList(),
                noteSuggestions = state.uniqueNotes,
                additionalContent = {
                    Column(verticalArrangement = Arrangement.spacedBy(dims.spacingLg)) {
                        // Note Filter with Regex Toggle
                        Column(verticalArrangement = Arrangement.spacedBy(dims.spacingSm)) {
                            EntrySwitchRow(
                                label = "Use Regex for Note",
                                checked = state.isNoteRegex,
                                onCheckedChange = { onEvent(BudgetEntryEvent.SetNoteRegex(it)) },
                                icon = Icons.Rounded.Code,
                                subtitle = "Supports /pattern/flags (i, m, s, x, d, c, l)"
                            )
                            EntryEditorialInput(
                                value = state.queryNote,
                                onValueChange = { onEvent(BudgetEntryEvent.SetQueryNote(it)) },
                                label = "NOTE QUERY",
                                icon = Icons.AutoMirrored.Rounded.Notes,
                                placeholder = if (state.isNoteRegex) "e.g. /coffee/i or /^Rent/m" else "e.g. Starbucks coffee"
                            )
                        }

                        // Payee Filter with Regex Toggle
                        Column(verticalArrangement = Arrangement.spacedBy(dims.spacingSm)) {
                            EntrySwitchRow(
                                label = "Use Regex for Payee",
                                checked = state.isPayeeRegex,
                                onCheckedChange = { onEvent(BudgetEntryEvent.SetPayeeRegex(it)) },
                                icon = Icons.Rounded.Code,
                                subtitle = "Supports /pattern/flags (i, m, s, x, d, c, l)"
                            )
                            EntryEditorialInput(
                                value = state.queryPayee,
                                onValueChange = { onEvent(BudgetEntryEvent.SetQueryPayee(it)) },
                                label = "PAYEE QUERY",
                                icon = Icons.Rounded.Person,
                                placeholder = if (state.isPayeeRegex) "e.g. /Amazon/i" else "e.g. Amazon"
                            )
                        }

                        // Tags Selection
                        EntryTagSection(
                            selectedTags = state.selectedTags.toList(),
                            onShowTagSheet = { onEvent(BudgetEntryEvent.ShowTagSheet(show = true)) },
                            onRemoveTag = { tag -> onEvent(BudgetEntryEvent.SetTags(state.selectedTags - tag)) }
                        )

                        // Payment Method and Status multi-selection
                        EntryPaymentMethodStatusRow(
                            paymentMethod = when {
                                state.selectedPaymentMethods.isEmpty() -> "All Methods"
                                state.selectedPaymentMethods.size == PaymentMethod.entries.size -> "All Methods"
                                else -> "${state.selectedPaymentMethods.size} Selected"
                            },
                            onPaymentMethodClick = { onEvent(BudgetEntryEvent.ShowPaymentMethodSheet(show = true)) },
                            statusLabel = when {
                                state.selectedStatuses.isEmpty() -> "All Statuses"
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

                        // Amount Constraints
                        AeonContextGrid {
                            // Min Amount
                            EntryActionCard(
                                icon = Icons.Rounded.ArrowDownward,
                                label = "MIN AMOUNT",
                                value = state.minAmount.ifEmpty { "Any" },
                                onClick = { onEvent(BudgetEntryEvent.ShowMinAmountSheet(show = true)) },
                                modifier = Modifier.weight(1f),
                                iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                iconTint = MaterialTheme.colorScheme.primary
                            )

                            // Max Amount
                            EntryActionCard(
                                icon = Icons.Rounded.ArrowUpward,
                                label = "MAX AMOUNT",
                                value = state.maxAmount.ifEmpty { "Any" },
                                onClick = { onEvent(BudgetEntryEvent.ShowMaxAmountSheet(show = true)) },
                                modifier = Modifier.weight(1f),
                                iconContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                iconTint = MaterialTheme.colorScheme.secondary
                            )
                        }

                        // Warranty
                        EntryActionCard(
                            icon = Icons.Rounded.Verified,
                            label = "WARRANTY",
                            value = when (state.hasWarranty) {
                                null -> "Any"
                                true -> "Has Warranty"
                                false -> "No Warranty"
                            },
                            onClick = {
                                onEvent(
                                    BudgetEntryEvent.SetHasWarranty(
                                        when (state.hasWarranty) {
                                            null -> true
                                            true -> false
                                            false -> null
                                        }
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            iconContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                            iconTint = MaterialTheme.colorScheme.tertiary
                        )

                        if (state.hasWarranty == true) {
                            val formatter = remember { java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()) }
                            AeonContextGrid {
                                EntryActionCard(
                                    icon = Icons.Rounded.CalendarToday,
                                    label = "WARRANTY START",
                                    value = state.warrantyExpirationStart?.let { formatter.format(java.util.Date(it)) } ?: "Select",
                                    onClick = { onEvent(BudgetEntryEvent.ShowWarrantyStartPicker(show = true)) },
                                    modifier = Modifier.weight(1f),
                                    iconContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                    iconTint = MaterialTheme.colorScheme.secondary
                                )

                                EntryActionCard(
                                    icon = Icons.Rounded.CalendarToday,
                                    label = "WARRANTY END",
                                    value = state.warrantyExpirationEnd?.let { formatter.format(java.util.Date(it)) } ?: "Select",
                                    onClick = { onEvent(BudgetEntryEvent.ShowWarrantyEndPicker(show = true)) },
                                    modifier = Modifier.weight(1f),
                                    iconContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                    iconTint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }

                        // Exclude Future
                        EntrySwitchRow(
                            label = "Exclude Future",
                            checked = state.excludeFuture,
                            onCheckedChange = { onEvent(BudgetEntryEvent.SetExcludeFuture(it)) },
                            icon = Icons.Rounded.Schedule,
                            subtitle = "Don't include future transactions"
                        )

                        // Management settings for the budget itself
                        EntrySection(title = "MANAGEMENT", icon = Icons.Rounded.Settings) {
                            Column(verticalArrangement = Arrangement.spacedBy(dims.spacingMd)) {
                                // Alert threshold control
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = dims.spacingSm),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(dims.spacingMd)
                                ) {
                                    Surface(
                                        shape = androidx.compose.foundation.shape.CircleShape,
                                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                        modifier = Modifier.size(dims.spacing2xl)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Rounded.NotificationsActive,
                                                contentDescription = null,
                                                modifier = Modifier.size(dims.iconSizeSm),
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

        if (state.showSharedFilterDialog) {
            SharedFilterConfirmationDialog(
                onDismiss = { onEvent(BudgetEntryEvent.ShowSharedFilterDialog(show = false)) },
                onConfirmUpdateShared = { onEvent(BudgetEntryEvent.ConfirmSaveWithSharedFilter(updateShared = true)) },
                onConfirmCreateNew = { onEvent(BudgetEntryEvent.ConfirmSaveWithSharedFilter(updateShared = false)) }
            )
        }
    }
}
