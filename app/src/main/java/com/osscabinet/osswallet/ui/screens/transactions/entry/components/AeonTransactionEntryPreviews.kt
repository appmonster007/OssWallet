package com.osscabinet.ossWallet.ui.screens.transactions.entry.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountRef
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.CategoryNature
import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.TransactionSide
import com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryContent
import com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryState
import com.osscabinet.ossWallet.ui.theme.LocalGlassmorphismEnabled
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEntryScreenPreviewContent(
    state: TransactionEntryState,
    showDeleteConfirmation: Boolean = false,
    glassEnabled: Boolean = true,
    theme: String = "System"
) {
    OssWalletTheme(theme = theme) {
        CompositionLocalProvider(LocalGlassmorphismEnabled provides glassEnabled) {
            Surface {
                TransactionEntryContent(
                    state = state,
                    onEvent = {},
                    onSearch = { flowOf(emptyList()) },
                    onBackClick = {},
                    onSaveClick = {},
                    onDeleteConfirm = {},
                    showDeleteConfirmation = showDeleteConfirmation,
                    onShowDeleteConfirmation = {}
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "New Expense")
@Composable
fun PreviewNewExpense() {
    TransactionEntryScreenPreviewContent(
        state = TransactionEntryState(
            entryMode = EntryMode.EXPENSE,
            source = TransactionSide(
                amount = "0.00",
                currency = "USD",
                account = AccountRef.Unselected
            )
        )
    )
}

@Preview(showBackground = true, name = "Edit Transaction")
@Composable
fun PreviewEditTransaction() {
    TransactionEntryScreenPreviewContent(
        state = TransactionEntryState(
            isEditMode = true,
            entryMode = EntryMode.EXPENSE,
            source = TransactionSide(
                amount = "42.50",
                currency = "EUR",
                account = AccountRef.Wallet("wallet1"),
                categoryId = 101L
            ),
            payee = "Starbucks",
            note = "Morning coffee",
            selectedDateMillis = System.currentTimeMillis(),
            categories = listOf(
                Category(
                    id = 101L,
                    name = "Food & Drinks",
                    iconName = "coffee",
                    colorHex = 0xFF4CAF50.toInt(),
                    isDefault = false,
                    parentId = null,
                    nature = CategoryNature.WANT
                )
            ),
            accounts = listOf(
                Account(
                    id = "wallet1",
                    name = "Main Wallet",
                    currencyCode = "EUR",
                    structureType = AccountStructureType.CASH,
                    colorHex = "#4CAF50",
                    icon = "Wallet"
                )
            )
        )
    )
}

@Preview(showBackground = true, name = "New Transfer")
@Composable
fun PreviewNewTransfer() {
    TransactionEntryScreenPreviewContent(
        state = TransactionEntryState(
            entryMode = EntryMode.TRANSFER,
            source = TransactionSide(
                amount = "100.00",
                currency = "USD",
                account = AccountRef.Wallet("wallet1")
            ),
            destination = TransactionSide(
                amount = "100.00",
                currency = "USD",
                account = AccountRef.Wallet("wallet2")
            ),
            accounts = listOf(
                Account(
                    id = "wallet1",
                    name = "Checking",
                    currencyCode = "USD",
                    structureType = AccountStructureType.BANK,
                    colorHex = "#2196F3",
                    icon = "AccountBalance"
                ),
                Account(
                    id = "wallet2",
                    name = "Savings",
                    currencyCode = "USD",
                    structureType = AccountStructureType.SAVINGS,
                    colorHex = "#9C27B0",
                    icon = "Savings"
                )
            )
        )
    )
}

@Preview(showBackground = true, name = "Delete Confirmation")
@Composable
fun PreviewDeleteConfirmation() {
    TransactionEntryScreenPreviewContent(
        state = TransactionEntryState(
            isEditMode = true,
            entryMode = EntryMode.EXPENSE,
            source = TransactionSide(amount = "10.00", currency = "USD")
        ),
        showDeleteConfirmation = true
    )
}

@Preview(showBackground = true, name = "Non-Glass Mode")
@Composable
fun PreviewNonGlassMode() {
    TransactionEntryScreenPreviewContent(
        state = TransactionEntryState(
            entryMode = EntryMode.EXPENSE,
            source = TransactionSide(
                amount = "250.00",
                currency = "USD",
                account = AccountRef.Wallet("wallet1")
            ),
            accounts = listOf(
                Account(
                    id = "wallet1",
                    name = "Main Wallet",
                    currencyCode = "USD",
                    structureType = AccountStructureType.CASH,
                    colorHex = "#4CAF50",
                    icon = "Wallet"
                )
            )
        ),
        glassEnabled = false
    )
}

@Preview(showBackground = true, name = "AMOLED Non-Glass")
@Composable
fun PreviewAmoledNonGlass() {
    TransactionEntryScreenPreviewContent(
        state = TransactionEntryState(
            entryMode = EntryMode.EXPENSE,
            source = TransactionSide(
                amount = "1500.00",
                currency = "USD",
                account = AccountRef.Wallet("wallet1")
            ),
            accounts = listOf(
                Account(
                    id = "wallet1",
                    name = "Main Wallet",
                    currencyCode = "USD",
                    structureType = AccountStructureType.CASH,
                    colorHex = "#4CAF50",
                    icon = "Wallet"
                )
            )
        ),
        glassEnabled = false,
        theme = "Amoled"
    )
}
