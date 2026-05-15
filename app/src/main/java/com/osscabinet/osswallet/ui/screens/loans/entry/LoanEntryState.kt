package com.osscabinet.ossWallet.ui.screens.loans.entry

import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange

data class LoanEntryState(
    val name: String = "",
    val principalAmount: String = "",
    val currency: String = "USD",
    val loanType: LoanType = LoanType.BORROWED,
    val counterparty: String = "",
    val interestRate: String = "",
    val note: String = "",
    val queryNote: String = "",
    val isNoteRegex: Boolean = false,
    val queryPayee: String = "",
    val isPayeeRegex: Boolean = false,
    val minAmount: String = "",
    val maxAmount: String = "",
    val hasWarranty: Boolean? = null,
    val warrantyExpirationStart: Long? = null,
    val warrantyExpirationEnd: Long? = null,
    val excludeFuture: Boolean = true,
    val selectedTrackTypes: Set<TransactionType> = setOf(TransactionType.EXPENSE, TransactionType.INCOME),
    val selectedCategoryIds: Set<Long> = emptySet(),
    val selectedCurrencies: Set<String> = emptySet(),
    val selectedTags: Set<String> = emptySet(),
    val selectedPaymentMethods: Set<PaymentMethod> = emptySet(),
    val selectedStatuses: Set<TransactionStatus> = emptySet(),
    val linkedTransactionIds: Set<Long> = emptySet(),
    val timelineRange: TimelineRange = TimelineRange.AllTime,
    val startDate: Long = System.currentTimeMillis(),
    val hasSettlementDate: Boolean = false,
    val settlementDate: Long = System.currentTimeMillis(),
    val linkedAccountId: String = "",
    val filterId: Long? = null,
    val linkedFilter: Filter? = null,
    val filterUsageCount: Int = 0,
    val showSharedFilterDialog: Boolean = false,
    val deleteFilterWithLoan: Boolean = false,
    val showDeleteDialog: Boolean = false,

    // UI State
    val showStartDatePicker: Boolean = false,
    val showSettlementDatePicker: Boolean = false,
    val showNumpad: Boolean = false,
    val showInterestNumpad: Boolean = false,
    val showTimelineSheet: Boolean = false,
    val showAccountSheet: Boolean = false,
    val showTrackTypeSheet: Boolean = false,
    val showCategorySheet: Boolean = false,
    val showTagSheet: Boolean = false,
    val showCurrencyFilterSheet: Boolean = false,
    val showPaymentMethodSheet: Boolean = false,
    val showStatusSheet: Boolean = false,
    val showLinkSheet: Boolean = false,
    val showFilterSheet: Boolean = false,
    val showMinAmountSheet: Boolean = false,
    val showMaxAmountSheet: Boolean = false,
    val showWarrantyStartPicker: Boolean = false,
    val showWarrantyEndPicker: Boolean = false,
    val accountQuery: String = "",
    val categoryQuery: String = "",
    val isEditMode: Boolean = false,
    val loanId: Long? = null,
    val navTriggered: Boolean = false,

    // External data
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val allTransactions: List<Transaction> = emptyList(),
    val availableTags: List<Tag> = emptyList(),
    val uniquePayees: List<String> = emptyList(),
    val uniqueNotes: List<String> = emptyList(),
    val currentProfileId: Int = 1,
    val existingLoan: Loan? = null,
    val availableFilters: List<com.osscabinet.ossWallet.domain.model.Filter> = emptyList()
) {
    val canSave: Boolean
        get() = name.isNotBlank() && 
                 principalAmount.toBigDecimalOrNull() != null &&
                 !navTriggered &&
                 linkedAccountId.isNotBlank()
}
