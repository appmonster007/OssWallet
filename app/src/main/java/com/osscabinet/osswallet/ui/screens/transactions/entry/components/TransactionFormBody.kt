package com.osscabinet.ossWallet.ui.screens.transactions.entry.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.components.AppIconMap
import com.osscabinet.ossWallet.ui.components.getAccountIconByName
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.components.entry.*
import com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryEvent
import com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryState

/**
 * Main content layout for transaction entry screen.
 * Optimized with shared components and inlined logic to reduce component bloat.
 */
@Composable
fun TransactionFormBody(
    state: TransactionEntryState,
    onEvent: (TransactionEntryEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // IMPACT: Header card containing transaction type selector and amount input
            AeonImpactHeaderCard(
                transactionType = state.transactionType,
                onTypeSelected = { onEvent(TransactionEntryEvent.SetTransactionType(it)) },
                amount = state.amount,
                currency = state.selectedCurrency,
                onCurrencyClick = { onEvent(TransactionEntryEvent.ShowNumpad(true)) },
                onAmountClick = { onEvent(TransactionEntryEvent.ShowNumpad(true)) },
                showName = false,
                isTransfer = state.isTransfer,
                targetAccountId = state.targetAccount?.id,
                targetCurrency = state.targetCurrency,
                onTargetCurrencyClick = { onEvent(TransactionEntryEvent.ShowTargetCurrencySheet(true)) },
                transferRate = state.exchangeRate,
                onExchangeRateClick = { onEvent(TransactionEntryEvent.ShowExchangeRateDialog(true)) },
                isSplitMode = state.isSplitMode,
                onSplitClick = {
                    if (!state.isSplitMode) {
                        onEvent(TransactionEntryEvent.ToggleSplitMode(true))
                    }
                    onEvent(TransactionEntryEvent.ShowSplitSheet(true))
                },
                supportedCurrencies = state.selectedAccount?.allowedCurrencies ?: listOf("USD")
            )

            // CONTEXT: Action cards row: Template, Date, Account, Category
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Row 1: Template and Date
                AeonContextGrid {
                    if (!state.isEditMode) {
                        EntryActionCard(
                            icon = Icons.Rounded.Bookmark,
                            label = "TEMPLATE",
                            value = state.selectedTemplateName ?: "None",
                            onClick = { onEvent(TransactionEntryEvent.ShowTemplateSheet(true)) },
                            modifier = Modifier.weight(1f),
                            iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            iconTint = MaterialTheme.colorScheme.primary
                        )
                    }

                    EntryActionCard(
                        icon = Icons.Rounded.CalendarToday,
                        label = "DATE",
                        value = formatDate(state.selectedDateMillis),
                        onClick = { onEvent(TransactionEntryEvent.ShowDatePicker(true)) },
                        modifier = Modifier.weight(1f),
                        iconContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                        iconTint = MaterialTheme.colorScheme.secondary
                    )
                }

                // Row 2: Account and Category/Target
                AeonContextGrid {
                    val accountIcon = getAccountIconByName(state.selectedAccount?.icon)
                    EntryActionCard(
                        icon = accountIcon,
                        label = if (state.isTransfer) "FROM" else "ACCOUNT",
                        value = state.selectedAccount?.name ?: "Select",
                        onClick = { onEvent(TransactionEntryEvent.ShowAccountSheet(true)) },
                        modifier = Modifier.weight(1f),
                        iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        iconTint = MaterialTheme.colorScheme.primary
                    )

                    if (state.isTransfer) {
                        val targetDisplay = state.targetAccount?.name ?: "Outside Wallet"
                        val targetIcon = getAccountIconByName(state.targetAccount?.icon)
                        EntryActionCard(
                            icon = targetIcon,
                            label = "TO",
                            value = targetDisplay,
                            onClick = { onEvent(TransactionEntryEvent.ShowTargetAccountSheet(true)) },
                            modifier = Modifier.weight(1f),
                            iconContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                            iconTint = MaterialTheme.colorScheme.tertiary
                        )
                    } else {
                        val categoryObj = state.categories.find { it.id == state.selectedCategoryId }
                        val displayCategoryName = if (state.isSplitMode) {
                            "Split Transaction (${state.splits.size})"
                        } else {
                            categoryObj?.name ?: "Unknown"
                        }
                        val categoryIcon = if (state.isSplitMode) {
                            Icons.Rounded.Splitscreen
                        } else {
                            getIconByName(categoryObj?.iconName)
                        }

                        EntryActionCard(
                            icon = categoryIcon,
                            label = if (state.isSplitMode) "TRANSACTION" else "CATEGORY",
                            value = displayCategoryName,
                            onClick = { 
                                if (state.isSplitMode) {
                                    onEvent(TransactionEntryEvent.ShowSplitSheet(true))
                                } else {
                                    onEvent(TransactionEntryEvent.ShowCategorySheet(true))
                                }
                            },
                            modifier = Modifier.weight(1f),
                            iconContainerColor = if (state.isSplitMode) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            } else {
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                            },
                            iconTint = if (state.isSplitMode) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.tertiary
                            }
                        )
                    }
                }
            }

            // DETAIL: Metadata section
            AeonDetailSection(
                note = if (state.isSplitMode) "Detail notes entered in splits" else state.note,
                onNoteChange = { if (!state.isSplitMode) onEvent(TransactionEntryEvent.SetNote(it)) },
                payee = state.payee,
                onPayeeChange = { onEvent(TransactionEntryEvent.SetPayee(it)) },
                selectedTags = state.selectedTags,
                onTagClick = { onEvent(TransactionEntryEvent.ShowTagSheet(true)) },
                onRemoveTag = { tag -> onEvent(TransactionEntryEvent.RemoveTag(tag)) },
                noteSuggestions = state.allNotes,
                payeeSuggestions = state.allPayees,
                additionalContent = {
                    // Linked Transactions
                    EntryLinkedTransactionsSection(
                        linkedTransactions = state.displayLinkedTxs,
                        accounts = state.accounts,
                        categories = state.categories,
                        is24Hour = state.use24HourFormat,
                        onShowSheet = { onEvent(TransactionEntryEvent.ShowLinkedTransactionSheet(true)) },
                        onRemoveLink = { txId ->
                            onEvent(TransactionEntryEvent.RemoveLinkedTransaction(state.transactionId ?: -1L, txId))
                        }
                    )

                    // Payment Method and Status row
                    val statusColor = Color(android.graphics.Color.parseColor(state.status.colorHex))
                    val statusIcon = AppIconMap[state.status.iconName] ?: Icons.Rounded.CheckCircle
                    val paymentIcon = AppIconMap[state.paymentMethod.iconName] ?: Icons.Rounded.Payments

                    EntryPaymentMethodStatusRow(
                        paymentMethod = state.paymentMethod.displayName,
                        onPaymentMethodClick = { onEvent(TransactionEntryEvent.ShowPaymentMethodSheet(true)) },
                        statusLabel = state.status.displayName,
                        onStatusClick = { onEvent(TransactionEntryEvent.ShowStatusSheet(true)) },
                        paymentMethodIcon = paymentIcon,
                        statusIcon = statusIcon,
                        statusIconTint = statusColor,
                        statusContainerColor = statusColor.copy(alpha = 0.1f)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                    )

                    // Warranty
                    EntryWarrantySection(
                        enabled = state.isWarrantyEnabled,
                        onToggle = { onEvent(TransactionEntryEvent.SetWarrantyEnabled(it)) },
                        expirationMillis = state.warrantyExpirationMillis,
                        onDateClick = { onEvent(TransactionEntryEvent.ShowWarrantyDatePicker(true)) }
                    )

                    // Location
                    EntryLocationRow(
                        coords = state.locationCoords,
                        onShowLocationSheet = { onEvent(TransactionEntryEvent.ShowLocationSheet(true)) },
                        label = "Transaction Location"
                    )
                }
            )
        }

        // Bottom sheets and dialogs
        TransactionEntrySheets(
            state = state,
            onEvent = onEvent
        )
    }
}

private fun formatDate(millis: Long): String {
    val formatter = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
    return formatter.format(java.util.Date(millis))
}
