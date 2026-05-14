package com.osscabinet.ossWallet.ui.screens.budgets.entry.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.BudgetPeriod
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.Category

@Composable
fun BudgetFormBody(
    name: String,
    onNameChange: (String) -> Unit,
    amount: String,
    currency: String,
    onCurrencyChange: (String) -> Unit,
    supportedCurrencies: List<String>,
    onAmountClick: () -> Unit,
    period: BudgetPeriod,
    onPeriodClick: () -> Unit,
    selectedAccountIds: Set<String>,
    accounts: List<Account>,
    onAccountClick: () -> Unit,
    selectedTrackTypes: Set<TransactionType>,
    onTrackTypeClick: () -> Unit,
    selectedCategoryIds: Set<Long>,
    categories: List<Category>,
    onCategoryClick: () -> Unit,
    note: String,
    onNoteChange: (String) -> Unit,
    alertThreshold: Float,
    onAlertThresholdChange: (Float) -> Unit,
    rollover: Boolean,
    onRolloverChange: (Boolean) -> Unit,
    selectedTags: Set<String>,
    onShowTagSheet: () -> Unit,
    onRemoveTag: (String) -> Unit,
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
        BudgetHeaderCard(
            name = name,
            onNameChange = onNameChange,
            amount = amount,
            currency = currency,
            onCurrencyChange = onCurrencyChange,
            supportedCurrencies = supportedCurrencies,
            onAmountClick = onAmountClick
        )

        // Context Action Cards Grid
        BudgetActionCards(
            period = period,
            onPeriodClick = onPeriodClick,
            selectedAccountIds = selectedAccountIds,
            accounts = accounts,
            onAccountClick = onAccountClick,
            selectedTrackTypes = selectedTrackTypes,
            onTrackTypeClick = onTrackTypeClick,
            selectedCategoryIds = selectedCategoryIds,
            categories = categories,
            onCategoryClick = onCategoryClick
        )

        // Detail Section
        BudgetDetailSection(
            note = note,
            onNoteChange = onNoteChange,
            alertThreshold = alertThreshold,
            onAlertThresholdChange = onAlertThresholdChange,
            rollover = rollover,
            onRolloverChange = onRolloverChange,
            selectedTags = selectedTags,
            onShowTagSheet = onShowTagSheet,
            onRemoveTag = onRemoveTag,
            noteSuggestions = noteSuggestions
        )
    }
}
