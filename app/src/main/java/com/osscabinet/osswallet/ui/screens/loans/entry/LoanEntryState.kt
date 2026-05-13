package com.osscabinet.ossWallet.ui.screens.loans.entry

import com.osscabinet.ossWallet.domain.model.*

data class LoanEntryState(
    val name: String = "",
    val principalAmount: String = "",
    val currency: String = "USD",
    val loanType: LoanType = LoanType.BORROWED,
    val counterparty: String = "",
    val interestRate: String = "",
    val note: String = "",
    val selectedCategoryIds: Set<Long> = emptySet(),
    val selectedTags: Set<String> = emptySet(),
    val startDate: Long = System.currentTimeMillis(),
    val hasSettlementDate: Boolean = false,
    val settlementDate: Long = System.currentTimeMillis(),
    val linkedAccountId: String = "",

    // UI State
    val showStartDatePicker: Boolean = false,
    val showSettlementDatePicker: Boolean = false,
    val showNumpad: Boolean = false,
    val showInterestNumpad: Boolean = false,
    val showAccountSheet: Boolean = false,
    val showCategorySheet: Boolean = false,
    val showTagSheet: Boolean = false,
    val accountQuery: String = "",
    val categoryQuery: String = "",
    val isEditMode: Boolean = false,
    val loanId: Long? = null,
    val navTriggered: Boolean = false,

    // External data
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val availableTags: List<String> = emptyList(),
    val uniquePayees: List<String> = emptyList(),
    val uniqueNotes: List<String> = emptyList(),
    val currentProfileId: Int = 1,
    val existingLoan: Loan? = null
) {
    val canSave: Boolean
        get() = name.isNotBlank() && 
                 principalAmount.toBigDecimalOrNull() != null && 
                 selectedCategoryIds.isNotEmpty()
}

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
    
    data class SetAccountQuery(val query: String) : LoanEntryEvent()
    data class SetCategoryQuery(val query: String) : LoanEntryEvent()
    
    data object TriggerNavigation : LoanEntryEvent()
}
