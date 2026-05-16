package com.osscabinet.ossWallet.ui.screens.previews

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.ui.screens.budgets.entry.BudgetEntryState
import com.osscabinet.ossWallet.ui.screens.budgets.entry.components.BudgetFormBody
import com.osscabinet.ossWallet.ui.screens.loans.entry.LoanEntryState
import com.osscabinet.ossWallet.ui.screens.loans.entry.components.LoanFormBody
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

@Preview(showBackground = true, widthDp = 400, heightDp = 3500, name = "Full Loan Entry Screen")
@Composable
fun FullLoanEntryPreview() {
    OssWalletTheme {
        Surface {
            LoanFormBody(
                state = LoanEntryState(
                    name = "Business Expansion Loan",
                    principalAmount = "50000",
                    currency = "USD",
                    loanType = LoanType.LENT,
                    counterparty = "Tech Ventures Inc.",
                    interestRate = "4.25",
                    note = "Funding for the new office equipment and software licenses.",
                    hasWarranty = true,
                    warrantyExpirationStart = System.currentTimeMillis(),
                    warrantyExpirationEnd = System.currentTimeMillis() + 31536000000L, // 1 year
                    selectedTags = setOf("Business", "Investment", "Software"),
                    linkedTransactionIds = setOf(101L, 102L),
                    queryNote = "Equipment",
                    queryPayee = "Tech Ventures",
                    isNoteRegex = false,
                    isPayeeRegex = false,
                    minAmount = "100",
                    maxAmount = "10000",
                    excludeFuture = true
                ),
                onEvent = {}
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 3500, name = "Full Budget Entry Screen")
@Composable
fun FullBudgetEntryPreview() {
    OssWalletTheme {
        Surface {
            BudgetFormBody(
                state = BudgetEntryState(
                    name = "European Summer Vacation",
                    limitAmount = "4500",
                    currency = "EUR",
                    note = "Budget for flights, accommodation, and daily expenses in Italy and Greece.",
                    hasWarranty = true,
                    warrantyExpirationStart = System.currentTimeMillis(),
                    warrantyExpirationEnd = System.currentTimeMillis() + 15768000000L, // 6 months
                    selectedTags = setOf("Travel", "Vacation", "Summer 2024"),
                    alertThreshold = 0.85f,
                    rollover = false,
                    queryNote = "Vacation",
                    queryPayee = "Airline|Hotel",
                    isNoteRegex = true,
                    isPayeeRegex = true,
                    minAmount = "10",
                    maxAmount = "2000"
                ),
                onEvent = {}
            )
        }
    }
}
