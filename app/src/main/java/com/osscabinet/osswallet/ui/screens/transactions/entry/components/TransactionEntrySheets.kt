package com.osscabinet.ossWallet.ui.screens.transactions.entry.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.osscabinet.ossWallet.R
import com.osscabinet.ossWallet.ui.components.AeonDateTimePicker
import com.osscabinet.ossWallet.ui.components.ExchangeRateDialog
import com.osscabinet.ossWallet.ui.components.entry.AeonAccountSelection
import com.osscabinet.ossWallet.ui.components.entry.AeonCalculatorSheet
import com.osscabinet.ossWallet.domain.model.AmountEditingSide
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.ui.components.entry.AeonCategorySelection
import com.osscabinet.ossWallet.ui.components.entry.AeonLinkedTransactionSelection
import com.osscabinet.ossWallet.ui.components.entry.AeonLocationSelection
import com.osscabinet.ossWallet.ui.components.entry.AeonPaymentMethodSelection
import com.osscabinet.ossWallet.ui.components.entry.AeonStatusSelection
import com.osscabinet.ossWallet.ui.components.entry.AeonTagSelectionDialog
import com.osscabinet.ossWallet.ui.components.entry.AeonTemplateSelection
import com.osscabinet.ossWallet.ui.components.entry.AeonTransactionSplitSheet
import com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryEvent
import com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryState

/**
 * Container for all bottom sheets used in transaction entry.
 * Uses shared components to ensure consistency.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEntrySheets(
    state: TransactionEntryState,
    onEvent: (TransactionEntryEvent) -> Unit,
    onSearch: (String) -> kotlinx.coroutines.flow.Flow<List<com.osscabinet.ossWallet.domain.model.Transaction>>
) {
    // Date/Time Picker
    if (state.showDatePicker) {
        AeonDateTimePicker(
            initialDateTime = state.selectedDateMillis,
            onDateTimeSelected = { millis ->
                onEvent(TransactionEntryEvent.SetDate(millis))
                onEvent(TransactionEntryEvent.ShowDatePicker(false))
            },
            onDismiss = { onEvent(TransactionEntryEvent.ShowDatePicker(false)) }
        )
    }

    // Category Selection
    if (state.showCategorySheet) {
        AeonCategorySelection(
            categories = state.categories.filter { !Category.isInitialBalance(it.id) },
            selectedCategoryIds = setOf(state.selectedCategoryId),
            onSelectionChange = { ids ->
                ids.firstOrNull()?.let { id ->
                    onEvent(TransactionEntryEvent.SetCategory(id))
                }
            },
            onDismiss = { onEvent(TransactionEntryEvent.ShowCategorySheet(false)) }
        )
    }

    // Account Selection
    if (state.showAccountSheet) {
        val sourceSelectedIds = state.selectedAccount?.id?.let { setOf(it) }
            ?: if (state.isTransfer && state.source.accountId == null) setOf("") else emptySet()
            
        // Filter out Target account if it's already selected and not "Outside"
        val filteredAccounts = state.accounts.filter { 
            it.id != state.destination.accountId || state.destination.accountId == null 
        }

        AeonAccountSelection(
            accounts = filteredAccounts,
            selectedAccountIds = sourceSelectedIds,
            onSelectionChange = { ids ->
                ids.firstOrNull()?.let { id ->
                    onEvent(TransactionEntryEvent.SetAccount(state.accounts.find { it.id == id }))
                }
            },
            onDismiss = { onEvent(TransactionEntryEvent.ShowAccountSheet(false)) },
            // Only allow Outside if Target is managed
            allowOutsideWallet = state.isTransfer && state.destination.accountId != null
        )
    }

    // Target Account Selection (for transfers)
    if (state.showTargetAccountSheet) {
        // Filter out Source account if it's already selected and not "Outside"
        val filteredAccounts = state.accounts.filter { 
            it.id != state.selectedAccount?.id || state.selectedAccount?.id == null 
        }

        AeonAccountSelection(
            accounts = filteredAccounts,
            selectedAccountIds = state.targetAccount?.id?.let { setOf(it) } ?: emptySet(),
            onSelectionChange = { ids ->
                ids.firstOrNull()?.let { id ->
                    onEvent(TransactionEntryEvent.SetTargetAccount(state.accounts.find { it.id == id }))
                }
            },
            onDismiss = { onEvent(TransactionEntryEvent.ShowTargetAccountSheet(false)) },
            title = stringResource(R.string.target_account_selection_title),
            // Only allow Outside if Source is managed
            allowOutsideWallet = state.source.accountId != null
        )
    }

    // Template Selection
    if (state.showTemplateSheet) {
        AeonTemplateSelection(
            templates = state.templates,
            categories = state.categories,
            onTemplateSelected = { template ->
                if (template != null) {
                    onEvent(TransactionEntryEvent.ApplyTemplate(template))
                }
                onEvent(TransactionEntryEvent.ShowTemplateSheet(false))
            },
            onDismiss = { onEvent(TransactionEntryEvent.ShowTemplateSheet(false)) },
            showCreateNew = false
        )
    }

    // Linked Transaction Selection
    if (state.showLinkedTransactionSheet) {
        AeonLinkedTransactionSelection(
            onSearch = onSearch,
            currentTransactionId = state.transactionId,
            linkedIds = state.linkedTransactionIds.toSet(),
            accounts = state.accounts,
            categories = state.categories,
            allTags = state.allTags,
            onLinkedTransactionsChanged = { ids ->
                onEvent(TransactionEntryEvent.SetLinkedTransactionIds(ids.toList()))
            },
            onDismiss = { onEvent(TransactionEntryEvent.ShowLinkedTransactionSheet(false)) }
        )
    }

    // Tag Selection
    if (state.showTagSheet) {
        AeonTagSelectionDialog(
            tags = state.allTags,
            selectedTags = state.selectedTags,
            onTagsSelected = { tags ->
                onEvent(TransactionEntryEvent.SetTags(tags))
            },
            onAddTag = { tag, color -> onEvent(TransactionEntryEvent.AddTag(tag, color)) },
            onDismiss = { onEvent(TransactionEntryEvent.ShowTagSheet(false)) }
        )
    }

    // Payment Method Selection
    if (state.showPaymentMethodSheet) {
        AeonPaymentMethodSelection(
            selectedMethods = setOf(state.paymentMethod),
            onSelectionChange = { methods ->
                methods.firstOrNull()?.let { method ->
                    onEvent(TransactionEntryEvent.SetPaymentMethod(method))
                }
            },
            onDismiss = { onEvent(TransactionEntryEvent.ShowPaymentMethodSheet(false)) }
        )
    }

    // Status Selection
    if (state.showStatusSheet) {
        AeonStatusSelection(
            selectedStatuses = setOf(state.status),
            onSelectionChange = { statuses ->
                statuses.firstOrNull()?.let { status ->
                    onEvent(TransactionEntryEvent.SetStatus(status))
                }
            },
            onDismiss = { onEvent(TransactionEntryEvent.ShowStatusSheet(false)) }
        )
    }

    // Warranty Date Picker
    if (state.showWarrantyDatePicker) {
        AeonDateTimePicker(
            initialDateTime = state.warrantyExpirationMillis,
            onDateTimeSelected = { millis ->
                onEvent(TransactionEntryEvent.SetWarrantyExpiration(millis))
                onEvent(TransactionEntryEvent.ShowWarrantyDatePicker(false))
            },
            onDismiss = { onEvent(TransactionEntryEvent.ShowWarrantyDatePicker(false)) }
        )
    }

    // Location Selection
    if (state.showLocationSheet) {
        AeonLocationSelection(
            gpsLocation = state.gpsLocation,
            onLocationSelected = { gpsStr, locationName ->
                onEvent(TransactionEntryEvent.SetLocation(gpsStr, locationName))
                onEvent(TransactionEntryEvent.ShowLocationSheet(false))
            },
            onDismiss = { onEvent(TransactionEntryEvent.ShowLocationSheet(false)) }
        )
    }

    // Split Transaction Screen
    if (state.showSplitSheet) {
        AeonTransactionSplitSheet(
            totalAmount = state.amountValue,
            currency = state.selectedCurrency,
            splits = state.splits,
            categories = state.categories,
            onAddSplit = { onEvent(TransactionEntryEvent.AddSplit) },
            onRemoveSplit = { index -> onEvent(TransactionEntryEvent.RemoveSplit(index)) },
            onUpdateSplit = { index, amount, note, categoryId ->
                onEvent(
                    TransactionEntryEvent.UpdateSplit(
                        index,
                        amount = amount,
                        note = note,
                        categoryId = categoryId
                    )
                )
            },
            onShowCategorySheet = { index ->
                onEvent(
                    TransactionEntryEvent.ShowSplitCategorySheet(
                        index
                    )
                )
            },
            onToggleSplitMode = { onEvent(TransactionEntryEvent.ToggleSplitMode(it)) },
            onDismiss = { onEvent(TransactionEntryEvent.ShowSplitSheet(false)) }
        )
    }

    // Split Category Selection
    state.showSplitCategorySheet?.let { index ->
        AeonCategorySelection(
            categories = state.categories.filter { !Category.isInitialBalance(it.id) },
            selectedCategoryIds = setOf(
                state.splits.getOrNull(index)?.categoryId ?: state.selectedCategoryId
            ),
            onSelectionChange = { ids ->
                ids.firstOrNull()?.let { id ->
                    onEvent(TransactionEntryEvent.UpdateSplit(index, categoryId = id))
                }
            },
            onDismiss = { onEvent(TransactionEntryEvent.ShowSplitCategorySheet(null)) }
        )
    }

    // Numpad (Calculator)
    if (state.isNumpadVisible) {
        AeonCalculatorSheet(
            currentAmount = if (state.activeAmountSide == AmountEditingSide.SOURCE) state.amount else state.targetAmount,
            onAmountChanged = { amount ->
                if (state.activeAmountSide == AmountEditingSide.SOURCE) {
                    onEvent(TransactionEntryEvent.SetAmount(amount))
                } else {
                    onEvent(TransactionEntryEvent.SetTargetAmount(amount))
                }
            },
            onDismiss = { onEvent(TransactionEntryEvent.ShowNumpad(false)) }
        )
    }

    // Exchange Rate Dialog (for transfers)
    if (state.showExchangeRateDialog) {
        ExchangeRateDialog(
            initialRate = state.exchangeRate,
            onDismiss = { onEvent(TransactionEntryEvent.ShowExchangeRateDialog(false)) },
            onConfirm = { rate ->
                onEvent(TransactionEntryEvent.SetExchangeRate(rate))
                onEvent(TransactionEntryEvent.ShowExchangeRateDialog(false))
            }
        )
    }

}
