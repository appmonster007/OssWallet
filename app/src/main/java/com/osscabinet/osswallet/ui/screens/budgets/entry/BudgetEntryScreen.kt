package com.osscabinet.ossWallet.ui.screens.budgets.entry

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.ui.components.AeonSelectMode
import com.osscabinet.ossWallet.ui.components.entry.*
import com.osscabinet.ossWallet.ui.screens.budgets.entry.components.BudgetFormBody

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetEntryScreen(
    budgetId: Long? = null,
    viewModel: BudgetEntryViewModel,
    onBackClick: () -> Unit
) {
    LaunchedEffect(budgetId) {
        viewModel.initialize(budgetId)
    }

    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(state.navTriggered) {
        if (state.navTriggered) {
            onBackClick()
        }
    }

    val allCurrencies = remember { CurrencyService.getAllCurrencies() }

    AeonEntryScaffold(
        title = if (state.isEditMode) "Edit Budget" else "New Budget",
        onBackClick = onBackClick,
        onSaveClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.saveBudget {
                viewModel.onEvent(BudgetEntryEvent.TriggerNavigation)
            }
        },
        canSave = state.canSave && !state.navTriggered,
        isEditMode = state.isEditMode,
        onDeleteClick = if (state.isEditMode) {
            {
                viewModel.deleteBudget {
                    onBackClick()
                }
            }
        } else null
    ) { innerPadding ->
        BudgetFormBody(
            name = state.name,
            onNameChange = { viewModel.onEvent(BudgetEntryEvent.SetName(it)) },
            amount = state.limitAmount,
            currency = state.currency,
            onCurrencyChange = { viewModel.onEvent(BudgetEntryEvent.SetCurrency(it)) },
            supportedCurrencies = allCurrencies,
            onAmountClick = { viewModel.onEvent(BudgetEntryEvent.ShowNumpad(true)) },
            period = state.period,
            onPeriodClick = { viewModel.onEvent(BudgetEntryEvent.ShowPeriodSheet(true)) },
            selectedAccountIds = state.selectedAccountIds,
            accounts = state.accounts,
            onAccountClick = { viewModel.onEvent(BudgetEntryEvent.ShowAccountSheet(true)) },
            selectedTrackTypes = state.selectedTrackTypes,
            onTrackTypeClick = { viewModel.onEvent(BudgetEntryEvent.ShowTrackTypeSheet(true)) },
            selectedCategoryIds = state.selectedCategoryIds,
            categories = state.categories,
            onCategoryClick = { viewModel.onEvent(BudgetEntryEvent.ShowCategorySheet(true)) },
            note = state.note,
            onNoteChange = { viewModel.onEvent(BudgetEntryEvent.SetNote(it)) },
            alertThreshold = state.alertThreshold,
            onAlertThresholdChange = { viewModel.onEvent(BudgetEntryEvent.SetAlertThreshold(it)) },
            rollover = state.rollover,
            onRolloverChange = { viewModel.onEvent(BudgetEntryEvent.SetRollover(it)) },
            selectedTags = state.selectedTags,
            onShowTagSheet = { viewModel.onEvent(BudgetEntryEvent.ShowTagSheet(true)) },
            onRemoveTag = { tag -> viewModel.onEvent(BudgetEntryEvent.SetTags(state.selectedTags - tag)) },
            onPaymentMethodClick = { viewModel.onEvent(BudgetEntryEvent.ShowPaymentMethodSheet(true)) },
            onStatusClick = { viewModel.onEvent(BudgetEntryEvent.ShowStatusSheet(true)) },
            onLinkRecordClick = { viewModel.onEvent(BudgetEntryEvent.ShowLinkSheet(true)) },
            noteSuggestions = state.uniqueNotes,
            modifier = Modifier.padding(innerPadding)
        )

        // Selection Sheets
        if (state.showPaymentMethodSheet) {
            AeonPaymentMethodSelection(
                selectedMethods = state.selectedPaymentMethods,
                onSelectionChange = { viewModel.onEvent(BudgetEntryEvent.SetPaymentMethods(it)) },
                onDismiss = { viewModel.onEvent(BudgetEntryEvent.ShowPaymentMethodSheet(false)) },
                mode = AeonSelectMode.MULTI,
                title = "Filter Payment Methods"
            )
        }

        if (state.showStatusSheet) {
            AeonStatusSelection(
                selectedStatuses = state.selectedStatuses,
                onSelectionChange = { viewModel.onEvent(BudgetEntryEvent.SetStatuses(it)) },
                onDismiss = { viewModel.onEvent(BudgetEntryEvent.ShowStatusSheet(false)) },
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
                onLinkedTransactionsChanged = { viewModel.onEvent(BudgetEntryEvent.SetLinkedTransactions(it)) },
                onDismiss = { viewModel.onEvent(BudgetEntryEvent.ShowLinkSheet(false)) }
            )
        }
        if (state.showNumpad) {
            AeonCalculatorSheet(
                currentAmount = state.limitAmount,
                onAmountChanged = { viewModel.onEvent(BudgetEntryEvent.SetLimitAmount(it)) },
                onDismiss = { viewModel.onEvent(BudgetEntryEvent.ShowNumpad(false)) },
                title = "Budget Limit"
            )
        }

        if (state.showPeriodSheet) {
            AeonSelectionSheet(
                title = "Budget Period",
                onDismiss = { viewModel.onEvent(BudgetEntryEvent.ShowPeriodSheet(false)) },
                items = BudgetPeriod.entries,
                selectedIds = setOf(state.period),
                onSelectionChange = { ids ->
                    ids.firstOrNull()?.let { 
                        viewModel.onEvent(BudgetEntryEvent.SetPeriod(it))
                        if (it == BudgetPeriod.CUSTOM) {
                            viewModel.onEvent(BudgetEntryEvent.ShowCustomStartPicker(true))
                        }
                    }
                },
                idMapper = { it },
                labelMapper = { it.name.lowercase().replaceFirstChar { it.uppercase() } },
                mode = AeonSelectMode.SINGLE,
                iconMapper = {
                    when (it) {
                        BudgetPeriod.DAILY -> Icons.Rounded.Today
                        BudgetPeriod.WEEKLY -> Icons.Rounded.ViewWeek
                        BudgetPeriod.MONTHLY -> Icons.Rounded.CalendarMonth
                        BudgetPeriod.QUARTERLY -> Icons.Rounded.PieChart
                        BudgetPeriod.YEARLY -> Icons.Rounded.CalendarToday
                        BudgetPeriod.CUSTOM -> Icons.Rounded.EditCalendar
                    }
                }
            )
        }

        if (state.showCustomStartPicker) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = state.customStartDate)
            DatePickerDialog(
                onDismissRequest = { viewModel.onEvent(BudgetEntryEvent.ShowCustomStartPicker(false)) },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { viewModel.onEvent(BudgetEntryEvent.SetCustomStartDate(it)) }
                        viewModel.onEvent(BudgetEntryEvent.ShowCustomStartPicker(false))
                        viewModel.onEvent(BudgetEntryEvent.ShowCustomEndPicker(true))
                    }) { Text("Next") }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onEvent(BudgetEntryEvent.ShowCustomStartPicker(false)) }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        if (state.showCustomEndPicker) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = state.customEndDate)
            DatePickerDialog(
                onDismissRequest = { viewModel.onEvent(BudgetEntryEvent.ShowCustomEndPicker(false)) },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { viewModel.onEvent(BudgetEntryEvent.SetCustomEndDate(it)) }
                        viewModel.onEvent(BudgetEntryEvent.ShowCustomEndPicker(false))
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onEvent(BudgetEntryEvent.ShowCustomEndPicker(false)) }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        if (state.showAccountSheet) {
            AeonAccountSelection(
                accounts = state.accounts,
                selectedAccountIds = state.selectedAccountIds,
                onSelectionChange = { ids ->
                    viewModel.onEvent(BudgetEntryEvent.SetAccountIds(ids))
                },
                onDismiss = { viewModel.onEvent(BudgetEntryEvent.ShowAccountSheet(false)) },
                mode = AeonSelectMode.MULTI,
                title = "Budget Accounts"
            )
        }

        if (state.showTrackTypeSheet) {
            AeonSelectionSheet(
                title = "Track Transaction Types",
                onDismiss = { viewModel.onEvent(BudgetEntryEvent.ShowTrackTypeSheet(false)) },
                items = TransactionType.entries,
                selectedIds = state.selectedTrackTypes,
                onSelectionChange = { ids ->
                    viewModel.onEvent(BudgetEntryEvent.SetTrackTypes(ids))
                },
                idMapper = { it },
                labelMapper = { it.displayName },
                mode = AeonSelectMode.MULTI,
                iconMapper = {
                    when (it) {
                        TransactionType.EXPENSE -> Icons.Rounded.Remove
                        TransactionType.INCOME -> Icons.Rounded.Add
                        TransactionType.TRANSFER -> Icons.Rounded.SwapHoriz
                    }
                }
            )
        }

        if (state.showCategorySheet) {
            AeonCategorySelection(
                categories = state.categories,
                selectedCategoryIds = state.selectedCategoryIds,
                onSelectionChange = { ids ->
                    viewModel.onEvent(BudgetEntryEvent.SetCategoryIds(ids))
                },
                onDismiss = { viewModel.onEvent(BudgetEntryEvent.ShowCategorySheet(false)) },
                mode = AeonSelectMode.MULTI,
                title = "Budget Categories"
            )
        }

        if (state.showTagSheet) {
            AeonTagSelection(
                tags = state.availableTags,
                selectedTags = state.selectedTags.toList(),
                onTagsSelected = {
                    viewModel.onEvent(BudgetEntryEvent.SetTags(it.toSet()))
                    viewModel.onEvent(BudgetEntryEvent.ShowTagSheet(false))
                },
                onAddTag = { name, _ -> 
                    viewModel.onEvent(BudgetEntryEvent.SetTags(state.selectedTags + name))
                },
                onEditTag = {},
                onDeleteTag = {},
                onDismiss = { viewModel.onEvent(BudgetEntryEvent.ShowTagSheet(false)) }
            )
        }
    }
}
