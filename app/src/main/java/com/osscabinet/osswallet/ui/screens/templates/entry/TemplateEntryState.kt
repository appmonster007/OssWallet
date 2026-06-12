package com.osscabinet.ossWallet.ui.screens.templates.entry

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountRef
import com.osscabinet.ossWallet.domain.model.AmountEditingSide
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.resolve
import com.osscabinet.ossWallet.domain.model.toStored
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionSide
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.dto.TransactionPrefillDTO
import com.osscabinet.ossWallet.ui.components.entry.EntryField
import java.math.BigDecimal

/**
 * Immutable state holder for template entry form.
 */
data class TemplateEntryState(
    // Core form fields
    val name: String = "",
    val source: TransactionSide = TransactionSide(currency = "USD"),
    val destination: TransactionSide = TransactionSide(),
    val activeAmountSide: AmountEditingSide = AmountEditingSide.SOURCE,
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val categoryName: String = "Unknown",
    val transferRate: BigDecimal = BigDecimal.ONE,
    val note: String = "",
    val payee: String = "",
    val location: String = "",
    val gpsLocation: String? = null,
    val isWarrantyEnabled: Boolean = false,
    val warrantyExpirationMillis: Long = System.currentTimeMillis() + 31536000000L,
    val transactionStatus: TransactionStatus = TransactionStatus.CLEARED,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val tags: Set<String> = emptySet(),

    // UI State
    val showAccountSheet: Boolean = false,
    val showTargetAccountSheet: Boolean = false,
    val showCategorySheet: Boolean = false,
    val showPaymentMethodSheet: Boolean = false,
    val showStatusSheet: Boolean = false,
    val showTagSheet: Boolean = false,
    val showCurrencySheet: Boolean = false,
    val showTargetCurrencySheet: Boolean = false,
    val showExchangeRateDialog: Boolean = false,
    val showWarrantyDatePicker: Boolean = false,
    val showLocationSheet: Boolean = false,
    val showLinkedTransactionSheet: Boolean = false,
    val isNumpadVisible: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val isLinkedToPlannedPayment: Boolean = false,

    // External data
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val allTags: List<Tag> = emptyList(),
    val uniquePayees: List<String> = emptyList(),
    val uniqueNotes: List<String> = emptyList(),
    val shortlistedCurrencies: List<String> = emptyList(),

    // Metadata
    val templateId: Long? = null,
    val isEditMode: Boolean = false,
    val navTriggered: Boolean = false,
    val prefillDto: TransactionPrefillDTO? = null,
    val displayLinkedTxs: List<Transaction> = emptyList()
) {
    // Legacy mapping
    val accountId: String? get() = source.account.toStored()
    val targetAccountId: String? get() = destination.account.toStored()
    val amount: String get() = source.amount
    val targetAmount: String get() = destination.amount
    val categoryId: Long get() = source.categoryId
    val currency: String get() = source.currency
    val targetCurrency: String get() = destination.currency

    val selectedAccount: Account? get() = source.account.resolve(accounts)
    val selectedTargetAccount: Account? get() = destination.account.resolve(accounts)
    val selectedCategory: Category? get() = categories.find { it.id == categoryId }

    val isIncome: Boolean get() = transactionType == TransactionType.INCOME
    val isExpense: Boolean get() = transactionType == TransactionType.EXPENSE
    val isTransfer: Boolean get() = transactionType == TransactionType.TRANSFER
    val isTransferInflow: Boolean get() = isTransfer && Category.isAnyTransferInflow(categoryId)

    val amountValue: BigDecimal
        get() = source.amountValue

    val errors: Set<EntryField> get() = buildSet {
        if (name.isBlank()) add(EntryField.NAME)
        // Templates can have empty amounts or categories as they are partially filled
    }

    val hasError: Boolean get() = errors.isNotEmpty()

    val canSave: Boolean
        get() = !hasError && !navTriggered
}
