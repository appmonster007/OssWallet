package com.osscabinet.ossWallet.ui.screens.templates.entry.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.ui.components.accounts.AccountSelection
import com.osscabinet.ossWallet.ui.screens.transactions.entry.components.TransactionAccountSelection
import com.osscabinet.ossWallet.ui.screens.transactions.entry.components.TransactionCategorySelection
import com.osscabinet.ossWallet.ui.screens.transactions.entry.sheets.PaymentMethodSelection
import com.osscabinet.ossWallet.ui.screens.transactions.entry.sheets.StatusSelection
import com.osscabinet.ossWallet.ui.screens.transactions.entry.sheets.TagSelection
import com.osscabinet.ossWallet.ui.components.entry.AeonCurrencySelection

@Composable
fun TemplateEntrySheets(
    isNumpadVisible: Boolean,
    onDismissNumpad: () -> Unit,
    amount: String,
    onAmountChange: (String) -> Unit,
    onTypeChangedFromNumpad: (String) -> Unit = {},
    transactionType: Int,
    decimalPrecision: Int,
    showAccountSheet: Boolean,
    onDismissAccountSheet: () -> Unit,
    showTargetAccountSheet: Boolean = false,
    onDismissTargetAccountSheet: () -> Unit = {},
    accounts: List<Account>,
    selectedAccount: Account?,
    onAccountSelected: (Account?) -> Unit,
    targetAccount: Account? = null,
    onTargetAccountSelected: (Account?) -> Unit = {},
    showCategorySheet: Boolean,
    onDismissCategorySheet: () -> Unit,
    categories: List<Category>,
    selectedCategoryId: Long,
    onCategorySelected: (String, Long) -> Unit,
    showPaymentMethodSheet: Boolean,
    onDismissPaymentMethodSheet: () -> Unit,
    paymentMethod: String,
    onPaymentMethodSelected: (String) -> Unit,
    showStatusSheet: Boolean = false,
    onDismissStatusSheet: () -> Unit = {},
    transactionStatus: com.osscabinet.ossWallet.domain.model.TransactionStatus = com.osscabinet.ossWallet.domain.model.TransactionStatus.CLEARED,
    onStatusSelected: (com.osscabinet.ossWallet.domain.model.TransactionStatus) -> Unit = {},
    showTagSheet: Boolean,
    onDismissTagSheet: () -> Unit,
    allTags: List<Tag>,
    selectedTags: List<String>,
    onTagsSelected: (List<String>) -> Unit,
    onAddTag: (String, Int) -> Unit,
    onEditTag: (Tag) -> Unit,
    onDeleteTag: (Tag) -> Unit,
    showCurrencySheet: Boolean = false,
    onDismissCurrencySheet: () -> Unit = {},
    currency: String = "USD",
    onCurrencySelected: (String) -> Unit = {},
    showTargetCurrencySheet: Boolean = false,
    onDismissTargetCurrencySheet: () -> Unit = {},
    targetCurrency: String = "USD",
    onTargetCurrencySelected: (String) -> Unit = {},
    showExchangeRateDialog: Boolean = false,
    onDismissExchangeRateDialog: () -> Unit = {},
    exchangeRate: java.math.BigDecimal = java.math.BigDecimal.ONE,
    onExchangeRateChanged: (java.math.BigDecimal) -> Unit = {},
    showWarrantyDatePicker: Boolean = false,
    onDismissWarrantyDatePicker: () -> Unit = {},
    warrantyExpirationMillis: Long = System.currentTimeMillis(),
    onWarrantyDateSelected: (Long) -> Unit = {},
    showLocationSheet: Boolean = false,
    onDismissLocationSheet: () -> Unit = {},
    latitude: Double? = null,
    longitude: Double? = null,
    onLocationSelected: (String, Double?, Double?) -> Unit = { _, _, _ -> }
) {
    if (isNumpadVisible) {
        com.osscabinet.ossWallet.ui.screens.transactions.entry.sheets.CalculatorSheet(
            currentAmount = amount,
            transactionType = transactionType,
            decimalPrecision = decimalPrecision,
            onAmountChanged = onAmountChange,
            onTypeChanged = onTypeChangedFromNumpad,
            onDismiss = onDismissNumpad
        )
    }

    if (showAccountSheet) {
        TransactionAccountSelection(
            accounts = accounts,
            selectedAccountId = selectedAccount?.id,
            onAccountSelected = onAccountSelected,
            onDismiss = onDismissAccountSheet,
            title = "Select Account",
            allowOutsideWallet = true
        )
    }

    if (showTargetAccountSheet) {
        TransactionAccountSelection(
            accounts = accounts,
            selectedAccountId = targetAccount?.id,
            onAccountSelected = onTargetAccountSelected,
            onDismiss = onDismissTargetAccountSheet,
            title = "Select Target Account",
            allowOutsideWallet = true
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

    if (showStatusSheet) {
        StatusSelection(
            selectedStatus = transactionStatus,
            onStatusSelected = onStatusSelected,
            onDismiss = onDismissStatusSheet
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

    if (showCurrencySheet) {
        AeonCurrencySelection(
            selectedCurrency = currency,
            onCurrencySelected = onCurrencySelected,
            onDismiss = onDismissCurrencySheet
        )
    }

    if (showTargetCurrencySheet) {
        AeonCurrencySelection(
            selectedCurrency = targetCurrency,
            onCurrencySelected = onTargetCurrencySelected,
            onDismiss = onDismissTargetCurrencySheet,
            title = "Select Target Currency"
        )
    }

    if (showExchangeRateDialog) {
        com.osscabinet.ossWallet.ui.components.ExchangeRateDialog(
            initialRate = exchangeRate,
            onConfirm = onExchangeRateChanged,
            onDismiss = onDismissExchangeRateDialog
        )
    }

    if (showWarrantyDatePicker) {
        com.osscabinet.ossWallet.ui.components.AeonDatePickerDialog(
            selectedDateMillis = warrantyExpirationMillis,
            onDateSelected = onWarrantyDateSelected,
            onDismiss = onDismissWarrantyDatePicker
        )
    }

    if (showLocationSheet) {
        com.osscabinet.ossWallet.ui.screens.transactions.entry.sheets.LocationSelection(
            currentCoords = if (latitude != null && longitude != null) latitude to longitude else null,
            onLocationSelected = { coords ->
                onLocationSelected("", coords?.first, coords?.second)
            },
            onDismiss = onDismissLocationSheet
        )
    }
}
