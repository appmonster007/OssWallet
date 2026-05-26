package com.osscabinet.ossWallet.ui.screens.plannedpayments.entry

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
import com.osscabinet.ossWallet.domain.model.RecurrenceType
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import java.math.BigDecimal

data class PlannedPaymentEntryState(
    val name: String = "",
    val amount: String = "",
    val currency: String = "USD",
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val selectedAccountId: String? = null,
    val targetAccountId: String? = null,
    val targetCurrency: String? = null,
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
    val selectedCategoryId: Long = Category.SYSTEM_UNKNOWN,
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
    val isIncome: Boolean get() = transactionType == TransactionType.INCOME
    val isExpense: Boolean get() = transactionType == TransactionType.EXPENSE
    val isTransfer: Boolean get() = transactionType == TransactionType.TRANSFER
    val isTransferInflow: Boolean get() = isTransfer && selectedCategoryId == Category.SYSTEM_TRANSFER_INFLOW

    val amountValue: BigDecimal
        get() = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO

    val canSave: Boolean
        get() = name.isNotBlank()
}
