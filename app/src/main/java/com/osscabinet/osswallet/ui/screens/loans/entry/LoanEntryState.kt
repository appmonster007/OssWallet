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
