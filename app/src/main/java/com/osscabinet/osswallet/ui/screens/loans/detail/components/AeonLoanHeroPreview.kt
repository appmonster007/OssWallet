package com.osscabinet.ossWallet.ui.screens.loans.detail.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Loan
import com.osscabinet.ossWallet.domain.model.LoanSummary
import com.osscabinet.ossWallet.domain.model.LoanType
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import java.math.BigDecimal

@Preview(showBackground = true)
@Composable
fun AeonLoanHeroPreview() {
    val loan = Loan(
        id = 1,
        name = "Personal Loan",
        principalAmount = BigDecimal("10000.00"),
        currency = "USD",
        startDate = System.currentTimeMillis(),
        lenderOrBorrowerName = "John Doe",
        loanType = LoanType.LENT,
        profileId = 1,
        interestRate = 5.0f,
        linkedAccountId = "1"
    )
    
    val summary = LoanSummary(
        loan = loan,
        remainingBalance = BigDecimal("7500.00"),
        totalPaid = BigDecimal("2500.00"),
        interestAccrued = BigDecimal("125.00")
    )

    OssWalletTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            LoanHeroCard(loan = loan, summary = summary)
        }
    }
}
