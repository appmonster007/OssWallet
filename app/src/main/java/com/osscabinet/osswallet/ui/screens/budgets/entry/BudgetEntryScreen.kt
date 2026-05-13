package com.osscabinet.ossWallet.ui.screens.budgets.entry

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.ui.components.entry.AeonEntryScaffold
import com.osscabinet.ossWallet.ui.screens.budgets.entry.components.BudgetEntrySheets
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
            noteSuggestions = state.uniqueNotes,
            modifier = Modifier.padding(innerPadding)
        )

        BudgetEntrySheets(
            showCustomStartPicker = state.showCustomStartPicker,
            onDismissCustomStartPicker = { viewModel.onEvent(BudgetEntryEvent.ShowCustomStartPicker(false)) },
            customStartDateMillis = state.customStartDate,
            onCustomStartDateSelected = { viewModel.onEvent(BudgetEntryEvent.SetCustomStartDate(it)) },
            showCustomEndPicker = state.showCustomEndPicker,
            onDismissCustomEndPicker = { viewModel.onEvent(BudgetEntryEvent.ShowCustomEndPicker(false)) },
            customEndDateMillis = state.customEndDate,
            onCustomEndDateSelected = { viewModel.onEvent(BudgetEntryEvent.SetCustomEndDate(it)) },
            showNumpad = state.showNumpad,
            onDismissNumpad = { viewModel.onEvent(BudgetEntryEvent.ShowNumpad(false)) },
            limitText = state.limitAmount,
            onLimitChanged = { viewModel.onEvent(BudgetEntryEvent.SetLimitAmount(it)) },
            showAccountSheet = state.showAccountSheet,
            onDismissAccountSheet = { viewModel.onEvent(BudgetEntryEvent.ShowAccountSheet(false)) },
            accountQuery = state.accountQuery,
            onAccountQueryChange = { viewModel.onEvent(BudgetEntryEvent.SetAccountQuery(it)) },
            accounts = state.accounts,
            selectedAccountIds = state.selectedAccountIds,
            onAccountSelectionChange = { viewModel.onEvent(BudgetEntryEvent.SetAccountIds(it)) },
            showPeriodSheet = state.showPeriodSheet,
            onDismissPeriodSheet = { viewModel.onEvent(BudgetEntryEvent.ShowPeriodSheet(false)) },
            period = state.period,
            onPeriodSelected = { 
                viewModel.onEvent(BudgetEntryEvent.SetPeriod(it))
                if (it == BudgetPeriod.CUSTOM) {
                    viewModel.onEvent(BudgetEntryEvent.ShowCustomStartPicker(true))
                }
                viewModel.onEvent(BudgetEntryEvent.ShowPeriodSheet(false))
            },
            showTrackTypeSheet = state.showTrackTypeSheet,
            onDismissTrackTypeSheet = { viewModel.onEvent(BudgetEntryEvent.ShowTrackTypeSheet(false)) },
            selectedTrackTypes = state.selectedTrackTypes,
            onTrackTypeSelectionChange = { viewModel.onEvent(BudgetEntryEvent.SetTrackTypes(it)) },
            showCategorySheet = state.showCategorySheet,
            onDismissCategorySheet = { viewModel.onEvent(BudgetEntryEvent.ShowCategorySheet(false)) },
            categoryQuery = state.categoryQuery,
            onCategoryQueryChange = { viewModel.onEvent(BudgetEntryEvent.SetCategoryQuery(it)) },
            categories = state.categories,
            selectedCategoryIds = state.selectedCategoryIds,
            onCategorySelectionChange = { viewModel.onEvent(BudgetEntryEvent.SetCategoryIds(it)) },
            showTagSheet = state.showTagSheet,
            onDismissTagSheet = { viewModel.onEvent(BudgetEntryEvent.ShowTagSheet(false)) },
            availableTags = state.availableTags.map { it.name },
            selectedTags = state.selectedTags,
            onTagSelectionChange = { viewModel.onEvent(BudgetEntryEvent.SetTags(it)) },
            onAddTag = { viewModel.onEvent(BudgetEntryEvent.SetTags(state.selectedTags + it)) }
        )
    }
}
