package com.osscabinet.ossWallet.ui.screens.plannedpayments.entry

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountRef
import com.osscabinet.ossWallet.domain.model.AmountEditingSide
import com.osscabinet.ossWallet.domain.model.resolve
import com.osscabinet.ossWallet.domain.model.toStored

import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
import com.osscabinet.ossWallet.domain.model.RecurrenceType
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionSide
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.ui.components.entry.EntryField
import java.math.BigDecimal

data class PlannedPaymentEntryState(
    val name: String = "",
    val source: TransactionSide = TransactionSide(currency = "USD"),
    val destination: TransactionSide = TransactionSide(),
    val activeAmountSide: AmountEditingSide = AmountEditingSide.SOURCE,
    val entryMode: EntryMode = EntryMode.EXPENSE,
    val transferRate: BigDecimal = BigDecimal.ONE,
    val recurrenceType: RecurrenceType = RecurrenceType.MONTHLY,
    val recurrenceInterval: Int = 1,
    val dayOfWeek: Int? = null,
    val dayOfMonth: Int? = null,
    val cronExpression: String? = null,
    val autoConfirm: Boolean = false,
    val startDateMillis: Long = System.currentTimeMillis(),
    val hasEndDate: Boolean = false,
    val endDateMillis: Long = System.currentTimeMillis(),
    val payee: String = "",
    val noteUserPart: String = "",
    val location: String = "",
    val gpsLocation: String? = null,
    val isWarrantyEnabled: Boolean = false,
    val warrantyExpirationMillis: Long = System.currentTimeMillis() + 31536000000L,
    val transactionStatus: TransactionStatus = TransactionStatus.CLEARED,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val selectedTagNames: Set<String> = emptySet(),
    val status: PlannedPaymentStatus = PlannedPaymentStatus.ACTIVE,
    val linkedTemplate: Template? = null,
    val templateUsageCount: Int = 0,

    // UI State
    val showScheduleBuilder: Boolean = false,
    val showEndDatePicker: Boolean = false,
    val showCategorySheet: Boolean = false,
    val showAccountSheet: Boolean = false,
    val showTargetAccountSheet: Boolean = false,
    val showPaymentMethodSheet: Boolean = false,
    val showTagSheet: Boolean = false,
    val showStatusSheet: Boolean = false,
    val showTransactionStatusSheet: Boolean = false,
    val showWarrantyDatePicker: Boolean = false,
    val showLocationSheet: Boolean = false,
    val showLinkedTransactionSheet: Boolean = false,
    val showNumpad: Boolean = false,
    val showCurrencySheet: Boolean = false,
    val showTargetCurrencySheet: Boolean = false,
    val showTemplateSheet: Boolean = false,
    val showExchangeRateDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val deleteTemplateWithPayment: Boolean = false,

    val isEditMode: Boolean = false,
    val plannedPaymentId: Long? = null,
    val navTriggered: Boolean = false,

    // External data
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val templates: List<Template> = emptyList(),
    val availableTags: List<Tag> = emptyList(),
    val uniquePayees: List<String> = emptyList(),
    val uniqueNotes: List<String> = emptyList(),
    val currentProfileId: Int = 1,
    val existingPayment: PlannedPayment? = null,
    val displayLinkedTxs: List<Transaction> = emptyList(),
    val shortlistedCurrencies: List<String> = emptyList()
) {
    // Legacy mapping
    val amount: String get() = source.amount
    val targetAmount: String get() = destination.amount
    val currency: String get() = source.currency
    val selectedAccountId: String? get() = source.account.toStored()
    val targetAccountId: String? get() = destination.account.toStored()
    val targetCurrency: String get() = destination.currency
    val selectedCategoryId: Long get() = source.categoryId

    val selectedAccount: Account? get() = source.account.resolve(accounts)
    val targetAccount: Account? get() = destination.account.resolve(accounts)

    val isIncome: Boolean get() = entryMode == EntryMode.INCOME
    val isExpense: Boolean get() = entryMode == EntryMode.EXPENSE
    val isTransfer: Boolean get() = entryMode == EntryMode.TRANSFER

    val amountValue: BigDecimal
        get() = source.amountValue

    val hasError: Boolean get() = errors.isNotEmpty()

    val canSave: Boolean
        get() = !hasError && !navTriggered

    // Grouped validation state
    val errors: Set<EntryField>
        get() = buildSet {
            if (name.isBlank()) add(EntryField.NAME)
            if (amountValue <= BigDecimal.ZERO) add(EntryField.AMOUNT)
            if (selectedCategoryId <= 0) add(EntryField.CATEGORY)
            if (isTransfer) {
                if (selectedAccountId.isNullOrBlank()) add(EntryField.ACCOUNT)
                if (targetAccountId.isNullOrBlank()) add(EntryField.TARGET_ACCOUNT)
            } else {
                if (selectedAccountId.isNullOrBlank()) add(EntryField.ACCOUNT)
            }
        }

    fun hasError(field: EntryField) = errors.contains(field)
}
