package com.osscabinet.ossWallet.ui.screens.loans.entry.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.components.getAccountIconByName
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.components.entry.*
import com.osscabinet.ossWallet.ui.screens.loans.entry.LoanEntryEvent
import com.osscabinet.ossWallet.ui.screens.loans.entry.LoanEntryState
import com.osscabinet.ossWallet.ui.theme.AeonTheme

/**
 * Refactored Loan Entry Form Body.
 * Standardized with other entry screens to use reusable components and a consistent layout.
 * Filter-centric design where the FILTER is a primary context item.
 */
@Composable
fun LoanFormBody(
    state: LoanEntryState,
    onEvent: (LoanEntryEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val amountColor = if (state.loanType == LoanType.LENT) Color(0xFF004D40) else Color(0xFFE65100)
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
            // IMPACT: Header card containing loan type and principal amount
            AeonImpactHeaderCard(
                selectedType = state.loanType,
                onTypeSelected = { onEvent(LoanEntryEvent.SetLoanType(it)) },
                types = listOf(
                    Triple(Icons.Rounded.ArrowUpward, "Lent", LoanType.LENT),
                    Triple(Icons.Rounded.ArrowDownward, "Borrowed", LoanType.BORROWED)
                ),
                amount = state.principalAmount,
                currency = state.currency,
                onCurrencyClick = { onEvent(LoanEntryEvent.ShowNumpad(show = true)) },
                onAmountClick = { onEvent(LoanEntryEvent.ShowNumpad(show = true)) },
                name = state.name,
                onNameChange = { onEvent(LoanEntryEvent.SetName(it)) },
                nameLabel = "LOAN NAME",
                showName = true,
                amountColor = amountColor
            )

            // CONTEXT: Action cards grid for primary loan parameters
            AeonContextGrid {
                // Row 1: FILTER and Period
                EntryActionRow {
                    val filterObj = state.availableFilters.find { it.id == state.filterId }
                    EntryActionCard(
                        icon = Icons.Rounded.FilterList,
                        label = "FILTER",
                        value = filterObj?.name ?: "None",
                        onClick = { onEvent(LoanEntryEvent.ShowFilterSheet(show = true)) },
                        modifier = Modifier.weight(1f),
                        iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        iconTint = MaterialTheme.colorScheme.primary
                    )

                    EntryActionCard(
                        icon = Icons.Rounded.DateRange,
                        label = "PERIOD",
                        value = when (val range = state.timelineRange) {
                            is TimelineRange.AllTime -> "All Time"
                            is TimelineRange.ThisPeriod -> range.period.label
                            is TimelineRange.LastPeriod -> range.period.label
                            is TimelineRange.CustomRange -> "Custom"
                        },
                        onClick = { onEvent(LoanEntryEvent.ShowTimelineSheet(show = true)) },
                        modifier = Modifier.weight(1f),
                        iconContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                        iconTint = MaterialTheme.colorScheme.secondary
                    )
                }

                // Row 2: Account and Interest
                EntryActionRow {
                    val accountObj = state.accounts.find { it.id == state.linkedAccountId }
                    EntryActionCard(
                        icon = getAccountIconByName(accountObj?.icon),
                        label = "ACCOUNT",
                        value = accountObj?.name ?: "Select",
                        onClick = { onEvent(LoanEntryEvent.ShowAccountSheet(show = true)) },
                        modifier = Modifier.weight(1f),
                        iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        iconTint = MaterialTheme.colorScheme.primary
                    )

                    EntryActionCard(
                        icon = Icons.Rounded.Percent,
                        label = "INTEREST",
                        value = if (state.interestRate.isEmpty()) "0%" else "${state.interestRate}%",
                        onClick = { onEvent(LoanEntryEvent.ShowInterestNumpad(show = true)) },
                        modifier = Modifier.weight(1f),
                        iconContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                        iconTint = MaterialTheme.colorScheme.tertiary
                    )
                }

                // Row 3: Categories and Start Date
                EntryActionRow {
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
                        onClick = { onEvent(LoanEntryEvent.ShowCategorySheet(show = true)) },
                        modifier = Modifier.weight(1f),
                        iconContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                        iconTint = MaterialTheme.colorScheme.secondary
                    )

                    val formatter = remember { java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()) }
                    EntryActionCard(
                        icon = Icons.Rounded.CalendarToday,
                        label = "START DATE",
                        value = formatter.format(java.util.Date(state.startDate)),
                        onClick = { onEvent(LoanEntryEvent.ShowStartDatePicker(show = true)) },
                        modifier = Modifier.weight(1f),
                        iconContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                        iconTint = MaterialTheme.colorScheme.secondary
                    )
                }

                // Row 4: Settlement and Flows
                EntryActionRow {
                    val formatter = remember { java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()) }
                    EntryActionCard(
                        icon = Icons.Rounded.EventAvailable,
                        label = "SETTLEMENT",
                        value = if (state.hasSettlementDate) {
                            formatter.format(java.util.Date(state.settlementDate))
                        } else "None",
                        onClick = { onEvent(LoanEntryEvent.ShowSettlementDatePicker(show = true)) },
                        modifier = Modifier.weight(1f),
                        iconContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                        iconTint = MaterialTheme.colorScheme.secondary
                    )

                    EntryActionCard(
                        icon = Icons.AutoMirrored.Rounded.ShowChart,
                        label = "FLOWS",
                        value = when {
                            state.selectedTrackTypes.isEmpty() -> "All Flows"
                            state.selectedTrackTypes.size >= 2 -> "All Flows"
                            else -> state.selectedTrackTypes.first().displayName
                        },
                        onClick = { onEvent(LoanEntryEvent.ShowTrackTypeSheet(show = true)) },
                        modifier = Modifier.weight(1f),
                        iconContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                        iconTint = MaterialTheme.colorScheme.tertiary
                    )
                }

                // Row 5: Currencies
                EntryActionCard(
                    icon = Icons.Rounded.CurrencyExchange,
                    label = "CURRENCIES",
                    value = when {
                        state.selectedCurrencies.isEmpty() -> "All Currencies"
                        state.selectedCurrencies.size == 1 -> state.selectedCurrencies.first()
                        else -> "${state.selectedCurrencies.size} Selected"
                    },
                    onClick = { onEvent(LoanEntryEvent.ShowCurrencyFilterSheet(show = true)) },
                    modifier = Modifier.fillMaxWidth(),
                    iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    iconTint = MaterialTheme.colorScheme.primary
                )
            }

            // DETAIL: Metadata and extended filter criteria
            AeonDetailSection(
                note = state.note,
                onNoteChange = { onEvent(LoanEntryEvent.SetNote(it)) },
                payee = state.counterparty,
                onPayeeChange = { onEvent(LoanEntryEvent.SetCounterparty(it)) },
                payeeLabel = if (state.loanType == LoanType.LENT) "BORROWER" else "LENDER",
                showNote = false,
                showPayee = false,
                showTags = false,
                selectedTags = emptyList(),
                noteSuggestions = state.uniqueNotes,
                payeeSuggestions = state.uniquePayees,
                additionalContent = {
                    Column(verticalArrangement = Arrangement.spacedBy(dims.spacingLg)) {
                        // Note Filter with Regex Toggle
                        Column(verticalArrangement = Arrangement.spacedBy(dims.spacingSm)) {
                            EntrySwitchRow(
                                label = "Use Regex for Note",
                                checked = state.isNoteRegex,
                                onCheckedChange = { onEvent(LoanEntryEvent.SetNoteRegex(it)) },
                                icon = Icons.Rounded.Code,
                                subtitle = "Supports /pattern/flags (i, m, s, x, d, c, l)"
                            )
                            EntryEditorialInput(
                                value = state.queryNote,
                                onValueChange = { onEvent(LoanEntryEvent.SetQueryNote(it)) },
                                label = "NOTE QUERY",
                                icon = Icons.AutoMirrored.Rounded.Notes,
                                placeholder = if (state.isNoteRegex) "e.g. /coffee/i or /^Rent/m" else "e.g. Starbucks coffee"
                            )
                        }

                        // Payee Filter with Regex Toggle
                        Column(verticalArrangement = Arrangement.spacedBy(dims.spacingSm)) {
                            EntrySwitchRow(
                                label = "Use Regex for Counterparty",
                                checked = state.isPayeeRegex,
                                onCheckedChange = { onEvent(LoanEntryEvent.SetPayeeRegex(it)) },
                                icon = Icons.Rounded.Code,
                                subtitle = "Supports /pattern/flags (i, m, s, x, d, c, l)"
                            )
                            EntryEditorialInput(
                                value = state.queryPayee,
                                onValueChange = { onEvent(LoanEntryEvent.SetQueryPayee(it)) },
                                label = if (state.loanType == LoanType.LENT) "BORROWER QUERY" else "LENDER QUERY",
                                icon = Icons.Rounded.Person,
                                placeholder = if (state.isPayeeRegex) "e.g. /Amazon/i" else "e.g. Amazon"
                            )
                        }

                        // Tags Selection
                        EntryTagSection(
                            selectedTags = state.selectedTags.toList(),
                            onShowTagSheet = { onEvent(LoanEntryEvent.ShowTagSheet(show = true)) },
                            onRemoveTag = { tag -> onEvent(LoanEntryEvent.SetTags(state.selectedTags - tag)) }
                        )

                        // Payment Method and Status multi-selection
                        EntryPaymentMethodStatusRow(
                            paymentMethod = when {
                                state.selectedPaymentMethods.isEmpty() -> "All Methods"
                                state.selectedPaymentMethods.size == PaymentMethod.entries.size -> "All Methods"
                                else -> "${state.selectedPaymentMethods.size} Selected"
                            },
                            onPaymentMethodClick = { onEvent(LoanEntryEvent.ShowPaymentMethodSheet(show = true)) },
                            statusLabel = when {
                                state.selectedStatuses.isEmpty() -> "All Statuses"
                                state.selectedStatuses.size == TransactionStatus.entries.size -> "All Statuses"
                                else -> "${state.selectedStatuses.size} Selected"
                            },
                            onStatusClick = { onEvent(LoanEntryEvent.ShowStatusSheet(show = true)) }
                        )

                        // Linked Transactions
                        EntryActionCard(
                            icon = Icons.Rounded.Link,
                            label = "LINKED TRANSACTIONS",
                            value = if (state.linkedTransactionIds.isEmpty()) "None" else "${state.linkedTransactionIds.size} Linked",
                            onClick = { onEvent(LoanEntryEvent.ShowLinkSheet(show = true)) },
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
                                onClick = { onEvent(LoanEntryEvent.ShowMinAmountSheet(show = true)) },
                                modifier = Modifier.weight(1f),
                                iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                iconTint = MaterialTheme.colorScheme.primary
                            )

                            // Max Amount
                            EntryActionCard(
                                icon = Icons.Rounded.ArrowUpward,
                                label = "MAX AMOUNT",
                                value = state.maxAmount.ifEmpty { "Any" },
                                onClick = { onEvent(LoanEntryEvent.ShowMaxAmountSheet(show = true)) },
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
                                    LoanEntryEvent.SetHasWarranty(
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
                                    onClick = { onEvent(LoanEntryEvent.ShowWarrantyStartPicker(show = true)) },
                                    modifier = Modifier.weight(1f),
                                    iconContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                    iconTint = MaterialTheme.colorScheme.secondary
                                )

                                EntryActionCard(
                                    icon = Icons.Rounded.CalendarToday,
                                    label = "WARRANTY END",
                                    value = state.warrantyExpirationEnd?.let { formatter.format(java.util.Date(it)) } ?: "Select",
                                    onClick = { onEvent(LoanEntryEvent.ShowWarrantyEndPicker(show = true)) },
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
                            onCheckedChange = { onEvent(LoanEntryEvent.SetExcludeFuture(it)) },
                            icon = Icons.Rounded.Schedule,
                            subtitle = "Don't include future transactions"
                        )

                        // Management settings for the loan itself
                        EntrySection(title = "MANAGEMENT", icon = Icons.Rounded.Settings) {
                            Column(verticalArrangement = Arrangement.spacedBy(dims.spacingMd)) {
                                // Has Settlement Date Toggle
                                EntrySwitchRow(
                                    label = "Has Settlement Date",
                                    checked = state.hasSettlementDate,
                                    onCheckedChange = { onEvent(LoanEntryEvent.SetHasSettlementDate(it)) },
                                    icon = Icons.Rounded.NotificationAdd,
                                    subtitle = "Enable to set a repayment deadline"
                                )
                            }
                        }
                    }
                }
            )
        }

        // Dedicated sheets for selecting multi-value parameters
        LoanEntrySheets(state = state, onEvent = onEvent)

        if (state.showSharedFilterDialog) {
            SharedFilterConfirmationDialog(
                onDismiss = { onEvent(LoanEntryEvent.ShowSharedFilterDialog(show = false)) },
                onConfirmUpdateShared = { onEvent(LoanEntryEvent.ConfirmSaveWithSharedFilter(updateShared = true)) },
                onConfirmCreateNew = { onEvent(LoanEntryEvent.ConfirmSaveWithSharedFilter(updateShared = false)) }
            )
        }
    }
}
