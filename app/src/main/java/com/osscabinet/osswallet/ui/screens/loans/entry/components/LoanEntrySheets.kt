package com.osscabinet.ossWallet.ui.screens.loans.entry.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.screens.transactions.entry.sheets.CalculatorSheet

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
        CalculatorSheet(
            currentAmount = principalText,
            transactionType = TransactionType.INCOME,
            decimalPrecision = 2,
            onAmountChanged = onPrincipalChanged,
            onDismiss = onDismissNumpad
        )
    }
    if (showInterestNumpad) {
        CalculatorSheet(
            currentAmount = interestText,
            transactionType = TransactionType.INCOME,
            decimalPrecision = 2,
            onAmountChanged = onInterestChanged,
            onDismiss = onDismissInterestNumpad
        )
    }

    if (showAccountSheet) {
        val filteredAccounts = remember(accountQuery, accounts) {
            if (accountQuery.isBlank()) accounts
            else accounts.filter { it.name.contains(accountQuery, ignoreCase = true) }
        }

        AeonFullScreenDialog(onDismissRequest = onDismissAccountSheet) {
            AeonSelectionScreen(
                title = "Select Account",
                onBack = onDismissAccountSheet
            ) { padding ->
                Column(modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 24.dp)) {
                    Text(
                        text = "Choose the source/destination account for this loan",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    AeonSearchField(
                        query = accountQuery,
                        onQueryChange = onAccountQueryChange,
                        placeholder = "Search accounts..."
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    Box(modifier = Modifier.weight(1f, fill = false)) {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            AeonHybridSelector(
                                items = filteredAccounts,
                                mode = AeonSelectMode.SINGLE,
                                selectedIds = setOf(linkedAccountId),
                                onSelectionChange = { ids ->
                                    ids.firstOrNull()?.let { onAccountSelected(it) }
                                    onDismissAccountSheet()
                                },
                                idMapper = { it.id },
                                labelMapper = { it.name },
                                detailMapper = { it.currencyCode },
                                iconMapper = { getAccountIconByName(it.icon) },
                                colorMapper = { Color(android.graphics.Color.parseColor(it.colorHex)) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCategorySheet) {
        val filteredCategories = remember(categoryQuery, categories) {
            if (categoryQuery.isBlank()) categories
            else categories.filter { it.name.contains(categoryQuery, ignoreCase = true) }
        }

        AeonFullScreenDialog(onDismissRequest = onDismissCategorySheet) {
            AeonSelectionScreen(
                title = "Categories",
                onBack = onDismissCategorySheet
            ) { padding ->
                Column(modifier = Modifier
                    .padding(padding)
                    .padding(24.dp)) {

                    AeonSearchField(
                        query = categoryQuery,
                        onQueryChange = onCategoryQueryChange,
                        placeholder = "Search categories..."
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(modifier = Modifier.weight(1f, fill = false)) {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            AeonHierarchicalHybridSelector(
                                items = filteredCategories,
                                mode = AeonSelectMode.MULTI,
                                selectedIds = selectedCategoryIds,
                                onSelectionChange = onCategorySelectionChange,
                                idMapper = { it.id },
                                labelMapper = { it.name },
                                parentIdMapper = { it.parentId },
                                iconMapper = { getIconByName(it.iconName) },
                                colorMapper = {
                                    if (it.colorHex == -1) Color(0xFF9E9E9E) else Color(
                                        it.colorHex
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
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
