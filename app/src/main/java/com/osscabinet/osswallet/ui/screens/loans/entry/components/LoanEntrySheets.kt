package com.osscabinet.ossWallet.ui.screens.loans.entry.components

import androidx.compose.runtime.*
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.components.entry.*

@Composable
fun LoanEntrySheets(
    showStartDatePicker: Boolean,
    onDismissStartDatePicker: () -> Unit,
    startDateMillis: Long,
    onStartDateSelected: (Long) -> Unit,
    showSettlementDatePicker: Boolean,
    onDismissSettlementDatePicker: () -> Unit,
    settlementDateMillis: Long,
    onSettlementDateSelected: (Long) -> Unit,
    showNumpad: Boolean,
    onDismissNumpad: () -> Unit,
    principalText: String,
    onPrincipalChanged: (String) -> Unit,
    showInterestNumpad: Boolean,
    onDismissInterestNumpad: () -> Unit,
    interestText: String,
    onInterestChanged: (String) -> Unit,
    showAccountSheet: Boolean,
    onDismissAccountSheet: () -> Unit,
    accountQuery: String,
    onAccountQueryChange: (String) -> Unit,
    accounts: List<Account>,
    linkedAccountId: String,
    onAccountSelected: (String) -> Unit,
    showCategorySheet: Boolean,
    onDismissCategorySheet: () -> Unit,
    categoryQuery: String,
    onCategoryQueryChange: (String) -> Unit,
    categories: List<Category>,
    selectedCategoryIds: Set<Long>,
    onCategorySelectionChange: (Set<Long>) -> Unit,
    showTagSheet: Boolean,
    onDismissTagSheet: () -> Unit,
    availableTags: List<String>,
    selectedTags: Set<String>,
    onTagSelectionChange: (Set<String>) -> Unit,
    onAddTag: (String) -> Unit
) {
    if (showStartDatePicker) {
        AeonDatePickerDialog(
            selectedDateMillis = startDateMillis,
            onDateSelected = onStartDateSelected,
            onDismiss = onDismissStartDatePicker
        )
    }
    if (showSettlementDatePicker) {
        AeonDatePickerDialog(
            selectedDateMillis = settlementDateMillis,
            onDateSelected = onSettlementDateSelected,
            onDismiss = onDismissSettlementDatePicker
        )
    }
    if (showNumpad) {
        AeonCalculatorSheet(
            currentAmount = principalText,
            transactionType = TransactionType.INCOME,
            decimalPrecision = 2,
            onAmountChanged = onPrincipalChanged,
            onDismiss = onDismissNumpad
        )
    }
    if (showInterestNumpad) {
        AeonCalculatorSheet(
            currentAmount = interestText,
            transactionType = TransactionType.INCOME,
            decimalPrecision = 2,
            onAmountChanged = onInterestChanged,
            onDismiss = onDismissInterestNumpad
        )
    }

    if (showAccountSheet) {
        AeonAccountSelection(
            accounts = accounts,
            selectedAccountIds = if (linkedAccountId.isNotBlank()) setOf(linkedAccountId) else emptySet(),
            onSelectionChange = { ids ->
                ids.firstOrNull()?.let { onAccountSelected(it) }
            },
            onDismiss = onDismissAccountSheet,
            title = "Select Account",
            description = "Choose the source/destination account for this loan"
        )
    }

    if (showCategorySheet) {
        AeonCategorySelection(
            categories = categories,
            selectedCategoryIds = selectedCategoryIds,
            onSelectionChange = onCategorySelectionChange,
            onDismiss = onDismissCategorySheet,
            mode = AeonSelectMode.MULTI,
            title = "Categories"
        )
    }

    if (showTagSheet) {
        LoanTagSelection(
            availableTags = availableTags,
            selectedTags = selectedTags,
            onSelectionChanged = onTagSelectionChange,
            onAddTag = onAddTag,
            onDismiss = onDismissTagSheet
        )
    }
}
