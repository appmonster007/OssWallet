package com.osscabinet.ossWallet.ui.screens.templates.entry

import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import java.math.BigDecimal

sealed class TemplateEntryEvent {
    data class SetName(val name: String) : TemplateEntryEvent()
    data class SetAmount(val amount: String) : TemplateEntryEvent()
    data class SetTransactionType(val type: TransactionType) : TemplateEntryEvent()
    data class SetCategory(val categoryId: Long) : TemplateEntryEvent()
    data class SetAccount(val accountId: String?) : TemplateEntryEvent()
    data class SetTargetAccount(val accountId: String?) : TemplateEntryEvent()
    data class SetCurrency(val currency: String) : TemplateEntryEvent()
    data class SetTargetCurrency(val currency: String?) : TemplateEntryEvent()
    data class SetTransferRate(val rate: BigDecimal) : TemplateEntryEvent()
    data object SwapAccounts : TemplateEntryEvent()
    data class SetNote(val note: String) : TemplateEntryEvent()
    data class SetPayee(val payee: String) : TemplateEntryEvent()
    data class SetLocation(val gpsLocation: String?, val location: String? = null) :
        TemplateEntryEvent()

    data class SetWarrantyEnabled(val enabled: Boolean) : TemplateEntryEvent()
    data class SetWarrantyExpiration(val millis: Long) : TemplateEntryEvent()
    data class SetTransactionStatus(val status: TransactionStatus) : TemplateEntryEvent()
    data class SetPaymentMethod(val method: PaymentMethod) : TemplateEntryEvent()
    data class SetTags(val tags: Set<String>) : TemplateEntryEvent()
    data class AddTag(val name: String, val color: Int? = null) : TemplateEntryEvent()
    data class UpdateTag(val tag: Tag) : TemplateEntryEvent()
    data class DeleteTag(val tag: Tag) : TemplateEntryEvent()
    data class SetLinkedTransactionIds(val ids: List<Long>) : TemplateEntryEvent()
    data class RemoveLinkedTransaction(val currentId: Long, val targetId: Long) :
        TemplateEntryEvent()

    // Sheet visibility
    data class ShowAccountSheet(val show: Boolean) : TemplateEntryEvent()
    data class ShowTargetAccountSheet(val show: Boolean) : TemplateEntryEvent()
    data class ShowCategorySheet(val show: Boolean) : TemplateEntryEvent()
    data class ShowPaymentMethodSheet(val show: Boolean) : TemplateEntryEvent()
    data class ShowTagSheet(val show: Boolean) : TemplateEntryEvent()
    data class ShowCurrencySheet(val show: Boolean) : TemplateEntryEvent()
    data class ShowTargetCurrencySheet(val show: Boolean) : TemplateEntryEvent()
    data class ShowExchangeRateDialog(val show: Boolean) : TemplateEntryEvent()
    data class ShowWarrantyDatePicker(val show: Boolean) : TemplateEntryEvent()
    data class ShowLocationSheet(val show: Boolean) : TemplateEntryEvent()
    data class ShowStatusSheet(val show: Boolean) : TemplateEntryEvent()
    data class ShowNumpad(val show: Boolean) : TemplateEntryEvent()
    data class ShowLinkedTransactionSheet(val show: Boolean) : TemplateEntryEvent()
    data class ShowDeleteDialog(val show: Boolean) : TemplateEntryEvent()

    // Navigation
    data object TriggerNavigation : TemplateEntryEvent()
}
