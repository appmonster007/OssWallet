package com.osscabinet.ossWallet.ui.screens.loans.entry.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.ui.components.getAccountIconByName
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.components.entry.*
import com.osscabinet.ossWallet.ui.screens.loans.entry.LoanEntryEvent
import com.osscabinet.ossWallet.ui.screens.loans.entry.LoanEntryState
import com.osscabinet.ossWallet.ui.theme.AeonTheme

@Composable
fun LoanFormBody(
    state: LoanEntryState,
    onEvent: (LoanEntryEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val amountColor = if (state.loanType == LoanType.LENT) Color(0xFF004D40) else Color(0xFFE65100)

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
                selectedType = state.loanType,
                onTypeSelected = { onEvent(LoanEntryEvent.SetLoanType(it)) },
                types = listOf(
                    Triple(Icons.Rounded.ArrowUpward, "Lent", LoanType.LENT),
                    Triple(Icons.Rounded.ArrowDownward, "Borrowed", LoanType.BORROWED)
                ),
                amount = state.principalAmount,
                currency = state.currency,
                onCurrencyClick = { onEvent(LoanEntryEvent.ShowNumpad(true)) },
                onAmountClick = { onEvent(LoanEntryEvent.ShowNumpad(true)) },
                showName = false,
                amountColor = amountColor
            )

            // CONTEXT: Action cards grid
            Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)) {
                // Row 1: Start Date and Interest Rate
                AeonContextGrid {
                    EntryDateCard(
                        icon = Icons.Rounded.CalendarToday,
                        label = "START DATE",
                        dateMillis = state.startDate,
                        onClick = { onEvent(LoanEntryEvent.ShowStartDatePicker(true)) },
                        modifier = Modifier.weight(1f)
                    )

                    EntryActionCard(
                        icon = Icons.Rounded.Percent,
                        label = "INTEREST",
                        value = if (state.interestRate.isEmpty()) "0%" else "${state.interestRate}%",
                        onClick = { onEvent(LoanEntryEvent.ShowInterestNumpad(true)) },
                        modifier = Modifier.weight(1f),
                        iconContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                        iconTint = MaterialTheme.colorScheme.tertiary
                    )
                }

                // Row 2: Account and Settlement Date
                AeonContextGrid {
                    val accountObj = state.accounts.find { it.id == state.linkedAccountId }
                    EntryActionCard(
                        icon = getAccountIconByName(accountObj?.icon),
                        label = "ACCOUNT",
                        value = accountObj?.name ?: "Select Account",
                        onClick = { onEvent(LoanEntryEvent.ShowAccountSheet(true)) },
                        modifier = Modifier.weight(1f),
                        iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        iconTint = MaterialTheme.colorScheme.primary
                    )

                    EntryActionCard(
                        icon = Icons.Rounded.EventAvailable,
                        label = "SETTLEMENT",
                        value = if (state.hasSettlementDate) {
                            val formatter = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
                            formatter.format(java.util.Date(state.settlementDate))
                        } else "None",
                        onClick = { onEvent(LoanEntryEvent.ShowSettlementDatePicker(true)) },
                        modifier = Modifier.weight(1f),
                        iconContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                        iconTint = MaterialTheme.colorScheme.secondary
                    )
                }

                // Row 3: Categories (Filters) (Full Width)
                val categoryObj = if (state.selectedCategoryIds.size == 1) state.categories.find { it.id == state.selectedCategoryIds.first() } else null
                EntryActionCard(
                    icon = categoryObj?.let { getIconByName(it.iconName) } ?: Icons.Rounded.Category,
                    label = "CATEGORY FILTERS",
                    value = when {
                        state.selectedCategoryIds.isEmpty() -> "0 Selected"
                        (state.categories.isNotEmpty() && state.selectedCategoryIds.size == state.categories.size) -> "All Categories"
                        categoryObj != null -> categoryObj.name
                        else -> "${state.selectedCategoryIds.size} Selected"
                    },
                    onClick = { onEvent(LoanEntryEvent.ShowCategorySheet(true)) },
                    modifier = Modifier.fillMaxWidth(),
                    iconContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                    iconTint = MaterialTheme.colorScheme.secondary
                )

                // Row 4: Linked Filter
                val filterObj = state.availableFilters.find { it.id == state.filterId }
                EntryActionCard(
                    icon = Icons.Rounded.FilterList,
                    label = "LINKED FILTER",
                    value = filterObj?.name ?: "None",
                    onClick = { onEvent(LoanEntryEvent.ShowFilterSheet(true)) },
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
                                onClick = { onEvent(LoanEntryEvent.MakeFilterUnique) },
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
                note = state.name,
                onNoteChange = { onEvent(LoanEntryEvent.SetName(it)) },
                payee = state.counterparty,
                onPayeeChange = { onEvent(LoanEntryEvent.SetCounterparty(it)) },
                payeeLabel = if (state.loanType == LoanType.LENT) "BORROWER" else "LENDER",
                payeeSuggestions = state.uniquePayees,
                selectedTags = state.selectedTags,
                onTagClick = { onEvent(LoanEntryEvent.ShowTagSheet(true)) },
                onRemoveTag = { tag -> onEvent(LoanEntryEvent.SetTags(state.selectedTags - tag)) },
                noteSuggestions = state.uniqueNotes,
                additionalContent = {
                    // Flow & Currencies
                    AeonContextGrid {
                        EntryActionCard(
                            icon = Icons.Rounded.ShowChart,
                            label = "FLOW",
                            value = when {
                                state.selectedTrackTypes.isEmpty() -> "0 Selected"
                                state.selectedTrackTypes.size >= 2 -> "Both"
                                else -> state.selectedTrackTypes.first().displayName
                            },
                            onClick = { onEvent(LoanEntryEvent.ShowTrackTypeSheet(true)) },
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
                            onClick = { onEvent(LoanEntryEvent.ShowCurrencyFilterSheet(true)) },
                            modifier = Modifier.weight(1f),
                            iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            iconTint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Payment Method and Status row
                    EntryPaymentMethodStatusRow(
                        paymentMethod = when {
                            state.selectedPaymentMethods.isEmpty() -> "0 Selected"
                            state.selectedPaymentMethods.size == PaymentMethod.entries.size -> "All Methods"
                            else -> "${state.selectedPaymentMethods.size} Selected"
                        },
                        onPaymentMethodClick = { onEvent(LoanEntryEvent.ShowPaymentMethodSheet(true)) },
                        statusLabel = when {
                            state.selectedStatuses.isEmpty() -> "0 Selected"
                            state.selectedStatuses.size == TransactionStatus.entries.size -> "All Statuses"
                            else -> "${state.selectedStatuses.size} Selected"
                        },
                        onStatusClick = { onEvent(LoanEntryEvent.ShowStatusSheet(true)) }
                    )

                    // Linked Transactions
                    EntryActionCard(
                        icon = Icons.Rounded.Link,
                        label = "LINKED TRANSACTIONS",
                        value = if (state.linkedTransactionIds.isEmpty()) "None" else "${state.linkedTransactionIds.size} Linked",
                        onClick = { onEvent(LoanEntryEvent.ShowLinkSheet(true)) },
                        modifier = Modifier.fillMaxWidth(),
                        iconContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                        iconTint = MaterialTheme.colorScheme.tertiary
                    )

                    // Has Settlement Date Toggle
                    EntrySwitchRow(
                        label = "Has Settlement Date",
                        checked = state.hasSettlementDate,
                        onCheckedChange = { onEvent(LoanEntryEvent.SetHasSettlementDate(it)) },
                        icon = Icons.Rounded.NotificationAdd,
                        subtitle = "Enable to set a repayment deadline"
                    )

                    // Additional Note field for actual loan note
                    EntryEditorialInput(
                        value = state.note,
                        onValueChange = { onEvent(LoanEntryEvent.SetNote(it)) },
                        label = "ADDITIONAL NOTES",
                        icon = Icons.AutoMirrored.Rounded.Notes
                    )
                }
            )
        }

        LoanEntrySheets(state = state, onEvent = onEvent)
    }
}
