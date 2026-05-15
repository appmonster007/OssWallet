package com.osscabinet.ossWallet.ui.screens.filters.entry.components

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
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.components.getAccountIconByName
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.components.entry.*
import com.osscabinet.ossWallet.ui.screens.filters.entry.FilterEntryEvent
import com.osscabinet.ossWallet.ui.screens.filters.entry.FilterEntryState
import com.osscabinet.ossWallet.ui.theme.AeonTheme

/**
 * Refactored Filter Entry Form Body.
 * Standardized with Budget and Loan entry screens.
 * Uses consistent sections and reusable components.
 */
@Composable
fun FilterFormBody(
    state: FilterEntryState,
    onEvent: (FilterEntryEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dims = AeonTheme.dimensions
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dims.screenPadding, vertical = dims.spacingLg),
            verticalArrangement = Arrangement.spacedBy(dims.spacingLg)
        ) {
            // IMPACT: Header card (Filter Name)
            AeonImpactHeaderCard(
                transactionType = TransactionType.EXPENSE,
                onTypeSelected = { },
                amount = "",
                currency = "",
                onCurrencyClick = { },
                onAmountClick = { },
                name = state.name,
                onNameChange = { onEvent(FilterEntryEvent.SetName(it)) },
                nameLabel = "FILTER NAME",
                showTypeSelector = false,
                showAmount = false,
                showCurrency = false
            )

            // CONTEXT: Primary filter criteria grid
            AeonContextGrid {
                EntryActionRow {
                    // Period
                    EntryActionCard(
                        icon = Icons.Rounded.DateRange,
                        label = "PERIOD",
                        value = when (val range = state.timelineRange) {
                            is TimelineRange.AllTime -> "All Time"
                            is TimelineRange.ThisPeriod -> "This ${range.period.label}"
                            is TimelineRange.LastPeriod -> range.period.label
                            is TimelineRange.CustomRange -> "Custom"
                        },
                        onClick = { onEvent(FilterEntryEvent.ShowTimelineSheet(show = true)) },
                        modifier = Modifier.weight(1f),
                        iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        iconTint = MaterialTheme.colorScheme.primary
                    )

                    // Flows (renamed from Types)
                    EntryActionCard(
                        icon = Icons.AutoMirrored.Rounded.ShowChart,
                        label = "FLOWS",
                        value = when {
                            state.selectedTransactionTypes.isEmpty() || (state.selectedTransactionTypes.size == (TransactionType.entries.size - 1)) -> "All Flows"
                            state.selectedTransactionTypes.size == 1 -> state.selectedTransactionTypes.first().displayName
                            else -> "${state.selectedTransactionTypes.size} Selected"
                        },
                        onClick = { onEvent(FilterEntryEvent.ShowTypeSheet(show = true)) },
                        modifier = Modifier.weight(1f),
                        iconContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                        iconTint = MaterialTheme.colorScheme.tertiary
                    )
                }

                EntryActionRow {
                    // Accounts
                    val accountObj = if (state.selectedAccountIds.size == 1) state.accounts.find { it.id == state.selectedAccountIds.first() } else null
                    EntryActionCard(
                        icon = getAccountIconByName(accountObj?.icon),
                        label = "ACCOUNTS",
                        value = when {
                            state.selectedAccountIds.isEmpty() || (state.accounts.isNotEmpty() && (state.selectedAccountIds.size == state.accounts.size)) -> "All Accounts"
                            accountObj != null -> accountObj.name
                            else -> "${state.selectedAccountIds.size} Selected"
                        },
                        onClick = { onEvent(FilterEntryEvent.ShowAccountSheet(show = true)) },
                        modifier = Modifier.weight(1f),
                        iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        iconTint = MaterialTheme.colorScheme.primary
                    )

                    // Categories
                    val categoryObj = if (state.selectedCategoryIds.size == 1) state.categories.find { it.id == state.selectedCategoryIds.first() } else null
                    EntryActionCard(
                        icon = categoryObj?.let { getIconByName(it.iconName) } ?: Icons.Rounded.Category,
                        label = "CATEGORIES",
                        value = when {
                            state.selectedCategoryIds.isEmpty() || (state.categories.isNotEmpty() && state.selectedCategoryIds.size == state.categories.size) -> "All Categories"
                            categoryObj != null -> categoryObj.name
                            else -> "${state.selectedCategoryIds.size} Selected"
                        },
                        onClick = { onEvent(FilterEntryEvent.ShowCategorySheet(show = true)) },
                        modifier = Modifier.weight(1f),
                        iconContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                        iconTint = MaterialTheme.colorScheme.secondary
                    )
                }

                // Currencies Standalone card
                EntryActionCard(
                    icon = Icons.Rounded.CurrencyExchange,
                    label = "CURRENCIES",
                    value = when {
                        state.selectedCurrencies.isEmpty() -> "All Currencies"
                        state.selectedCurrencies.size == 1 -> state.selectedCurrencies.first()
                        else -> "${state.selectedCurrencies.size} Selected"
                    },
                    onClick = { onEvent(FilterEntryEvent.ShowCurrencySheet(show = true)) },
                    modifier = Modifier.fillMaxWidth(),
                    iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    iconTint = MaterialTheme.colorScheme.primary
                )
            }

            // DETAIL: Extended filter criteria and search
            AeonDetailSection(
                note = state.queryNote,
                onNoteChange = { onEvent(FilterEntryEvent.SetQueryNote(it)) },
                noteLabel = "NOTE QUERY",
                payee = state.queryPayee,
                onPayeeChange = { onEvent(FilterEntryEvent.SetQueryPayee(it)) },
                payeeLabel = "PAYEE QUERY",
                showNote = false,
                showPayee = false,
                selectedTags = emptyList(),
                additionalContent = {
                    Column(verticalArrangement = Arrangement.spacedBy(dims.spacingLg)) {
                        // Note Filter with Regex Toggle
                        Column(verticalArrangement = Arrangement.spacedBy(dims.spacingSm)) {
                            EntrySwitchRow(
                                label = "Use Regex for Note",
                                checked = state.isNoteRegex,
                                onCheckedChange = { onEvent(FilterEntryEvent.SetNoteRegex(it)) },
                                icon = Icons.Rounded.Code,
                                subtitle = "Supports /pattern/flags (i, m, s, x, d, c, l)"
                            )
                            EntryEditorialInput(
                                value = state.queryNote,
                                onValueChange = { onEvent(FilterEntryEvent.SetQueryNote(it)) },
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
                                onCheckedChange = { onEvent(FilterEntryEvent.SetPayeeRegex(it)) },
                                icon = Icons.Rounded.Code,
                                subtitle = "Supports /pattern/flags (i, m, s, x, d, c, l)"
                            )
                            EntryEditorialInput(
                                value = state.queryPayee,
                                onValueChange = { onEvent(FilterEntryEvent.SetQueryPayee(it)) },
                                label = "PAYEE QUERY",
                                icon = Icons.Rounded.Person,
                                placeholder = if (state.isPayeeRegex) "e.g. /Amazon/i" else "e.g. Amazon"
                            )
                        }

                        // Tags Selection
                        EntryTagSection(
                            selectedTags = state.selectedTags.toList(),
                            onShowTagSheet = { onEvent(FilterEntryEvent.ShowTagSheet(show = true)) },
                            onRemoveTag = { tag -> onEvent(FilterEntryEvent.SetTags(state.selectedTags - tag)) }
                        )

                        // Payment Method and Status
                        EntryPaymentMethodStatusRow(
                            paymentMethod = if (state.selectedPaymentMethods.isEmpty() || state.selectedPaymentMethods.size == PaymentMethod.entries.size) "All Methods" else "${state.selectedPaymentMethods.size} Selected",
                            onPaymentMethodClick = { onEvent(FilterEntryEvent.ShowPaymentMethodSheet(show = true)) },
                            statusLabel = if (state.selectedStatuses.isEmpty() || state.selectedStatuses.size == TransactionStatus.entries.size) "All Statuses" else "${state.selectedStatuses.size} Selected",
                            onStatusClick = { onEvent(FilterEntryEvent.ShowStatusSheet(show = true)) }
                        )

                        // Linked Transactions
                        EntryActionCard(
                            icon = Icons.Rounded.Link,
                            label = "LINKED TRANSACTIONS",
                            value = if (state.linkedTransactionIds.isEmpty()) "None" else "${state.linkedTransactionIds.size} Linked",
                            onClick = { onEvent(FilterEntryEvent.ShowLinkSheet(show = true)) },
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
                                onClick = { onEvent(FilterEntryEvent.ShowMinAmountSheet(show = true)) },
                                modifier = Modifier.weight(1f),
                                iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                iconTint = MaterialTheme.colorScheme.primary
                            )

                            // Max Amount
                            EntryActionCard(
                                icon = Icons.Rounded.ArrowUpward,
                                label = "MAX AMOUNT",
                                value = state.maxAmount.ifEmpty { "Any" },
                                onClick = { onEvent(FilterEntryEvent.ShowMaxAmountSheet(show = true)) },
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
                                    FilterEntryEvent.SetHasWarranty(
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
                                    onClick = { onEvent(FilterEntryEvent.ShowWarrantyStartPicker(show = true)) },
                                    modifier = Modifier.weight(1f),
                                    iconContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                    iconTint = MaterialTheme.colorScheme.secondary
                                )

                                EntryActionCard(
                                    icon = Icons.Rounded.CalendarToday,
                                    label = "WARRANTY END",
                                    value = state.warrantyExpirationEnd?.let { formatter.format(java.util.Date(it)) } ?: "Select",
                                    onClick = { onEvent(FilterEntryEvent.ShowWarrantyEndPicker(show = true)) },
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
                            onCheckedChange = { onEvent(FilterEntryEvent.SetExcludeFuture(it)) },
                            icon = Icons.Rounded.Schedule,
                            subtitle = "Don't include future transactions"
                        )
                    }
                }
            )
        }

        FilterEntrySheets(state = state, onEvent = onEvent)
    }
}
