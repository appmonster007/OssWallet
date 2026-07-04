package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.osscabinet.ossWallet.R
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AmountEditingSide
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.ui.components.AeonDatePickerDialog
import com.osscabinet.ossWallet.ui.components.AeonDateTimePicker
import com.osscabinet.ossWallet.ui.components.ExchangeRateDialog
import com.osscabinet.ossWallet.ui.components.entry.*
import java.math.BigDecimal

@Composable
fun EntrySheetOrchestrator(
    sheetState: FormSheetState,
    accounts: List<Account>,
    categories: List<Category>,
    allTags: List<Tag>,
    selectedTags: Set<String>,
    currentAmount: String,
    currentExchangeRate: BigDecimal,
    shortlistedCurrencies: List<String>,
    fullCurrencies: List<String>,
    isTransfer: Boolean,
    onAmountChange: (String) -> Unit,
    onAccountChange: (String?) -> Unit,
    onTargetAccountChange: (String?) -> Unit,
    onCategoryChange: (Long) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onTargetCurrencyChange: (String) -> Unit,
    onExchangeRateChange: (BigDecimal) -> Unit,
    onTagsChange: (Set<String>) -> Unit,
    onAddTag: (String, Int?) -> Unit,
    onDismissSheet: (EntryField?) -> Unit, // Using EntryField as a type identifier for which sheet to close
    onSearchTransactions: (String) -> kotlinx.coroutines.flow.Flow<List<Transaction>>,
    linkedIds: Set<Long>,
    onLinkedTransactionsChanged: (Set<Long>) -> Unit,
    selectedAccountId: String? = null,
    selectedTargetAccountId: String? = null,
    selectedCategoryId: Long = -1L,
    selectedCurrency: String = "USD",
    selectedTargetCurrency: String = "USD",
    // Date and metadata specifics
    selectedDateMillis: Long? = null,
    onDateChange: ((Long) -> Unit)? = null,
    warrantyDateMillis: Long? = null,
    onWarrantyDateChange: ((Long) -> Unit)? = null,
    gpsLocation: String? = null,
    onLocationChange: ((String?, String?) -> Unit)? = null,
    paymentMethod: com.osscabinet.ossWallet.domain.model.PaymentMethod? = null,
    onPaymentMethodChange: ((com.osscabinet.ossWallet.domain.model.PaymentMethod) -> Unit)? = null,
    transactionStatus: com.osscabinet.ossWallet.domain.model.TransactionStatus? = null,
    onTransactionStatusChange: ((com.osscabinet.ossWallet.domain.model.TransactionStatus) -> Unit)? = null,
    currentTransactionId: Long? = null
) {
    if (sheetState.isNumpadVisible) {
        AeonCalculatorSheet(
            currentAmount = currentAmount,
            onAmountChanged = onAmountChange,
            onDismiss = { onDismissSheet(EntryField.AMOUNT) }
        )
    }

    if (sheetState.showAccount) {
        val filtered = if (isTransfer) listOf(Account.OUTSIDE_WALLET) + accounts else accounts
        AeonAccountSelection(
            accounts = filtered,
            selectedAccountIds = selectedAccountId?.let { setOf(it) } ?: emptySet(),
            onSelectionChange = { ids -> onAccountChange(ids.firstOrNull()) },
            onDismiss = { onDismissSheet(EntryField.ACCOUNT) },
            title = "Select Account"
        )
    }

    if (sheetState.showTargetAccount) {
        val filtered = listOf(Account.OUTSIDE_WALLET) + accounts
        AeonAccountSelection(
            accounts = filtered,
            selectedAccountIds = selectedTargetAccountId?.let { setOf(it) } ?: emptySet(),
            onSelectionChange = { ids -> onTargetAccountChange(ids.firstOrNull()) },
            onDismiss = { onDismissSheet(EntryField.TARGET_ACCOUNT) },
            title = stringResource(R.string.target_account_selection_title)
        )
    }

    if (sheetState.showCategory) {
        AeonCategorySelection(
            categories = categories.filter { !Category.isInitialBalance(it.id) },
            selectedCategoryIds = setOf(selectedCategoryId),
            onSelectionChange = { ids -> onCategoryChange(ids.firstOrNull() ?: -1L) },
            onDismiss = { onDismissSheet(EntryField.CATEGORY) }
        )
    }

    if (sheetState.showCurrency) {
        AeonCurrencySelection(
            selectedCurrencies = setOf(selectedCurrency),
            onSelectionChange = { ids -> ids.firstOrNull()?.let { onCurrencyChange(it) } },
            onDismiss = { onDismissSheet(null) }, // Using null as we don't have a currency field in EntryField yet
            currencies = fullCurrencies.ifEmpty { shortlistedCurrencies }
        )
    }

    if (sheetState.showTargetCurrency) {
        AeonCurrencySelection(
            selectedCurrencies = setOf(selectedTargetCurrency),
            onSelectionChange = { ids -> ids.firstOrNull()?.let { onTargetCurrencyChange(it) } },
            onDismiss = { onDismissSheet(null) },
            currencies = fullCurrencies.ifEmpty { shortlistedCurrencies }
        )
    }

    if (sheetState.showTag) {
        AeonTagSelectionDialog(
            tags = allTags,
            selectedTags = selectedTags,
            onTagsSelected = onTagsChange,
            onAddTag = onAddTag,
            onDismiss = { onDismissSheet(null) }
        )
    }

    if (sheetState.showExchangeRate) {
        ExchangeRateDialog(
            initialRate = currentExchangeRate,
            onConfirm = { onExchangeRateChange(it); onDismissSheet(null) },
            onDismiss = { onDismissSheet(null) }
        )
    }

    if (sheetState.showDate && selectedDateMillis != null && onDateChange != null) {
        AeonDateTimePicker(
            initialDateTime = selectedDateMillis,
            onDateTimeSelected = onDateChange,
            onDismiss = { onDismissSheet(null) }
        )
    }

    if (sheetState.showWarrantyDate && onWarrantyDateChange != null) {
        AeonDatePickerDialog(
            selectedDateMillis = warrantyDateMillis ?: (System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000),
            onDateSelected = onWarrantyDateChange,
            onDismiss = { onDismissSheet(null) }
        )
    }

    if (sheetState.showLocation && onLocationChange != null) {
        AeonLocationSelection(
            gpsLocation = gpsLocation,
            onLocationSelected = onLocationChange,
            onDismiss = { onDismissSheet(null) }
        )
    }

    if (sheetState.showPaymentMethod && onPaymentMethodChange != null) {
        AeonPaymentMethodSelection(
            selectedMethods = if (paymentMethod != null) setOf(paymentMethod) else emptySet(),
            onSelectionChange = { methods -> methods.firstOrNull()?.let { onPaymentMethodChange(it) } },
            onDismiss = { onDismissSheet(null) }
        )
    }

    if (sheetState.showStatus && transactionStatus != null && onTransactionStatusChange != null) {
        AeonStatusSelection(
            selectedStatuses = setOf(transactionStatus),
            onSelectionChange = { statuses -> statuses.firstOrNull()?.let { onTransactionStatusChange(it) } },
            onDismiss = { onDismissSheet(null) }
        )
    }

    if (sheetState.showLinkedTransaction) {
        AeonLinkedTransactionSelection(
            onSearch = onSearchTransactions,
            currentTransactionId = currentTransactionId,
            linkedIds = linkedIds,
            accounts = accounts,
            categories = categories,
            allTags = allTags,
            onLinkedTransactionsChanged = onLinkedTransactionsChanged,
            onDismiss = { onDismissSheet(null) }
        )
    }
}
