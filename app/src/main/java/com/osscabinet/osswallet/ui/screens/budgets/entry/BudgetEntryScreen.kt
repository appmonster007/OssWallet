package com.osscabinet.ossWallet.ui.screens.budgets.entry

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.ui.components.AeonConfirmDeleteDialog
import com.osscabinet.ossWallet.ui.components.AeonDatePickerDialog
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
            { viewModel.onEvent(BudgetEntryEvent.ShowDeleteDialog(true)) }
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
            selectedCurrencies = state.selectedCurrencies,
            onCurrencyFilterClick = { viewModel.onEvent(BudgetEntryEvent.ShowCurrencyFilterSheet(true)) },
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
            selectedPaymentMethods = state.selectedPaymentMethods,
            onPaymentMethodClick = { viewModel.onEvent(BudgetEntryEvent.ShowPaymentMethodSheet(true)) },
            selectedStatuses = state.selectedStatuses,
            onStatusClick = { viewModel.onEvent(BudgetEntryEvent.ShowStatusSheet(true)) },
            linkedTransactionIds = state.linkedTransactionIds,
            onLinkRecordClick = { viewModel.onEvent(BudgetEntryEvent.ShowLinkSheet(true)) },
            filterId = state.filterId,
            onFilterClick = { viewModel.onEvent(BudgetEntryEvent.ShowFilterSheet(true)) },
            availableFilters = state.availableFilters,
            noteSuggestions = state.uniqueNotes,
            modifier = Modifier.padding(innerPadding),
            isFilterShared = state.filterUsageCount > 1,
            filterUsageCount = state.filterUsageCount,
            onMakeFilterUnique = { viewModel.onEvent(BudgetEntryEvent.MakeFilterUnique) }
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
            AeonDatePickerDialog(
                selectedDateMillis = state.customStartDate,
                onDateSelected = { 
                    viewModel.onEvent(BudgetEntryEvent.SetCustomStartDate(it))
                    viewModel.onEvent(BudgetEntryEvent.ShowCustomStartPicker(false))
                    viewModel.onEvent(BudgetEntryEvent.ShowCustomEndPicker(true))
                },
                onDismiss = { viewModel.onEvent(BudgetEntryEvent.ShowCustomStartPicker(false)) },
                label = "Custom Start Date"
            )
        }

        if (state.showCustomEndPicker) {
            AeonDatePickerDialog(
                selectedDateMillis = state.customEndDate,
                onDateSelected = { 
                    viewModel.onEvent(BudgetEntryEvent.SetCustomEndDate(it))
                    viewModel.onEvent(BudgetEntryEvent.ShowCustomEndPicker(false))
                },
                onDismiss = { viewModel.onEvent(BudgetEntryEvent.ShowCustomEndPicker(false)) },
                label = "Custom End Date"
            )
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
                items = TransactionType.entries.filter { it != TransactionType.TRANSFER },
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
                title = "Budget Categories",
                startExpanded = false
            )
        }

        if (state.showTagSheet) {
            AeonTagSelection(
                tags = state.availableTags,
                selectedTags = state.selectedTags,
                onTagsSelected = {
                    viewModel.onEvent(BudgetEntryEvent.SetTags(it))
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

        if (state.showCurrencyFilterSheet) {
            AeonCurrencySelection(
                selectedCurrencies = state.selectedCurrencies,
                onSelectionChange = { viewModel.onEvent(BudgetEntryEvent.SetCurrencies(it)) },
                onDismiss = { viewModel.onEvent(BudgetEntryEvent.ShowCurrencyFilterSheet(false)) },
                mode = AeonSelectMode.MULTI,
                title = "Filter Currencies"
            )
        }

        if (state.showFilterSheet) {
            AeonFilterSelection(
                filters = state.availableFilters,
                selectedFilterId = state.filterId,
                onFilterSelected = { viewModel.onEvent(BudgetEntryEvent.SetFilterId(it)) },
                onDismiss = { viewModel.onEvent(BudgetEntryEvent.ShowFilterSheet(false)) },
                title = "Select Filter",
                showCreateNew = true
            )
        }

        AeonConfirmDeleteDialog(
            show = state.showDeleteDialog,
            title = "Delete Budget",
            message = "Are you sure you want to delete this budget?",
            onConfirm = {
                viewModel.deleteBudget()
            },
            onDismiss = { viewModel.onEvent(BudgetEntryEvent.ShowDeleteDialog(false)) },
            extraContent = {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = state.deleteFilterWithBudget,
                            onCheckedChange = { viewModel.onEvent(BudgetEntryEvent.SetDeleteFilterWithBudget(it)) }
                        )
                        Text("Also delete linked filter", style = MaterialTheme.typography.bodyMedium)
                    }
                    
                    if (state.deleteFilterWithBudget && state.filterUsageCount > 1) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "Warning: This filter is shared with ${state.filterUsageCount - 1} other record(s). Deleting it will also affect them.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}
