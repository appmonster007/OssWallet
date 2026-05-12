package com.osscabinet.ossWallet.ui.screens.loans.entry.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.LoanType

@Composable
fun LoanFormBody(
    loanType: LoanType,
    onTypeSelected: (LoanType) -> Unit,
    name: String,
    onNameChange: (String) -> Unit,
    amount: String,
    currency: String,
    onCurrencyChange: (String) -> Unit,
    supportedCurrencies: List<String>,
    onAmountClick: () -> Unit,
    startDateMillis: Long,
    onStartDateClick: () -> Unit,
    interestRate: String,
    onInterestClick: () -> Unit,
    linkedAccountId: String,
    accounts: List<Account>,
    onAccountClick: () -> Unit,
    hasSettlementDate: Boolean,
    settlementDateMillis: Long,
    onSettlementClick: () -> Unit,
    selectedCategoryIds: Set<Long>,
    categories: List<Category>,
    onCategoryClick: () -> Unit,
    counterparty: String,
    onCounterpartyChange: (String) -> Unit,
    note: String,
    onNoteChange: (String) -> Unit,
    onHasSettlementDateChange: (Boolean) -> Unit,
    selectedTags: Set<String>,
    onShowTagSheet: () -> Unit,
    onRemoveTag: (String) -> Unit,
    counterpartySuggestions: List<String>,
    noteSuggestions: List<String>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Impact Header Card
        LoanHeaderCard(
            loanType = loanType,
            onTypeSelected = onTypeSelected,
            name = name,
            onNameChange = onNameChange,
            amount = amount,
            currency = currency,
            onCurrencyChange = onCurrencyChange,
            supportedCurrencies = supportedCurrencies,
            onAmountClick = onAmountClick
        )

        // Context Action Cards
        LoanActionCards(
            startDateMillis = startDateMillis,
            onStartDateClick = onStartDateClick,
            interestRate = interestRate,
            onInterestClick = onInterestClick,
            linkedAccountId = linkedAccountId,
            accounts = accounts,
            onAccountClick = onAccountClick,
            hasSettlementDate = hasSettlementDate,
            settlementDateMillis = settlementDateMillis,
            onSettlementClick = onSettlementClick,
            selectedCategoryIds = selectedCategoryIds,
            categories = categories,
            onCategoryClick = onCategoryClick
        )

        // Detail Section
        LoanDetailSection(
            counterparty = counterparty,
            onCounterpartyChange = onCounterpartyChange,
            loanType = loanType,
            note = note,
            onNoteChange = onNoteChange,
            hasSettlementDate = hasSettlementDate,
            onHasSettlementDateChange = onHasSettlementDateChange,
            selectedTags = selectedTags,
            onShowTagSheet = onShowTagSheet,
            onRemoveTag = onRemoveTag,
            counterpartySuggestions = counterpartySuggestions,
            noteSuggestions = noteSuggestions
        )
    }
}
