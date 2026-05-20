package com.osscabinet.ossWallet.ui.screens.loans.entry.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.LoanType
import com.osscabinet.ossWallet.ui.screens.loans.entry.LoanEntryState
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

@Preview(showBackground = true)
@Composable
fun AeonLoanEntryPreview() {
    OssWalletTheme {
        LoanFormBody(
            state = LoanEntryState(
                name = "Home Loan",
                principalAmount = "250000",
                currency = "USD",
                loanType = LoanType.BORROWED,
                interestRate = "3.5",
                counterparty = "Bank of America",
                note = "Mortgage for the new house",
                startDate = 1715731200000L, // May 15, 2024
                hasSettlementDate = true,
                settlementDate = 1778803200000L // May 15, 2026
            ),
            onSearch = { kotlinx.coroutines.flow.flowOf(emptyList()) },
            onEvent = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
