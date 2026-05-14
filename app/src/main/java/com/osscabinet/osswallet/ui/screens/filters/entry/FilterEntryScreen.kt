package com.osscabinet.ossWallet.ui.screens.filters.entry

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.ui.components.AeonDatePickerDialog
import com.osscabinet.ossWallet.ui.components.AeonSelectMode
import com.osscabinet.ossWallet.ui.components.entry.*
import com.osscabinet.ossWallet.ui.screens.filters.entry.components.FilterFormBody

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterEntryScreen(
    filterId: Long? = null,
    viewModel: FilterEntryViewModel,
    onBackClick: () -> Unit
) {
    LaunchedEffect(filterId) {
        viewModel.initialize(filterId)
    }

    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(state.navTriggered) {
        if (state.navTriggered) {
            onBackClick()
        }
    }

    AeonEntryScaffold(
        title = if (state.isEditMode) "Edit Filter" else "New Filter",
        onBackClick = onBackClick,
        onSaveClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.saveFilter {
                viewModel.onEvent(FilterEntryEvent.TriggerNavigation)
            }
        },
        onResetClick = {
            viewModel.onEvent(FilterEntryEvent.ResetFilter)
        },
        canSave = state.canSave && !state.navTriggered,
        isEditMode = state.isEditMode,
        onDeleteClick = if (state.isEditMode) {
            {
                viewModel.deleteFilter()
            }
        } else null
    ) { innerPadding ->
        FilterFormBody(
            name = state.name,
            onNameChange = { viewModel.onEvent(FilterEntryEvent.SetName(it)) },
            transactionType = state.transactionType,
            onTypeClick = { viewModel.onEvent(FilterEntryEvent.ShowTypeSheet(true)) },
            selectedAccountIds = state.selectedAccountIds,
            accounts = state.accounts,
            onAccountClick = { viewModel.onEvent(FilterEntryEvent.ShowAccountSheet(true)) },
            selectedCategoryIds = state.selectedCategoryIds,
            categories = state.categories,
            onCategoryClick = { viewModel.onEvent(FilterEntryEvent.ShowCategorySheet(true)) },
            selectedPaymentMethods = state.selectedPaymentMethods,
            onPaymentMethodClick = { viewModel.onEvent(FilterEntryEvent.ShowPaymentMethodSheet(true)) },
            selectedStatuses = state.selectedStatuses,
            onStatusClick = { viewModel.onEvent(FilterEntryEvent.ShowStatusSheet(true)) },
            linkedTransactionIds = state.linkedTransactionIds,
            onLinkRecordClick = { viewModel.onEvent(FilterEntryEvent.ShowLinkSheet(true)) },
            selectedTags = state.selectedTags,
            onShowTagSheet = { viewModel.onEvent(FilterEntryEvent.ShowTagSheet(true)) },
            onRemoveTag = { tag -> viewModel.onEvent(FilterEntryEvent.SetTags(state.selectedTags - tag)) },
            selectedCurrencies = state.selectedCurrencies,
            onCurrencyClick = { viewModel.onEvent(FilterEntryEvent.ShowCurrencySheet(true)) },
            queryNote = state.queryNote,
            onQueryNoteChange = { viewModel.onEvent(FilterEntryEvent.SetQueryNote(it)) },
            isNoteRegex = state.isNoteRegex,
            onNoteRegexChange = { viewModel.onEvent(FilterEntryEvent.SetNoteRegex(it)) },
            queryPayee = state.queryPayee,
            onQueryPayeeChange = { viewModel.onEvent(FilterEntryEvent.SetQueryPayee(it)) },
            isPayeeRegex = state.isPayeeRegex,
            onPayeeRegexChange = { viewModel.onEvent(FilterEntryEvent.SetPayeeRegex(it)) },
            minAmount = state.minAmount,
            onMinAmountClick = { viewModel.onEvent(FilterEntryEvent.ShowMinAmountSheet(true)) },
            maxAmount = state.maxAmount,
            onMaxAmountClick = { viewModel.onEvent(FilterEntryEvent.ShowMaxAmountSheet(true)) },
            hasWarranty = state.hasWarranty,
            onWarrantyClick = { viewModel.onEvent(FilterEntryEvent.SetHasWarranty(
                when (state.hasWarranty) {
                    null -> true
                    true -> false
                    false -> null
                }
            )) },
            warrantyExpirationStart = state.warrantyExpirationStart,
            onWarrantyStartClick = { viewModel.onEvent(FilterEntryEvent.ShowWarrantyStartPicker(true)) },
            warrantyExpirationEnd = state.warrantyExpirationEnd,
            onWarrantyEndClick = { viewModel.onEvent(FilterEntryEvent.ShowWarrantyEndPicker(true)) },
            excludeFuture = state.excludeFuture,
            onExcludeFutureChange = { viewModel.onEvent(FilterEntryEvent.SetExcludeFuture(it)) },
            modifier = Modifier.padding(innerPadding)
        )

        // Selection Sheets
        if (state.showTypeSheet) {
            AeonSelectionSheet(
                title = "Transaction Type",
                onDismiss = { viewModel.onEvent(FilterEntryEvent.ShowTypeSheet(false)) },
                items = listOf(null) + com.osscabinet.ossWallet.domain.model.TransactionType.entries.filter { it != com.osscabinet.ossWallet.domain.model.TransactionType.TRANSFER },
                selectedIds = setOf(state.transactionType),
                onSelectionChange = { ids ->
                    viewModel.onEvent(FilterEntryEvent.SetTransactionType(ids.firstOrNull()))
                },
                idMapper = { it },
                labelMapper = { it?.displayName ?: "All (Income & Expense)" },
                mode = AeonSelectMode.SINGLE,
                iconMapper = {
                    when (it) {
                        null -> Icons.Rounded.FilterList
                        com.osscabinet.ossWallet.domain.model.TransactionType.EXPENSE -> Icons.Rounded.Remove
                        com.osscabinet.ossWallet.domain.model.TransactionType.INCOME -> Icons.Rounded.Add
                        com.osscabinet.ossWallet.domain.model.TransactionType.TRANSFER -> Icons.Rounded.SwapHoriz
                    }
                }
            )
        }

        if (state.showAccountSheet) {
            AeonAccountSelection(
                accounts = state.accounts,
                selectedAccountIds = state.selectedAccountIds,
                onSelectionChange = { ids -> viewModel.onEvent(FilterEntryEvent.SetAccountIds(ids)) },
                onDismiss = { viewModel.onEvent(FilterEntryEvent.ShowAccountSheet(false)) },
                mode = AeonSelectMode.MULTI,
                title = "Filter Accounts"
            )
        }

        if (state.showCategorySheet) {
            AeonCategorySelection(
                categories = state.categories,
                selectedCategoryIds = state.selectedCategoryIds,
                onSelectionChange = { ids -> viewModel.onEvent(FilterEntryEvent.SetCategoryIds(ids)) },
                onDismiss = { viewModel.onEvent(FilterEntryEvent.ShowCategorySheet(false)) },
                mode = AeonSelectMode.MULTI,
                title = "Filter Categories"
            )
        }

        if (state.showPaymentMethodSheet) {
            AeonPaymentMethodSelection(
                selectedMethods = state.selectedPaymentMethods,
                onSelectionChange = { viewModel.onEvent(FilterEntryEvent.SetPaymentMethods(it)) },
                onDismiss = { viewModel.onEvent(FilterEntryEvent.ShowPaymentMethodSheet(false)) },
                mode = AeonSelectMode.MULTI,
                title = "Filter Payment Methods"
            )
        }

        if (state.showStatusSheet) {
            AeonStatusSelection(
                selectedStatuses = state.selectedStatuses,
                onSelectionChange = { viewModel.onEvent(FilterEntryEvent.SetStatuses(it)) },
                onDismiss = { viewModel.onEvent(FilterEntryEvent.ShowStatusSheet(false)) },
                mode = AeonSelectMode.MULTI,
                title = "Filter Statuses"
            )
        }

        if (state.showLinkSheet) {
            AeonLinkedTransactionSelection(
                transactions = state.allTransactions,
                currentTransactionId = null,
                linkedIds = state.linkedTransactionIds,
                accounts = state.accounts,
                categories = state.categories,
                onLinkedTransactionsChanged = { viewModel.onEvent(FilterEntryEvent.SetLinkedTransactions(it)) },
                onDismiss = { viewModel.onEvent(FilterEntryEvent.ShowLinkSheet(false)) }
            )
        }

        if (state.showTagSheet) {
            AeonTagSelection(
                tags = state.availableTags,
                selectedTags = state.selectedTags,
                onTagsSelected = {
                    viewModel.onEvent(FilterEntryEvent.SetTags(it))
                    viewModel.onEvent(FilterEntryEvent.ShowTagSheet(false))
                },
                onAddTag = { name, _ -> 
                    viewModel.onEvent(FilterEntryEvent.SetTags(state.selectedTags + name))
                },
                onEditTag = {},
                onDeleteTag = {},
                onDismiss = { viewModel.onEvent(FilterEntryEvent.ShowTagSheet(false)) }
            )
        }

        if (state.showCurrencySheet) {
            val allCurrencies = listOf("USD", "EUR", "GBP", "JPY", "INR", "CAD", "AUD", "CHF", "CNY")
            AeonSelectionSheet(
                title = "Filter Currencies",
                onDismiss = { viewModel.onEvent(FilterEntryEvent.ShowCurrencySheet(false)) },
                items = allCurrencies,
                selectedIds = state.selectedCurrencies,
                onSelectionChange = { ids -> viewModel.onEvent(FilterEntryEvent.SetCurrencies(ids)) },
                idMapper = { it },
                labelMapper = { it },
                mode = AeonSelectMode.MULTI,
                iconMapper = { Icons.Rounded.AttachMoney }
            )
        }

        if (state.showMinAmountSheet) {
            AeonCalculatorSheet(
                currentAmount = state.minAmount,
                onAmountChanged = { viewModel.onEvent(FilterEntryEvent.SetMinAmount(it)) },
                onDismiss = { viewModel.onEvent(FilterEntryEvent.ShowMinAmountSheet(false)) },
                title = "Minimum Amount"
            )
        }

        if (state.showMaxAmountSheet) {
            AeonCalculatorSheet(
                currentAmount = state.maxAmount,
                onAmountChanged = { viewModel.onEvent(FilterEntryEvent.SetMaxAmount(it)) },
                onDismiss = { viewModel.onEvent(FilterEntryEvent.ShowMaxAmountSheet(false)) },
                title = "Maximum Amount"
            )
        }

        if (state.showWarrantyStartPicker) {
            AeonDatePickerDialog(
                selectedDateMillis = state.warrantyExpirationStart ?: System.currentTimeMillis(),
                onDateSelected = { 
                    viewModel.onEvent(FilterEntryEvent.SetWarrantyExpirationStart(it))
                    viewModel.onEvent(FilterEntryEvent.ShowWarrantyStartPicker(false))
                },
                onDismiss = { viewModel.onEvent(FilterEntryEvent.ShowWarrantyStartPicker(false)) },
                label = "Warranty Start"
            )
        }

        if (state.showWarrantyEndPicker) {
            AeonDatePickerDialog(
                selectedDateMillis = state.warrantyExpirationEnd ?: System.currentTimeMillis(),
                onDateSelected = { 
                    viewModel.onEvent(FilterEntryEvent.SetWarrantyExpirationEnd(it))
                    viewModel.onEvent(FilterEntryEvent.ShowWarrantyEndPicker(false))
                },
                onDismiss = { viewModel.onEvent(FilterEntryEvent.ShowWarrantyEndPicker(false)) },
                label = "Warranty End"
            )
        }
    }
}
