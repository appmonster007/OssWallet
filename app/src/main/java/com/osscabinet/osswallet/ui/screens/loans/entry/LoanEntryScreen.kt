package com.osscabinet.ossWallet.ui.screens.loans.entry

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
            { viewModel.onEvent(LoanEntryEvent.ShowDeleteDialog(true)) }
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
            selectedPaymentMethods = state.selectedPaymentMethods,
            onPaymentMethodClick = { viewModel.onEvent(LoanEntryEvent.ShowPaymentMethodSheet(true)) },
            selectedStatuses = state.selectedStatuses,
            onStatusClick = { viewModel.onEvent(LoanEntryEvent.ShowStatusSheet(true)) },
            linkedTransactionIds = state.linkedTransactionIds,
            onLinkRecordClick = { viewModel.onEvent(LoanEntryEvent.ShowLinkSheet(true)) },
            filterId = state.filterId,
            onFilterClick = { viewModel.onEvent(LoanEntryEvent.ShowFilterSheet(true)) },
            availableFilters = state.availableFilters,
            counterpartySuggestions = state.uniquePayees,
            noteSuggestions = state.uniqueNotes,
            modifier = Modifier.padding(innerPadding),
            isFilterShared = state.filterUsageCount > 1,
            filterUsageCount = state.filterUsageCount,
            onMakeFilterUnique = { viewModel.onEvent(LoanEntryEvent.MakeFilterUnique) }
        )

        // Selection Sheets
        if (state.showStartDatePicker) {
            AeonDatePickerDialog(
                selectedDateMillis = state.startDate,
                onDateSelected = { 
                    viewModel.onEvent(LoanEntryEvent.SetStartDate(it))
                    viewModel.onEvent(LoanEntryEvent.ShowStartDatePicker(false))
                },
                onDismiss = { viewModel.onEvent(LoanEntryEvent.ShowStartDatePicker(false)) }
            )
        }

        if (state.showSettlementDatePicker) {
            AeonDatePickerDialog(
                selectedDateMillis = state.settlementDate,
                onDateSelected = { 
                    viewModel.onEvent(LoanEntryEvent.SetSettlementDate(it))
                    viewModel.onEvent(LoanEntryEvent.ShowSettlementDatePicker(false))
                },
                onDismiss = { viewModel.onEvent(LoanEntryEvent.ShowSettlementDatePicker(false)) }
            )
        }

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

        if (state.showNumpad) {
            AeonCalculatorSheet(
                currentAmount = state.principalAmount,
                onAmountChanged = { viewModel.onEvent(LoanEntryEvent.SetPrincipalAmount(it)) },
                onDismiss = { viewModel.onEvent(LoanEntryEvent.ShowNumpad(false)) },
                title = "Principal Amount"
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
                },
                onDismiss = { viewModel.onEvent(LoanEntryEvent.ShowAccountSheet(false)) },
                mode = AeonSelectMode.SINGLE,
                title = "Loan Account"
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
                mode = AeonSelectMode.MULTI,
                title = "Loan Categories",
                startExpanded = false
            )
        }

        if (state.showTagSheet) {
            AeonTagSelection(
                tags = state.availableTags.map { Tag(name = it, colorHex = -1) },
                selectedTags = state.selectedTags,
                onTagsSelected = {
                    viewModel.onEvent(LoanEntryEvent.SetTags(it))
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

        if (state.showFilterSheet) {
            AeonFilterSelection(
                filters = state.availableFilters,
                selectedFilterId = state.filterId,
                onFilterSelected = { viewModel.onEvent(LoanEntryEvent.SetFilterId(it)) },
                onDismiss = { viewModel.onEvent(LoanEntryEvent.ShowFilterSheet(false)) },
                title = "Select Filter",
                showCreateNew = true
            )
        }

        AeonConfirmDeleteDialog(
            show = state.showDeleteDialog,
            title = "Delete Loan",
            message = "Are you sure you want to delete this loan?",
            onConfirm = {
                viewModel.deleteLoan()
            },
            onDismiss = { viewModel.onEvent(LoanEntryEvent.ShowDeleteDialog(false)) },
            extraContent = {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = state.deleteFilterWithLoan,
                            onCheckedChange = { viewModel.onEvent(LoanEntryEvent.SetDeleteFilterWithLoan(it)) }
                        )
                        Text("Also delete linked filter", style = MaterialTheme.typography.bodyMedium)
                    }
                    
                    if (state.deleteFilterWithLoan && state.filterUsageCount > 1) {
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
