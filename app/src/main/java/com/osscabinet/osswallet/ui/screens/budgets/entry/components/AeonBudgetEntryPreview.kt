package com.osscabinet.ossWallet.ui.screens.budgets.entry.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.BudgetPeriod
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

@Preview(showBackground = true)
@Composable
fun AeonBudgetEntryPreview() {
    OssWalletTheme {
        BudgetFormBody(
            name = "Monthly Groceries",
            onNameChange = {},
            amount = "500.00",
            currency = "USD",
            onCurrencyChange = {},
            supportedCurrencies = listOf("USD", "EUR"),
            onAmountClick = {},
            period = BudgetPeriod.MONTHLY,
            onPeriodClick = {},
            selectedAccountIds = emptySet(),
            accounts = emptyList(),
            onAccountClick = {},
            selectedTrackTypes = setOf(TransactionType.EXPENSE),
            onTrackTypeClick = {},
            selectedCurrencies = emptySet(),
            onCurrencyFilterClick = {},
            selectedCategoryIds = emptySet(),
            categories = emptyList(),
            onCategoryClick = {},
            note = "Try to keep it under 500",
            onNoteChange = {},
            alertThreshold = 0.8f,
            onAlertThresholdChange = {},
            rollover = true,
            onRolloverChange = {},
            selectedTags = setOf("Grocery", "Monthly"),
            onShowTagSheet = {},
            onRemoveTag = {},
            selectedPaymentMethods = emptySet(),
            onPaymentMethodClick = {},
            selectedStatuses = emptySet(),
            onStatusClick = {},
            linkedTransactionIds = emptySet(),
            onLinkRecordClick = {},
            filterId = null,
            onFilterClick = {},
            availableFilters = emptyList(),
            noteSuggestions = emptyList(),
            modifier = Modifier.padding(16.dp)
        )
    }
}
