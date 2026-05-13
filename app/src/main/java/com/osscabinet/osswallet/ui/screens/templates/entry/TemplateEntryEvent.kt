package com.osscabinet.ossWallet.ui.screens.templates.entry

import com.osscabinet.ossWallet.domain.model.Tag
import java.math.BigDecimal

sealed class TemplateEntryEvent {
    data class SetName(val name: String) : TemplateEntryEvent()
    data class SetAmount(val amount: String) : TemplateEntryEvent()
    data class SetTransactionType(val type: Int) : TemplateEntryEvent()
    data class SetCategory(val categoryId: Long) : TemplateEntryEvent()
    data class SetAccount(val accountId: String?) : TemplateEntryEvent()
    data class SetTargetAccount(val accountId: String?) : TemplateEntryEvent()
    data class SetCurrency(val currency: String) : TemplateEntryEvent()
    data class SetTargetCurrency(val currency: String?) : TemplateEntryEvent()
    data class SetTransferRate(val rate: BigDecimal) : TemplateEntryEvent()
    data class SetNote(val note: String) : TemplateEntryEvent()
    data class SetPayee(val payee: String) : TemplateEntryEvent()
    data class SetLocation(val location: String) : TemplateEntryEvent()
    data class SetPaymentMethod(val method: String) : TemplateEntryEvent()
    data class SetTags(val tags: List<String>) : TemplateEntryEvent()
    data class AddTag(val name: String, val color: Int? = null) : TemplateEntryEvent()
    data class UpdateTag(val tag: Tag) : TemplateEntryEvent()
    data class DeleteTag(val tag: Tag) : TemplateEntryEvent()

    // Sheet visibility
    data class ShowAccountSheet(val show: Boolean) : TemplateEntryEvent()
    data class ShowTargetAccountSheet(val show: Boolean) : TemplateEntryEvent()
    data class ShowCategorySheet(val show: Boolean) : TemplateEntryEvent()
    data class ShowPaymentMethodSheet(val show: Boolean) : TemplateEntryEvent()
    data class ShowTagSheet(val show: Boolean) : TemplateEntryEvent()
    data class ShowCurrencySheet(val show: Boolean) : TemplateEntryEvent()
    data class ShowTargetCurrencySheet(val show: Boolean) : TemplateEntryEvent()
    data class ShowExchangeRateDialog(val show: Boolean) : TemplateEntryEvent()
    data class ShowNumpad(val show: Boolean) : TemplateEntryEvent()

    // Navigation
    data object TriggerNavigation : TemplateEntryEvent()
}
