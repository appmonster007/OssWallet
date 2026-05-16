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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.components.AeonTimelinePeriodSelector
import com.osscabinet.ossWallet.ui.components.getAccountIconByName
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.components.entry.*
import com.osscabinet.ossWallet.ui.screens.loans.entry.LoanEntryEvent
import com.osscabinet.ossWallet.ui.screens.loans.entry.LoanEntryState
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.WarningColor
import com.osscabinet.ossWallet.ui.theme.AppColors

/**
 * Loan Entry Form Body with Optimized Information Hierarchy.
 * LOAN CONTEXT (Top): Identity, Dates, Principal Account, and Loan Note.
 * REPAYMENT TRACKING (Below): Detailed filter criteria for repayment transactions.
 */
@Composable
fun LoanFormBody(
    state: LoanEntryState,
    onEvent: (LoanEntryEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val amountColor = if (state.loanType == LoanType.LENT) AppColors.primary else WarningColor
    val dims = AeonTheme.dimensions
    val formatter = remember { java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dims.screenPadding, vertical = dims.spacingLg),
            verticalArrangement = Arrangement.spacedBy(dims.spacingLg)
        ) {
            // IMPACT: Primary Identity (Loan Type & Principal)
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

            // LOAN CONTEXT: Core parameters of the loan agreement
            EntrySection(title = "LOAN PARAMETERS", icon = Icons.Rounded.Info) {
                Column(verticalArrangement = Arrangement.spacedBy(dims.spacingLg)) {
                    // Counterparty identity
                    EntryEditorialInput(
                        value = state.counterparty,
                        onValueChange = { onEvent(LoanEntryEvent.SetCounterparty(it)) },
                        label = if (state.loanType == LoanType.LENT) "BORROWER NAME" else "LENDER NAME",
                        icon = Icons.Rounded.Person,
                        placeholder = "Who is the other party?"
                    )

                    AeonContextGrid {
                        // Core Agreement parameters
                        EntryActionRow {
                            EntryActionCard(
                                icon = Icons.Rounded.CalendarToday,
                                label = "START DATE",
                                value = formatter.format(java.util.Date(state.startDate)),
                                onClick = { onEvent(LoanEntryEvent.ShowStartDatePicker(show = true)) },
                                modifier = Modifier.weight(1f),
                                iconContainerColor = AppColors.formIconContainerSecondary.copy(alpha = 0.1f),
                                iconTint = AppColors.secondary
                            )

                            EntryActionCard(
                                icon = Icons.Rounded.EventAvailable,
                                label = "SETTLEMENT",
                                value = if (state.hasSettlementDate) formatter.format(java.util.Date(state.settlementDate)) else "None Set",
                                onClick = { onEvent(LoanEntryEvent.ShowSettlementDatePicker(show = true)) },
                                modifier = Modifier.weight(1f),
                                iconContainerColor = AppColors.formIconContainerSecondary.copy(alpha = 0.1f),
                                iconTint = AppColors.secondary
                            )
                        }

                        EntryActionRow {
                            val accountObj = state.accounts.find { it.id == state.linkedAccountId }
                            EntryActionCard(
                                icon = getAccountIconByName(accountObj?.icon),
                                label = "PRINCIPAL ACCOUNT",
                                value = accountObj?.name ?: "Select...",
                                onClick = { onEvent(LoanEntryEvent.ShowAccountSheet(show = true)) },
                                modifier = Modifier.weight(1f),
                                iconContainerColor = AppColors.formIconContainerPrimary.copy(alpha = 0.1f),
                                iconTint = AppColors.primary
                            )

                            EntryActionCard(
                                icon = Icons.Rounded.Percent,
                                label = "INTEREST RATE",
                                value = if (state.interestRate.isEmpty()) "0%" else "${state.interestRate}%",
                                onClick = { onEvent(LoanEntryEvent.ShowInterestNumpad(show = true)) },
                                modifier = Modifier.weight(1f),
                                iconContainerColor = AppColors.formIconContainerTertiary.copy(alpha = 0.1f),
                                iconTint = AppColors.tertiary
                            )
                        }
                    }

                    // Loan's own note
                    EntryEditorialInput(
                        value = state.note,
                        onValueChange = { onEvent(LoanEntryEvent.SetNote(it)) },
                        label = "LOAN NOTE",
                        icon = Icons.AutoMirrored.Rounded.Notes,
                        placeholder = "Terms, conditions, or reminders..."
                    )
                }
            }

            // TRACKING FILTER: Criteria for identifying repayments
            EntrySection(title = "REPAYMENT TRACKING", icon = Icons.Rounded.FilterList) {
                Column(verticalArrangement = Arrangement.spacedBy(dims.spacingLg)) {
                    // Optimized Period Selection
                    AeonTimelinePeriodSelector(
                        currentRange = state.timelineRange,
                        onRangeSelected = { onEvent(LoanEntryEvent.SetTimelineRange(it)) },
                        swipeEntireComponent = false
                    )

                    // Filter Preset
                    val filterObj = state.availableFilters.find { it.id == state.filterId }
                    EntryActionCard(
                        icon = Icons.Rounded.Tune,
                        label = "FILTER PRESET",
                        value = filterObj?.name ?: "No Preset Applied",
                        onClick = { onEvent(LoanEntryEvent.ShowFilterSheet(show = true)) },
                        modifier = Modifier.fillMaxWidth(),
                        iconContainerColor = AppColors.formIconContainerPrimary.copy(alpha = 0.1f),
                        iconTint = AppColors.primary
                    )

                    AeonContextGrid {
                        // Repayment Currencies
                        EntryActionRow {
                            EntryActionCard(
                                icon = Icons.Rounded.CurrencyExchange,
                                label = "CURRENCIES",
                                value = if (state.selectedCurrencies.isEmpty()) "All Currencies" else if (state.selectedCurrencies.size == 1) state.selectedCurrencies.first() else "${state.selectedCurrencies.size} Selected",
                                onClick = { onEvent(LoanEntryEvent.ShowCurrencyFilterSheet(show = true)) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Categories & Flows
                        EntryActionRow {
                            val categoryObj = if (state.selectedCategoryIds.size == 1) state.categories.find { it.id == state.selectedCategoryIds.first() } else null
                            EntryActionCard(
                                icon = categoryObj?.let { getIconByName(it.iconName) } ?: Icons.Rounded.Category,
                                label = "CATEGORIES",
                                value = if (state.selectedCategoryIds.isEmpty()) "All Categories" else categoryObj?.name ?: "${state.selectedCategoryIds.size} Selected",
                                onClick = { onEvent(LoanEntryEvent.ShowCategorySheet(show = true)) },
                                modifier = Modifier.weight(1f)
                            )

                            EntryActionCard(
                                icon = Icons.AutoMirrored.Rounded.ShowChart,
                                label = "FLOWS",
                                value = if (state.selectedTrackTypes.size >= 2 || state.selectedTrackTypes.isEmpty()) "All Flows" else state.selectedTrackTypes.first().displayName,
                                onClick = { onEvent(LoanEntryEvent.ShowTrackTypeSheet(show = true)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // RESTORED: Advanced Search Queries with Regex Toggles
                    Column(verticalArrangement = Arrangement.spacedBy(dims.spacingMd)) {
                        // Note Query
                        Column(verticalArrangement = Arrangement.spacedBy(dims.spacingSm)) {
                            EntrySwitchRow(
                                label = "Regex for Note",
                                checked = state.isNoteRegex,
                                onCheckedChange = { onEvent(LoanEntryEvent.SetNoteRegex(it)) },
                                icon = Icons.Rounded.Code,
                                subtitle = "Supports /pattern/flags"
                            )
                            EntryEditorialInput(
                                value = state.queryNote,
                                onValueChange = { onEvent(LoanEntryEvent.SetQueryNote(it)) },
                                label = "NOTE QUERY",
                                icon = Icons.AutoMirrored.Rounded.Notes,
                                placeholder = if (state.isNoteRegex) "/pattern/i" else "Match note text..."
                            )
                        }

                        // Counterparty Query
                        Column(verticalArrangement = Arrangement.spacedBy(dims.spacingSm)) {
                            EntrySwitchRow(
                                label = "Regex for Counterparty",
                                checked = state.isPayeeRegex,
                                onCheckedChange = { onEvent(LoanEntryEvent.SetPayeeRegex(it)) },
                                icon = Icons.Rounded.Code,
                                subtitle = "Supports /pattern/flags"
                            )
                            EntryEditorialInput(
                                value = state.queryPayee,
                                onValueChange = { onEvent(LoanEntryEvent.SetQueryPayee(it)) },
                                label = "COUNTERPARTY QUERY",
                                icon = Icons.Rounded.Person,
                                placeholder = if (state.isPayeeRegex) "/pattern/i" else "Match name..."
                            )
                        }
                    }

                    // Tags
                    EntryTagSection(
                        tags = state.selectedTags.map { name -> 
                            state.availableTags.find { it.name == name } ?: com.osscabinet.ossWallet.domain.model.Tag(name = name)
                        },
                        onShowTagSheet = { onEvent(LoanEntryEvent.ShowTagSheet(show = true)) },
                        onRemoveTag = { tag -> onEvent(LoanEntryEvent.SetTags(state.selectedTags - tag)) }
                    )

                    // RESTORED: Payment Method and Status Filters
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

                    // Repayment Linking
                    EntryActionCard(
                        icon = Icons.Rounded.Link,
                        label = "LINKED REPAYMENTS",
                        value = if (state.linkedTransactionIds.isEmpty()) "None" else "${state.linkedTransactionIds.size} Linked",
                        onClick = { onEvent(LoanEntryEvent.ShowLinkSheet(show = true)) },
                        modifier = Modifier.fillMaxWidth(),
                        iconContainerColor = AppColors.formIconContainerTertiary.copy(alpha = 0.1f),
                        iconTint = AppColors.tertiary
                    )

                    // Constraints
                    AeonContextGrid {
                        EntryActionRow {
                            EntryActionCard(
                                icon = Icons.Rounded.ArrowDownward,
                                label = "MIN AMOUNT",
                                value = state.minAmount.ifEmpty { "Any" },
                                onClick = { onEvent(LoanEntryEvent.ShowMinAmountSheet(show = true)) },
                                modifier = Modifier.weight(1f)
                            )
                            EntryActionCard(
                                icon = Icons.Rounded.ArrowUpward,
                                label = "MAX AMOUNT",
                                value = state.maxAmount.ifEmpty { "Any" },
                                onClick = { onEvent(LoanEntryEvent.ShowMaxAmountSheet(show = true)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Warranty
                    EntryActionCard(
                        icon = Icons.Rounded.Verified,
                        label = "WARRANTY STATUS",
                        value = when (state.hasWarranty) {
                            null -> "Any"
                            true -> "Has Warranty"
                            false -> "No Warranty"
                        },
                        onClick = { onEvent(LoanEntryEvent.SetHasWarranty(if (state.hasWarranty == true) false else if (state.hasWarranty == false) null else true)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (state.hasWarranty == true) {
                        AeonContextGrid {
                            EntryActionRow {
                                EntryActionCard(
                                    icon = Icons.Rounded.CalendarToday,
                                    label = "WARRANTY START",
                                    value = state.warrantyExpirationStart?.let { formatter.format(java.util.Date(it)) } ?: "Select",
                                    onClick = { onEvent(LoanEntryEvent.ShowWarrantyStartPicker(show = true)) },
                                    modifier = Modifier.weight(1f)
                                )
                                EntryActionCard(
                                    icon = Icons.Rounded.CalendarToday,
                                    label = "WARRANTY END",
                                    value = state.warrantyExpirationEnd?.let { formatter.format(java.util.Date(it)) } ?: "Select",
                                    onClick = { onEvent(LoanEntryEvent.ShowWarrantyEndPicker(show = true)) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    EntrySwitchRow(
                        label = "Exclude Future",
                        checked = state.excludeFuture,
                        onCheckedChange = { onEvent(LoanEntryEvent.SetExcludeFuture(it)) },
                        icon = Icons.Rounded.Schedule,
                        subtitle = "Ignore future transactions"
                    )
                }
            }

            // MANAGEMENT: Configuration
            EntrySection(title = "MANAGEMENT", icon = Icons.Rounded.Settings) {
                EntrySwitchRow(
                    label = "Settlement Deadline",
                    checked = state.hasSettlementDate,
                    onCheckedChange = { onEvent(LoanEntryEvent.SetHasSettlementDate(it)) },
                    icon = Icons.Rounded.NotificationAdd,
                    subtitle = "Set a date for full repayment"
                )
            }
        }

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
