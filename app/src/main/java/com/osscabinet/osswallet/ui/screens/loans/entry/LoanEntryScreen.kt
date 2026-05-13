package com.osscabinet.ossWallet.ui.screens.loans.entry

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.ui.components.entry.AeonEntryScaffold
import com.osscabinet.ossWallet.ui.screens.loans.entry.components.LoanEntrySheets
import com.osscabinet.ossWallet.ui.screens.loans.entry.components.LoanFormBody

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanEntryScreen(
    loanId: Long? = null,
    viewModel: LoanEntryViewModel,
    onBackClick: () -> Unit
) {
    LaunchedEffect(loanId) {
        viewModel.initialize(loanId)
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
        title = if (state.isEditMode) "Edit Loan" else "New Loan",
        onBackClick = onBackClick,
        onSaveClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.saveLoan {
                viewModel.onEvent(LoanEntryEvent.TriggerNavigation)
            }
        },
        canSave = state.canSave && !state.navTriggered,
        isEditMode = state.isEditMode,
        onDeleteClick = if (state.isEditMode) {
            {
                viewModel.deleteLoan {
                    onBackClick()
                }
            }
        } else null
    ) { innerPadding ->
        LoanFormBody(
            loanType = state.loanType,
            onTypeSelected = { viewModel.onEvent(LoanEntryEvent.SetLoanType(it)) },
            name = state.name,
            onNameChange = { viewModel.onEvent(LoanEntryEvent.SetName(it)) },
            amount = state.principalAmount,
            currency = state.currency,
            onCurrencyChange = { viewModel.onEvent(LoanEntryEvent.SetCurrency(it)) },
            supportedCurrencies = allCurrencies,
            onAmountClick = { viewModel.onEvent(LoanEntryEvent.ShowNumpad(true)) },
            startDateMillis = state.startDate,
            onStartDateClick = { viewModel.onEvent(LoanEntryEvent.ShowStartDatePicker(true)) },
            interestRate = state.interestRate,
            onInterestClick = { viewModel.onEvent(LoanEntryEvent.ShowInterestNumpad(true)) },
            linkedAccountId = state.linkedAccountId,
            accounts = state.accounts,
            onAccountClick = { viewModel.onEvent(LoanEntryEvent.ShowAccountSheet(true)) },
            hasSettlementDate = state.hasSettlementDate,
            settlementDateMillis = state.settlementDate,
            onSettlementClick = { viewModel.onEvent(LoanEntryEvent.ShowSettlementDatePicker(true)) },
            selectedCategoryIds = state.selectedCategoryIds,
            categories = state.categories,
            onCategoryClick = { viewModel.onEvent(LoanEntryEvent.ShowCategorySheet(true)) },
            counterparty = state.counterparty,
            onCounterpartyChange = { viewModel.onEvent(LoanEntryEvent.SetCounterparty(it)) },
            note = state.note,
            onNoteChange = { viewModel.onEvent(LoanEntryEvent.SetNote(it)) },
            onHasSettlementDateChange = { viewModel.onEvent(LoanEntryEvent.SetHasSettlementDate(it)) },
            selectedTags = state.selectedTags,
            onShowTagSheet = { viewModel.onEvent(LoanEntryEvent.ShowTagSheet(true)) },
            onRemoveTag = { tag -> viewModel.onEvent(LoanEntryEvent.SetTags(state.selectedTags - tag)) },
            counterpartySuggestions = state.uniquePayees,
            noteSuggestions = state.uniqueNotes,
            modifier = Modifier.padding(innerPadding)
        )

        LoanEntrySheets(
            showStartDatePicker = state.showStartDatePicker,
            onDismissStartDatePicker = { viewModel.onEvent(LoanEntryEvent.ShowStartDatePicker(false)) },
            startDateMillis = state.startDate,
            onStartDateSelected = { viewModel.onEvent(LoanEntryEvent.SetStartDate(it)) },
            showSettlementDatePicker = state.showSettlementDatePicker,
            onDismissSettlementDatePicker = { viewModel.onEvent(LoanEntryEvent.ShowSettlementDatePicker(false)) },
            settlementDateMillis = state.settlementDate,
            onSettlementDateSelected = { viewModel.onEvent(LoanEntryEvent.SetSettlementDate(it)) },
            showNumpad = state.showNumpad,
            onDismissNumpad = { viewModel.onEvent(LoanEntryEvent.ShowNumpad(false)) },
            principalText = state.principalAmount,
            onPrincipalChanged = { viewModel.onEvent(LoanEntryEvent.SetPrincipalAmount(it)) },
            showInterestNumpad = state.showInterestNumpad,
            onDismissInterestNumpad = { viewModel.onEvent(LoanEntryEvent.ShowInterestNumpad(false)) },
            interestText = state.interestRate,
            onInterestChanged = { viewModel.onEvent(LoanEntryEvent.SetInterestRate(it)) },
            showAccountSheet = state.showAccountSheet,
            onDismissAccountSheet = { viewModel.onEvent(LoanEntryEvent.ShowAccountSheet(false)) },
            accountQuery = state.accountQuery,
            onAccountQueryChange = { viewModel.onEvent(LoanEntryEvent.SetAccountQuery(it)) },
            accounts = state.accounts,
            linkedAccountId = state.linkedAccountId,
            onAccountSelected = { viewModel.onEvent(LoanEntryEvent.SetLinkedAccountId(it)) },
            showCategorySheet = state.showCategorySheet,
            onDismissCategorySheet = { viewModel.onEvent(LoanEntryEvent.ShowCategorySheet(false)) },
            categoryQuery = state.categoryQuery,
            onCategoryQueryChange = { viewModel.onEvent(LoanEntryEvent.SetCategoryQuery(it)) },
            categories = state.categories,
            selectedCategoryIds = state.selectedCategoryIds,
            onCategorySelectionChange = { viewModel.onEvent(LoanEntryEvent.SetCategoryIds(it)) },
            showTagSheet = state.showTagSheet,
            onDismissTagSheet = { viewModel.onEvent(LoanEntryEvent.ShowTagSheet(false)) },
            availableTags = state.availableTags,
            selectedTags = state.selectedTags,
            onTagSelectionChange = { viewModel.onEvent(LoanEntryEvent.SetTags(it)) },
            onAddTag = { viewModel.onEvent(LoanEntryEvent.SetTags(state.selectedTags + it)) }
        )
    }
}
