package com.osscabinet.ossWallet.ui.screens.plannedpayments.entry

import com.osscabinet.ossWallet.domain.model.AmountEditingSide
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
import com.osscabinet.ossWallet.domain.model.RecurrenceType
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import java.math.BigDecimal

sealed class PlannedPaymentEntryEvent {
    data class SetName(val name: String) : PlannedPaymentEntryEvent()
    data class SetAmountText(val amount: String) : PlannedPaymentEntryEvent()
    data class SetTargetAmount(val amount: String) : PlannedPaymentEntryEvent()
    data class SetCurrency(val currency: String) : PlannedPaymentEntryEvent()
    data class SetTransactionType(val type: TransactionType) : PlannedPaymentEntryEvent()
    data class SetAccountId(val id: String?) : PlannedPaymentEntryEvent()
    data class SetTargetAccountId(val id: String?) : PlannedPaymentEntryEvent()
    data class SetTargetCurrency(val currency: String?) : PlannedPaymentEntryEvent()
    data class SetTransferRate(val rate: BigDecimal) : PlannedPaymentEntryEvent()
    data object SwapAccounts : PlannedPaymentEntryEvent()
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
    data class SetLocation(val gpsLocation: String?, val location: String? = null) :
        PlannedPaymentEntryEvent()

    data class SetWarrantyEnabled(val enabled: Boolean) : PlannedPaymentEntryEvent()
    data class SetWarrantyExpiration(val millis: Long) : PlannedPaymentEntryEvent()
    data class SetTransactionStatus(val status: TransactionStatus) : PlannedPaymentEntryEvent()
    data class SetPaymentMethod(val method: PaymentMethod) : PlannedPaymentEntryEvent()
    data class SetTags(val tags: Set<String>) : PlannedPaymentEntryEvent()
    data class SetStatus(val status: PlannedPaymentStatus) : PlannedPaymentEntryEvent()
    data class SetLinkedTemplate(val template: Template?) : PlannedPaymentEntryEvent()
    data class SetDeleteTemplateWithPayment(val delete: Boolean) : PlannedPaymentEntryEvent()
    data class SetLinkedTransactionIds(val ids: List<Long>) : PlannedPaymentEntryEvent()
    data class RemoveLinkedTransaction(val currentId: Long, val targetId: Long) :
        PlannedPaymentEntryEvent()

    // Visibility
    data class ShowScheduleBuilder(val show: Boolean) : PlannedPaymentEntryEvent()
    data class ShowEndDatePicker(val show: Boolean) : PlannedPaymentEntryEvent()
    data class ShowCategorySheet(val show: Boolean) : PlannedPaymentEntryEvent()
    data class ShowAccountSheet(val show: Boolean) : PlannedPaymentEntryEvent()
    data class ShowTargetAccountSheet(val show: Boolean) : PlannedPaymentEntryEvent()
    data class ShowPaymentMethodSheet(val show: Boolean) : PlannedPaymentEntryEvent()
    data class ShowTagSheet(val show: Boolean) : PlannedPaymentEntryEvent()
    data class ShowStatusSheet(val show: Boolean) : PlannedPaymentEntryEvent()
    data class ShowTransactionStatusSheet(val show: Boolean) : PlannedPaymentEntryEvent()
    data class ShowWarrantyDatePicker(val show: Boolean) : PlannedPaymentEntryEvent()
    data class ShowLocationSheet(val show: Boolean) : PlannedPaymentEntryEvent()
    data class ShowNumpad(val show: Boolean, val side: AmountEditingSide = AmountEditingSide.SOURCE) : PlannedPaymentEntryEvent()
    data class ShowCurrencySheet(val show: Boolean) : PlannedPaymentEntryEvent()
    data class ShowTargetCurrencySheet(val show: Boolean) : PlannedPaymentEntryEvent()
    data class ShowTemplateSheet(val show: Boolean) : PlannedPaymentEntryEvent()
    data class ShowExchangeRateDialog(val show: Boolean) : PlannedPaymentEntryEvent()
    data class ShowLinkedTransactionSheet(val show: Boolean) : PlannedPaymentEntryEvent()
    data class ShowDeleteDialog(val show: Boolean) : PlannedPaymentEntryEvent()

    data object TriggerNavigation : PlannedPaymentEntryEvent()
}
