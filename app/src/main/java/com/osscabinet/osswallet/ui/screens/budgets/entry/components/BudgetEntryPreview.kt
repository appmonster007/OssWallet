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
fun BudgetEntryPreview() {
    OssWalletTheme {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        com.osscabinet.ossWallet.ui.components.entry.AeonImpactHeaderCard(
            transactionType = TransactionType.INCOME,
            onTypeSelected = {},
            amount = "500.00",
            currency = "USD",
            onCurrencyClick = {},
            onAmountClick = {},
            name = "Monthly Groceries",
            onNameChange = {},
            nameLabel = "BUDGET NAME",
            showTypeSelector = false,
            amountColor = androidx.compose.ui.graphics.Color(0xFF004D40)
        )

            BudgetActionCards(
                period = BudgetPeriod.MONTHLY,
                onPeriodClick = {},
                selectedAccountIds = emptySet(),
                accounts = emptyList(),
                onAccountClick = {},
                selectedTrackTypes = setOf(TransactionType.EXPENSE),
                onTrackTypeClick = {},
                selectedCategoryIds = emptySet(),
                categories = emptyList(),
                onCategoryClick = {}
            )

            BudgetDetailSection(
                note = "Try to keep it under 500",
                onNoteChange = {},
                alertThreshold = 0.8f,
                onAlertThresholdChange = {},
                rollover = true,
                onRolloverChange = {},
                selectedTags = setOf("Grocery", "Monthly"),
                onShowTagSheet = {},
                onRemoveTag = {}
            )
        }
    }
}
