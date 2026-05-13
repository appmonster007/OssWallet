package com.osscabinet.ossWallet.ui.screens.plannedpayments.entry

import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.domain.model.dto.PlannedPaymentPrefillDTO
import java.math.BigDecimal

data class PlannedPaymentEntryState(
    val title: String = "",
    val amountText: String = "",
    val currency: String = "USD",
    val transactionType: Int = 0,
    val selectedAccountId: String = "",
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
    val paymentMethod: String = "Cash",
    val selectedTagNames: List<String> = emptyList(),
    val status: PlannedPaymentStatus = PlannedPaymentStatus.ACTIVE,
    val linkedTemplate: Template? = null,

    // UI State
    val showScheduleBuilder: Boolean = false,
    val showEndDatePicker: Boolean = false,
    val showCategorySheet: Boolean = false,
    val showAccountSheet: Boolean = false,
    val showTargetAccountSheet: Boolean = false,
    val showPaymentMethodSheet: Boolean = false,
    val showTagSheet: Boolean = false,
    val showStatusSheet: Boolean = false,
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
    val existingPayment: PlannedPayment? = null
) {
    val canSave: Boolean
        get() = title.isNotBlank() && (amountText.toBigDecimalOrNull()?.signum() == 1)
}

sealed class PlannedPaymentEntryEvent {
    data class SetTitle(val title: String) : PlannedPaymentEntryEvent()
    data class SetAmountText(val amount: String) : PlannedPaymentEntryEvent()
    data class SetCurrency(val currency: String) : PlannedPaymentEntryEvent()
    data class SetTransactionType(val type: Int) : PlannedPaymentEntryEvent()
    data class SetAccountId(val id: String) : PlannedPaymentEntryEvent()
    data class SetTargetAccountId(val id: String?) : PlannedPaymentEntryEvent()
    data class SetTargetCurrency(val currency: String?) : PlannedPaymentEntryEvent()
    data class SetTransferRate(val rate: BigDecimal) : PlannedPaymentEntryEvent()
    data class SetRecurrence(
        val startDate: Long,
        val type: RecurrenceType,
        val interval: Int,
        val dayOfWeek: Int?,
        val dayOfMonth: Int?,
        val cronExpression: String?
    ) : PlannedPaymentEntryEvent()
    data class SetAutoConfirm(val auto: Boolean) : PlannedPaymentEntryEvent()
    data class SetHasEndDate(val has: Boolean) : PlannedPaymentEntryEvent()
    data class SetEndDate(val date: Long) : PlannedPaymentEntryEvent()
    data class SetCategoryId(val id: Long) : PlannedPaymentEntryEvent()
    data class SetPayee(val payee: String) : PlannedPaymentEntryEvent()
    data class SetNote(val note: String) : PlannedPaymentEntryEvent()
    data class SetPaymentMethod(val method: String) : PlannedPaymentEntryEvent()
    data class SetTags(val tags: List<String>) : PlannedPaymentEntryEvent()
    data class SetStatus(val status: PlannedPaymentStatus) : PlannedPaymentEntryEvent()
    data class SetLinkedTemplate(val template: Template?) : PlannedPaymentEntryEvent()
    data class SetDeleteTemplateWithPayment(val delete: Boolean) : PlannedPaymentEntryEvent()

    // Visibility
    data class ShowScheduleBuilder(val show: Boolean) : PlannedPaymentEntryEvent()
    data class ShowEndDatePicker(val show: Boolean) : PlannedPaymentEntryEvent()
    data class ShowCategorySheet(val show: Boolean) : PlannedPaymentEntryEvent()
    data class ShowAccountSheet(val show: Boolean) : PlannedPaymentEntryEvent()
    data class ShowTargetAccountSheet(val show: Boolean) : PlannedPaymentEntryEvent()
    data class ShowPaymentMethodSheet(val show: Boolean) : PlannedPaymentEntryEvent()
    data class ShowTagSheet(val show: Boolean) : PlannedPaymentEntryEvent()
    data class ShowStatusSheet(val show: Boolean) : PlannedPaymentEntryEvent()
    data class ShowNumpad(val show: Boolean) : PlannedPaymentEntryEvent()
    data class ShowCurrencySheet(val show: Boolean) : PlannedPaymentEntryEvent()
    data class ShowTargetCurrencySheet(val show: Boolean) : PlannedPaymentEntryEvent()
    data class ShowTemplateSheet(val show: Boolean) : PlannedPaymentEntryEvent()
    data class ShowExchangeRateDialog(val show: Boolean) : PlannedPaymentEntryEvent()
    data class ShowDeleteDialog(val show: Boolean) : PlannedPaymentEntryEvent()

    data object TriggerNavigation : PlannedPaymentEntryEvent()
}
