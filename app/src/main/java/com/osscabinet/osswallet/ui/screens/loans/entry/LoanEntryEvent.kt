package com.osscabinet.ossWallet.ui.screens.loans.entry

import com.osscabinet.ossWallet.domain.model.LoanType

sealed class LoanEntryEvent {
    data class SetName(val name: String) : LoanEntryEvent()
    data class SetPrincipalAmount(val amount: String) : LoanEntryEvent()
    data class SetCurrency(val currency: String) : LoanEntryEvent()
    data class SetLoanType(val type: LoanType) : LoanEntryEvent()
    data class SetCounterparty(val counterparty: String) : LoanEntryEvent()
    data class SetInterestRate(val rate: String) : LoanEntryEvent()
    data class SetNote(val note: String) : LoanEntryEvent()
    data class SetCategoryIds(val ids: Set<Long>) : LoanEntryEvent()
    data class SetTags(val tags: Set<String>) : LoanEntryEvent()
    data class SetPaymentMethods(val methods: Set<com.osscabinet.ossWallet.domain.model.PaymentMethod>) : LoanEntryEvent()
    data class SetStatuses(val statuses: Set<com.osscabinet.ossWallet.domain.model.TransactionStatus>) : LoanEntryEvent()
    data class SetLinkedTransactions(val ids: Set<Long>) : LoanEntryEvent()
    data class SetStartDate(val date: Long) : LoanEntryEvent()
    data class SetHasSettlementDate(val has: Boolean) : LoanEntryEvent()
    data class SetSettlementDate(val date: Long) : LoanEntryEvent()
    data class SetLinkedAccountId(val id: String) : LoanEntryEvent()
    
    // Visibility
    data class ShowStartDatePicker(val show: Boolean) : LoanEntryEvent()
    data class ShowSettlementDatePicker(val show: Boolean) : LoanEntryEvent()
    data class ShowNumpad(val show: Boolean) : LoanEntryEvent()
    data class ShowInterestNumpad(val show: Boolean) : LoanEntryEvent()
    data class ShowAccountSheet(val show: Boolean) : LoanEntryEvent()
    data class ShowCategorySheet(val show: Boolean) : LoanEntryEvent()
    data class ShowTagSheet(val show: Boolean) : LoanEntryEvent()
    data class ShowPaymentMethodSheet(val show: Boolean) : LoanEntryEvent()
    data class ShowStatusSheet(val show: Boolean) : LoanEntryEvent()
    data class ShowLinkSheet(val show: Boolean) : LoanEntryEvent()
    
    data class SetAccountQuery(val query: String) : LoanEntryEvent()
    data class SetCategoryQuery(val query: String) : LoanEntryEvent()
    
    data object TriggerNavigation : LoanEntryEvent()
}
