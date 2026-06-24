package com.osscabinet.ossWallet.ui.screens.transactions.entry

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountRef
import com.osscabinet.ossWallet.domain.model.AmountEditingSide
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.resolve
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionSide
import com.osscabinet.ossWallet.domain.model.TransactionSplit
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.dto.TransactionPrefillDTO
import com.osscabinet.ossWallet.ui.components.entry.EntryField
import java.math.BigDecimal

/**
 * Immutable state holder for transaction entry form.
 * All UI state is centralized here for predictable state management.
 */
data class TransactionEntryState(
    // Core form fields
    val source: TransactionSide = TransactionSide(),
    val destination: TransactionSide = TransactionSide(),
    val activeAmountSide: AmountEditingSide = AmountEditingSide.SOURCE,
    val selectedTags: Set<String> = emptySet(),
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val status: TransactionStatus = TransactionStatus.CLEARED,
    val entryMode: EntryMode = EntryMode.EXPENSE,
    val selectedDateMillis: Long = System.currentTimeMillis(),
    val payee: String = "",
    val note: String = "",

    // Warranty
    val isWarrantyEnabled: Boolean = false,
    val warrantyExpirationMillis: Long = System.currentTimeMillis() + 31536000000L, // +1 year

    // Location
    val gpsLocation: String? = null,
    val location: String? = null,

    // Linked transactions
    val linkedTransactionIds: List<Long> = emptyList(),

    // Transfer specific
    val exchangeRate: BigDecimal = BigDecimal.ONE,

    // Template
    val selectedTemplateName: String? = null,

    // Splitting
    val isSplitMode: Boolean = false,
    val splits: List<TransactionSplit> = emptyList(),

    // Sheet visibility flags
    val showDatePicker: Boolean = false,
    val showCategorySheet: Boolean = false,
    val showAccountSheet: Boolean = false,
    val showTargetAccountSheet: Boolean = false,
    val showExchangeRateDialog: Boolean = false,
    val showTemplateSheet: Boolean = false,
    val showLinkedTransactionSheet: Boolean = false,
    val showTagSheet: Boolean = false,
    val showPaymentMethodSheet: Boolean = false,
    val showStatusSheet: Boolean = false,
    val showWarrantyDatePicker: Boolean = false,
    val showLocationSheet: Boolean = false,
    val showSplitSheet: Boolean = false,
    val showSplitCategorySheet: Int? = null,
    val showSplitNumpad: Int? = null,
    val isNumpadVisible: Boolean = false,
    val showCurrencySheet: Boolean = false,
    val showTargetCurrencySheet: Boolean = false,

    // External data (populated from repositories)
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val allTags: List<Tag> = emptyList(),
    val templates: List<Template> = emptyList(),
    val allPayees: List<String> = emptyList(),
    val allNotes: List<String> = emptyList(),
    val exchangeRates: Map<String, BigDecimal> = emptyMap(),
    val mapsAccessEnabled: Boolean = false,
    val shortlistedCurrencies: List<String> = emptyList(),
    val fullProviderCurrencies: List<String> = emptyList(),

    // Edit mode data
    val transactionId: Long? = null,
    val existingTransaction: Transaction? = null,
    val linkedTxs: List<Transaction> = emptyList(),
    val prefillDto: TransactionPrefillDTO? = null,

    // Navigation
    val isEditMode: Boolean = false,
    val navTriggered: Boolean = false
) {
    // Legacy mapping for UI and logic compatibility
    val amount: String get() = source.amount
    val targetAmount: String get() = destination.amount
    val selectedCategoryId: Long get() = source.categoryId
    val selectedAccount: Account? get() = source.account.resolve(accounts)
    val selectedCurrency: String get() = source.currency
    val targetAccount: Account? get() = destination.account.resolve(accounts)
    val targetCurrency: String get() = destination.currency

    val isIncome: Boolean get() = entryMode == EntryMode.INCOME
    val isExpense: Boolean get() = entryMode == EntryMode.EXPENSE
    val isTransfer: Boolean get() = entryMode == EntryMode.TRANSFER

    val displayLinkedTxs: List<Transaction>
        get() = linkedTxs.sortedByDescending { it.timestamp }

    val amountValue: BigDecimal
        get() = source.amountValue

    val errors: Set<EntryField>
        get() = buildSet {
            // Amount validation (Allowing 0.00 is fine, but negative or unparseable is not)
            if (amountValue < BigDecimal.ZERO) add(EntryField.AMOUNT)

            // Account validation
            if (isTransfer) {
                // Both sides must be explicitly chosen (Unselected = not yet picked)
                if (source.account is AccountRef.Unselected) add(EntryField.ACCOUNT)
                if (destination.account is AccountRef.Unselected) add(EntryField.TARGET_ACCOUNT)
                // Self-transfer guard: source and destination must not be the same wallet account
                if (source.account is AccountRef.Wallet &&
                    destination.account is AccountRef.Wallet &&
                    source.account.id == (destination.account as AccountRef.Wallet).id) {
                    add(EntryField.ACCOUNT)
                    add(EntryField.TARGET_ACCOUNT)
                }
                // Wallet minimum rule: at least one side must be an internal wallet account
                if (source.account is AccountRef.Outside && destination.account is AccountRef.Outside) {
                    add(EntryField.ACCOUNT)
                    add(EntryField.TARGET_ACCOUNT)
                }
            } else {
                if (source.account !is AccountRef.Wallet) add(EntryField.ACCOUNT)
            }

            // Split validation
            if (isSplitMode) {
                val totalSplitAmount =
                    splits.sumOf { it.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO }
                if (totalSplitAmount != amountValue) add(EntryField.AMOUNT)
                if (splits.any { it.categoryId == null }) add(EntryField.CATEGORY)
            }
        }

    val hasError: Boolean get() = errors.isNotEmpty()

    val canSave: Boolean
        get() = !hasError && !navTriggered

    fun hasError(field: EntryField) = errors.contains(field)
}

/**
 * Sealed class representing events that can modify the transaction entry state.
 */
