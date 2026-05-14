package com.osscabinet.ossWallet.ui.screens.budgets.entry

import com.osscabinet.ossWallet.domain.model.*
import java.math.BigDecimal

data class BudgetEntryState(
    val name: String = "",
    val limitAmount: String = "",
    val currency: String = "USD",
    val period: BudgetPeriod = BudgetPeriod.MONTHLY,
    val selectedTrackTypes: Set<TransactionType> = setOf(TransactionType.EXPENSE),
    val alertThreshold: Float = 0.8f,
    val rollover: Boolean = false,
    val note: String = "",
    val selectedCategoryIds: Set<Long> = emptySet(),
    val selectedAccountIds: Set<String> = emptySet(),
    val selectedTags: Set<String> = emptySet(),
    val selectedPaymentMethods: Set<PaymentMethod> = emptySet(),
    val selectedStatuses: Set<TransactionStatus> = emptySet(),
    val linkedTransactionIds: Set<Long> = emptySet(),
    val customStartDate: Long = System.currentTimeMillis(),
    val customEndDate: Long = System.currentTimeMillis(),
    val filterId: Long? = null,
    val linkedFilter: Filter? = null,
    val filterUsageCount: Int = 0,
    val deleteFilterWithBudget: Boolean = false,
    val showDeleteDialog: Boolean = false,

    // UI State
    val showCustomStartPicker: Boolean = false,
    val showCustomEndPicker: Boolean = false,
    val showNumpad: Boolean = false,
    val showAccountSheet: Boolean = false,
    val showTrackTypeSheet: Boolean = false,
    val showCategorySheet: Boolean = false,
    val showTagSheet: Boolean = false,
    val showPeriodSheet: Boolean = false,
    val showPaymentMethodSheet: Boolean = false,
    val showStatusSheet: Boolean = false,
    val showLinkSheet: Boolean = false,
    val showFilterSheet: Boolean = false,
    val accountQuery: String = "",
    val categoryQuery: String = "",
    val isEditMode: Boolean = false,
    val budgetId: Long? = null,
    val navTriggered: Boolean = false,

    // External data
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val allTransactions: List<Transaction> = emptyList(),
    val availableTags: List<Tag> = emptyList(),
    val uniqueNotes: List<String> = emptyList(),
    val currentProfileId: Int = 1,
    val existingBudget: Budget? = null,
    val availableFilters: List<com.osscabinet.ossWallet.domain.model.Filter> = emptyList()
) {
    val canSave: Boolean
        get() = name.isNotBlank() && 
                 limitAmount.toBigDecimalOrNull() != null
}
