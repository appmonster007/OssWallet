package com.osscabinet.ossWallet.ui.screens.budgets.entry.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.BudgetPeriod
import com.osscabinet.ossWallet.domain.model.BudgetTrackType
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

@Preview(showBackground = true)
@Composable
fun BudgetEntryPreview() {
    OssWalletTheme {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BudgetHeaderCard(
                name = "Monthly Groceries",
                onNameChange = {},
                amount = "500.00",
                currency = "USD",
                onCurrencyChange = {},
                supportedCurrencies = listOf("USD", "EUR"),
                onAmountClick = {}
            )

            BudgetActionCards(
                period = BudgetPeriod.MONTHLY,
                onPeriodClick = {},
                selectedAccountIds = emptySet(),
                accounts = emptyList(),
                onAccountClick = {},
                selectedTrackTypes = setOf(BudgetTrackType.EXPENSE),
                onTrackTypeClick = {},
                selectedCategoryIds = emptySet(),
                categories = emptyList(),
                onCategoryClick = {},
                selectedTags = emptySet(),
                onTagClick = {}
            )

            BudgetDetailSection(
                note = "Try to keep it under 500",
                onNoteChange = {},
                alertThreshold = 0.8f,
                onAlertThresholdChange = {},
                rollover = true,
                onRolloverChange = {}
            )
        }
    }
}
