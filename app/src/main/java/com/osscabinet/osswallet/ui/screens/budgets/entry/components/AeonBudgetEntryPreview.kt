package com.osscabinet.ossWallet.ui.screens.budgets.entry.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.BudgetPeriod
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.ui.screens.budgets.entry.BudgetEntryState
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

@Preview(showBackground = true)
@Composable
fun AeonBudgetEntryPreview() {
    OssWalletTheme {
        BudgetFormBody(
            state = BudgetEntryState(
                name = "Monthly Groceries",
                limitAmount = "500.00",
                currency = "USD",
                period = BudgetPeriod.MONTHLY,
                selectedTrackTypes = setOf(TransactionType.EXPENSE),
                note = "Try to keep it under 500",
                alertThreshold = 0.8f,
                rollover = true,
                selectedTags = setOf("Grocery", "Monthly"),
                selectedCategoryIds = setOf(1L) // Simulating a selection
            ),
            onEvent = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
