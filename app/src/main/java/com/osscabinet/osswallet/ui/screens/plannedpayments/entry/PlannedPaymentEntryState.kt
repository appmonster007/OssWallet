package com.osscabinet.ossWallet.ui.screens.plannedpayments.entry

import com.osscabinet.ossWallet.domain.model.*
import java.math.BigDecimal

data class PlannedPaymentEntryState(
    val name: String = "",
    val amount: String = "",
    val currency: String = "USD",
    val transactionType: Int = 0,
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
    val selectedCategoryId: Long = 1002L,
    val payee: String = "",
    val noteUserPart: String = "",
    val location: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isWarrantyEnabled: Boolean = false,
    val warrantyExpirationMillis: Long = System.currentTimeMillis() + 31536000000L,
    val transactionStatus: TransactionStatus = TransactionStatus.CLEARED,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val selectedTagNames: List<String> = emptyList(),
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
    val allTransactions: List<Transaction> = emptyList(),
    val use24HourFormat: Boolean = false
) {
    val canSave: Boolean
        get() = name.isNotBlank() && (amount.toBigDecimalOrNull()?.signum() == 1)
}
