package com.osscabinet.ossWallet.ui.screens.filters.entry

import com.osscabinet.ossWallet.domain.model.TransactionType

sealed class FilterEntryEvent {
    data class SetName(val name: String) : FilterEntryEvent()
    data class SetTransactionTypes(val types: Set<TransactionType>) : FilterEntryEvent()
    data class SetAccountIds(val ids: Set<String>) : FilterEntryEvent()
    data class SetCategoryIds(val ids: Set<Long>) : FilterEntryEvent()
    data class SetPaymentMethods(val methods: Set<com.osscabinet.ossWallet.domain.model.PaymentMethod>) : FilterEntryEvent()
    data class SetStatuses(val statuses: Set<com.osscabinet.ossWallet.domain.model.TransactionStatus>) : FilterEntryEvent()
    data class SetLinkedTransactions(val ids: Set<Long>) : FilterEntryEvent()
    data class SetTags(val tags: Set<String>) : FilterEntryEvent()
    data class SetCurrencies(val currencies: Set<String>) : FilterEntryEvent()
    data class SetQueryNote(val query: String) : FilterEntryEvent()
    data class SetNoteRegex(val isRegex: Boolean) : FilterEntryEvent()
    data class SetQueryPayee(val query: String) : FilterEntryEvent()
    data class SetPayeeRegex(val isRegex: Boolean) : FilterEntryEvent()
    data class SetMinAmount(val amount: String) : FilterEntryEvent()
    data class SetMaxAmount(val amount: String) : FilterEntryEvent()
    data class SetHasWarranty(val hasWarranty: Boolean?) : FilterEntryEvent()
    data class SetWarrantyExpirationStart(val date: Long?) : FilterEntryEvent()
    data class SetWarrantyExpirationEnd(val date: Long?) : FilterEntryEvent()
    data class SetExcludeFuture(val exclude: Boolean) : FilterEntryEvent()
    
    // Visibility
    data class ShowAccountSheet(val show: Boolean) : FilterEntryEvent()
    data class ShowCategorySheet(val show: Boolean) : FilterEntryEvent()
    data class ShowPaymentMethodSheet(val show: Boolean) : FilterEntryEvent()
    data class ShowStatusSheet(val show: Boolean) : FilterEntryEvent()
    data class ShowLinkSheet(val show: Boolean) : FilterEntryEvent()
    data class ShowTagSheet(val show: Boolean) : FilterEntryEvent()
    data class ShowCurrencySheet(val show: Boolean) : FilterEntryEvent()
    data class ShowTypeSheet(val show: Boolean) : FilterEntryEvent()
    data class ShowMinAmountSheet(val show: Boolean) : FilterEntryEvent()
    data class ShowMaxAmountSheet(val show: Boolean) : FilterEntryEvent()
    data class ShowWarrantyStartPicker(val show: Boolean) : FilterEntryEvent()
    data class ShowWarrantyEndPicker(val show: Boolean) : FilterEntryEvent()
    
    data object TriggerNavigation : FilterEntryEvent()
    data object ResetFilter : FilterEntryEvent()
}
