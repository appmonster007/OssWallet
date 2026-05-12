package com.osscabinet.ossWallet.ui.screens.templates.entry.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.ui.components.CalculatorNumpad
import com.osscabinet.ossWallet.ui.components.accounts.AccountSelection
import com.osscabinet.ossWallet.ui.screens.transactions.entry.components.TransactionCategorySelection
import com.osscabinet.ossWallet.ui.screens.transactions.entry.sheets.PaymentMethodSelection
import com.osscabinet.ossWallet.ui.screens.transactions.entry.sheets.TagSelection

@Composable
fun TemplateEntrySheets(
    isNumpadVisible: Boolean,
    onDismissNumpad: () -> Unit,
    amount: String,
    onAmountChange: (String, String) -> Unit,
    transactionType: Int,
    decimalPrecision: Int,
    showAccountSheet: Boolean,
    onDismissAccountSheet: () -> Unit,
    accounts: List<Account>,
    selectedAccount: Account?,
    onAccountSelected: (Account) -> Unit,
    showCategorySheet: Boolean,
    onDismissCategorySheet: () -> Unit,
    categories: List<Category>,
    selectedCategoryId: Long,
    onCategorySelected: (String, Long) -> Unit,
    showPaymentMethodSheet: Boolean,
    onDismissPaymentMethodSheet: () -> Unit,
    paymentMethod: String,
    onPaymentMethodSelected: (String) -> Unit,
    showTagSheet: Boolean,
    onDismissTagSheet: () -> Unit,
    allTags: List<Tag>,
    selectedTags: List<String>,
    onTagsSelected: (List<String>) -> Unit,
    onAddTag: (String, Int) -> Unit,
    onEditTag: (Tag) -> Unit,
    onDeleteTag: (Tag) -> Unit
) {
    if (isNumpadVisible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismissNumpad() }
        )
        
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            CalculatorNumpad(
                amount = amount,
                onAmountChange = onAmountChange,
                transactionType = if (transactionType == 1) "income" else "expense",
                decimalPrecision = decimalPrecision,
                onDone = onDismissNumpad,
                modifier = Modifier.padding(8.dp)
            )
        }
    }

    if (showAccountSheet || showCategorySheet || showPaymentMethodSheet || showTagSheet) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { 
                    onDismissAccountSheet()
                    onDismissCategorySheet()
                    onDismissPaymentMethodSheet()
                    onDismissTagSheet()
                }
        )
    }

    if (showAccountSheet) {
        AccountSelection(
            accounts = accounts,
            selectedAccount = selectedAccount,
            onAccountSelected = onAccountSelected,
            onDismiss = onDismissAccountSheet
        )
    }

    if (showCategorySheet) {
        TransactionCategorySelection(
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            onCategorySelected = onCategorySelected,
            onDismiss = onDismissCategorySheet
        )
    }

    if (showPaymentMethodSheet) {
        PaymentMethodSelection(
            selectedMethod = paymentMethod,
            onMethodSelected = onPaymentMethodSelected,
            onDismiss = onDismissPaymentMethodSheet
        )
    }

    if (showTagSheet) {
        TagSelection(
            tags = allTags,
            selectedTags = selectedTags,
            onTagsSelected = onTagsSelected,
            onAddTag = onAddTag,
            onEditTag = onEditTag,
            onDeleteTag = onDeleteTag,
            onDismiss = onDismissTagSheet
        )
    }
}
