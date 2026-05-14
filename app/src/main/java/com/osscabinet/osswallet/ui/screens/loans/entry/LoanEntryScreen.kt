package com.osscabinet.ossWallet.ui.screens.loans.entry

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.ui.components.entry.*
import com.osscabinet.ossWallet.ui.components.AeonSelectMode
import com.osscabinet.ossWallet.ui.screens.loans.entry.components.LoanFormBody
import com.osscabinet.ossWallet.domain.model.Tag

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
            onPaymentMethodClick = { viewModel.onEvent(LoanEntryEvent.ShowPaymentMethodSheet(true)) },
            onStatusClick = { viewModel.onEvent(LoanEntryEvent.ShowStatusSheet(true)) },
            onLinkRecordClick = { viewModel.onEvent(LoanEntryEvent.ShowLinkSheet(true)) },
            counterpartySuggestions = state.uniquePayees,
            noteSuggestions = state.uniqueNotes,
            modifier = Modifier.padding(innerPadding)
        )

        // Standardized Selection Sheets
        if (state.showPaymentMethodSheet) {
            AeonPaymentMethodSelection(
                selectedMethods = state.selectedPaymentMethods,
                onSelectionChange = { viewModel.onEvent(LoanEntryEvent.SetPaymentMethods(it)) },
                onDismiss = { viewModel.onEvent(LoanEntryEvent.ShowPaymentMethodSheet(false)) },
                mode = AeonSelectMode.MULTI,
                title = "Filter Payment Methods"
            )
        }

        if (state.showStatusSheet) {
            AeonStatusSelection(
                selectedStatuses = state.selectedStatuses,
                onSelectionChange = { viewModel.onEvent(LoanEntryEvent.SetStatuses(it)) },
                onDismiss = { viewModel.onEvent(LoanEntryEvent.ShowStatusSheet(false)) },
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
                onLinkedTransactionsChanged = { viewModel.onEvent(LoanEntryEvent.SetLinkedTransactions(it)) },
                onDismiss = { viewModel.onEvent(LoanEntryEvent.ShowLinkSheet(false)) }
            )
        }
        if (state.showStartDatePicker) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = state.startDate)
            DatePickerDialog(
                onDismissRequest = { viewModel.onEvent(LoanEntryEvent.ShowStartDatePicker(false)) },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { viewModel.onEvent(LoanEntryEvent.SetStartDate(it)) }
                        viewModel.onEvent(LoanEntryEvent.ShowStartDatePicker(false))
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onEvent(LoanEntryEvent.ShowStartDatePicker(false)) }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        if (state.showSettlementDatePicker) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = state.settlementDate)
            DatePickerDialog(
                onDismissRequest = { viewModel.onEvent(LoanEntryEvent.ShowSettlementDatePicker(false)) },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { viewModel.onEvent(LoanEntryEvent.SetSettlementDate(it)) }
                        viewModel.onEvent(LoanEntryEvent.ShowSettlementDatePicker(false))
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onEvent(LoanEntryEvent.ShowSettlementDatePicker(false)) }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        if (state.showNumpad) {
            AeonCalculatorSheet(
                currentAmount = state.principalAmount,
                onAmountChanged = { viewModel.onEvent(LoanEntryEvent.SetPrincipalAmount(it)) },
                onDismiss = { viewModel.onEvent(LoanEntryEvent.ShowNumpad(false)) },
                title = "Loan Principal"
            )
        }

        if (state.showInterestNumpad) {
            AeonCalculatorSheet(
                currentAmount = state.interestRate,
                onAmountChanged = { viewModel.onEvent(LoanEntryEvent.SetInterestRate(it)) },
                onDismiss = { viewModel.onEvent(LoanEntryEvent.ShowInterestNumpad(false)) },
                title = "Interest Rate (%)"
            )
        }

        if (state.showAccountSheet) {
            AeonAccountSelection(
                accounts = state.accounts,
                selectedAccountIds = if (state.linkedAccountId.isEmpty()) emptySet() else setOf(state.linkedAccountId),
                onSelectionChange = { ids ->
                    ids.firstOrNull()?.let { viewModel.onEvent(LoanEntryEvent.SetLinkedAccountId(it)) }
                    viewModel.onEvent(LoanEntryEvent.ShowAccountSheet(false))
                },
                onDismiss = { viewModel.onEvent(LoanEntryEvent.ShowAccountSheet(false)) },
                title = "Linked Account"
            )
        }

        if (state.showCategorySheet) {
            AeonCategorySelection(
                categories = state.categories,
                selectedCategoryIds = state.selectedCategoryIds,
                onSelectionChange = { ids ->
                    viewModel.onEvent(LoanEntryEvent.SetCategoryIds(ids))
                },
                onDismiss = { viewModel.onEvent(LoanEntryEvent.ShowCategorySheet(false)) },
                mode = AeonSelectMode.MULTI
            )
        }

        if (state.showTagSheet) {
            AeonTagSelection(
                tags = state.availableTags.map { Tag(0, it, 0) }, // Mock Tag objects
                selectedTags = state.selectedTags.toList(),
                onTagsSelected = {
                    viewModel.onEvent(LoanEntryEvent.SetTags(it.toSet()))
                    viewModel.onEvent(LoanEntryEvent.ShowTagSheet(false))
                },
                onAddTag = { name, _ -> 
                    viewModel.onEvent(LoanEntryEvent.SetTags(state.selectedTags + name))
                },
                onEditTag = {},
                onDeleteTag = {},
                onDismiss = { viewModel.onEvent(LoanEntryEvent.ShowTagSheet(false)) }
            )
        }
    }
}
