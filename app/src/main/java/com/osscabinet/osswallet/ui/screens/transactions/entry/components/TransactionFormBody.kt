package com.osscabinet.ossWallet.ui.screens.transactions.entry.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.components.entry.*
import com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryEvent
import com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryState
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors

/**
 * Main content layout for transaction entry screen.
 * Optimized with shared components and unified metadata section.
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
                .padding(
                    horizontal = AeonTheme.dimensions.screenPadding,
                    vertical = AeonTheme.dimensions.spacingLg
                ),
            verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg)
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
            AeonContextGrid {
                // Row 1: Template and Date
                EntryActionRow {
                    if (!state.isEditMode) {
                        EntryActionCard(
                            icon = Icons.Rounded.Bookmark,
                            label = "TEMPLATE",
                            value = state.selectedTemplateName ?: "None",
                            onClick = { onEvent(TransactionEntryEvent.ShowTemplateSheet(true)) },
                            modifier = Modifier.weight(1f),
                            iconContainerColor = AppColors.formIconContainerPrimary.copy(alpha = 0.1f),
                            iconTint = AppColors.primary
                        )
                    }

                    EntryActionCard(
                        icon = Icons.Rounded.CalendarToday,
                        label = "DATE",
                        value = formatDate(state.selectedDateMillis),
                        onClick = { onEvent(TransactionEntryEvent.ShowDatePicker(true)) },
                        modifier = Modifier.weight(1f),
                        iconContainerColor = AppColors.formIconContainerSecondary.copy(alpha = 0.1f),
                        iconTint = AppColors.secondary
                    )
                }

                // Row 2: Account and Category/Target
                AeonEntryContextRow(
                    isTransfer = state.isTransfer,
                    selectedAccount = state.selectedAccount,
                    onAccountClick = { onEvent(TransactionEntryEvent.ShowAccountSheet(true)) },
                    selectedCategory = state.categories.find { it.id == state.selectedCategoryId },
                    onCategoryClick = { onEvent(TransactionEntryEvent.ShowCategorySheet(true)) },
                    targetAccount = state.targetAccount,
                    onTargetAccountClick = { onEvent(TransactionEntryEvent.ShowTargetAccountSheet(true)) },
                    isSplitMode = state.isSplitMode,
                    splitCount = state.splits.size,
                    onSplitClick = { onEvent(TransactionEntryEvent.ShowSplitSheet(true)) }
                )
            }

            // DETAIL: Metadata section
            AeonDetailSection(
                note = if (state.isSplitMode) "Detail notes entered in splits" else state.note,
                onNoteChange = { if (!state.isSplitMode) onEvent(TransactionEntryEvent.SetNote(it)) },
                payee = state.payee,
                onPayeeChange = { onEvent(TransactionEntryEvent.SetPayee(it)) },
                selectedTags = state.selectedTags.map { name -> 
                    state.allTags.find { it.name == name } ?: com.osscabinet.ossWallet.domain.model.Tag(name = name)
                },
                onTagClick = { onEvent(TransactionEntryEvent.ShowTagSheet(true)) },
                onRemoveTag = { tag -> onEvent(TransactionEntryEvent.RemoveTag(tag)) },
                noteSuggestions = state.allNotes,
                payeeSuggestions = state.allPayees,
                additionalContent = {
                    AeonEntryMetadataSection(
                        state = AeonMetadataState(
                            paymentMethod = state.paymentMethod,
                            statusLabel = state.status.displayName,
                            statusIcon = state.status.iconName,
                            statusColor = state.status.colorHex,
                            isWarrantyEnabled = state.isWarrantyEnabled,
                            warrantyExpiration = state.warrantyExpirationMillis,
                            location = state.locationCoords,
                            linkedTransactions = state.displayLinkedTxs
                        ),
                        accounts = state.accounts,
                        categories = state.categories,
                        is24Hour = state.use24HourFormat,
                        onShowLinkedTransactions = { onEvent(TransactionEntryEvent.ShowLinkedTransactionSheet(true)) },
                        onRemoveLink = { txId -> onEvent(TransactionEntryEvent.RemoveLinkedTransaction(state.transactionId ?: -1L, txId)) },
                        onPaymentMethodClick = { onEvent(TransactionEntryEvent.ShowPaymentMethodSheet(true)) },
                        onStatusClick = { onEvent(TransactionEntryEvent.ShowStatusSheet(true)) },
                        onWarrantyToggle = { onEvent(TransactionEntryEvent.SetWarrantyEnabled(it)) },
                        onWarrantyDateClick = { onEvent(TransactionEntryEvent.ShowWarrantyDatePicker(true)) },
                        onLocationClick = { onEvent(TransactionEntryEvent.ShowLocationSheet(true)) },
                        locationLabel = "Transaction Location"
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
