package com.osscabinet.ossWallet.ui.screens.budgets.detail.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.model.BudgetAlertLevel
import com.osscabinet.ossWallet.domain.model.BudgetProgress
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import java.math.BigDecimal

@Preview(showBackground = true)
@Composable
fun AeonBudgetHeroPreview() {
    val budget = Budget(
        id = 1,
        name = "Monthly Entertainment",
        currency = "USD",
        limitAmount = BigDecimal("200.00"),
        timelineRange = TimelineRange.ThisPeriod(ThisPeriodType.MONTH),
        profileId = 1,
        alertThreshold = 0.8f
    )
    
    val progress = BudgetProgress(
        budget = budget,
        spent = BigDecimal("175.00"),
        limit = BigDecimal("200.00"),
        remaining = BigDecimal("25.00"),
        percentage = 0.875f,
        alertLevel = BudgetAlertLevel.WARNING
    )

    OssWalletTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            BudgetHeroCard(budget = budget, progress = progress)
        }
    }
}
