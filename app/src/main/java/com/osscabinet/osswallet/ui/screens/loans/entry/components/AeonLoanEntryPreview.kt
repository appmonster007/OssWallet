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
                note = "Main residence mortgage"
            ),
            onEvent = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
