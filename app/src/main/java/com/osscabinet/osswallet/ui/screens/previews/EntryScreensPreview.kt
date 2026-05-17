package com.osscabinet.ossWallet.ui.screens.previews

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.ui.screens.accounts.entry.AccountEntryState
import com.osscabinet.ossWallet.ui.screens.accounts.entry.components.AccountFormBody
import com.osscabinet.ossWallet.ui.screens.categories.list.components.CategoryEditSheet
import com.osscabinet.ossWallet.ui.screens.budgets.entry.BudgetEntryState
import com.osscabinet.ossWallet.ui.screens.budgets.entry.components.BudgetFormBody
import com.osscabinet.ossWallet.ui.screens.filters.entry.FilterEntryState
import com.osscabinet.ossWallet.ui.screens.filters.entry.components.FilterFormBody
import com.osscabinet.ossWallet.ui.screens.loans.entry.LoanEntryState
import com.osscabinet.ossWallet.ui.screens.loans.entry.components.LoanFormBody
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import java.math.BigDecimal



@Preview(showBackground = true, widthDp = 400, heightDp = 1500, name = "Account Entry Screen - Edit")
@Composable
fun AccountEntryEditPreview() {
    OssWalletTheme {
        Surface {
            AccountFormBody(
                state = AccountEntryState(
                    accountName = "Primary Credit Card",
                    structureType = AccountStructureType.CREDIT_CARD,
                    primaryCurrency = "EUR",
                    allowedCurrencies = setOf("EUR"),
                    selectedIconName = "CreditCard",
                    accentColorHex = "#B71C1C",
                    currentBalance = BigDecimal("-1250.50"),
                    isEditMode = true
                ),
                onEvent = {}
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 800, name = "Visual Identity Dialog Preview")
@Composable
fun VisualIdentityDialogPreview() {
    OssWalletTheme {
        Surface {
            AccountFormBody(
                state = AccountEntryState(
                    accountName = "Dialog Test",
                    showIconSheet = true
                ),
                onEvent = {}
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 800, name = "Category Entry Dialog Preview")
@Composable
fun CategoryEntryDialogPreview() {
    OssWalletTheme {
        Surface {
            CategoryEditSheet(
                category = null,
                parentCategories = listOf(
                    Category(id = 1L, name = "Food", iconName = "Fastfood", colorHex = 0xFF4CAF50.toInt(), isDefault = false, parentId = null),
                    Category(id = 2L, name = "Transport", iconName = "DirectionsBus", colorHex = 0xFF2196F3.toInt(), isDefault = false, parentId = null)
                ),
                onDismiss = {},
                onSave = { _, _, _, _, _ -> }
            )
        }
    }
}
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

@Preview(showBackground = true, widthDp = 400, heightDp = 3500, name = "Full Filter Entry Screen")
@Composable
fun FullFilterEntryPreview() {
    OssWalletTheme {
        Surface {
            FilterFormBody(
                state = FilterEntryState(
                    name = "Business Meals & Coffee",
                    selectedTransactionTypes = setOf(TransactionType.EXPENSE),
                    selectedCategoryIds = setOf(1L, 2L, 3L),
                    selectedCurrencies = setOf("USD", "EUR"),
                    queryNote = "Lunch|Dinner|Coffee",
                    isNoteRegex = true,
                    queryPayee = "Starbucks|Subway",
                    isPayeeRegex = true,
                    selectedTags = setOf("Business", "Food"),
                    minAmount = "5",
                    maxAmount = "500",
                    hasWarranty = false,
                    excludeFuture = true
                ),
                onEvent = {}
            )
        }
    }
}
