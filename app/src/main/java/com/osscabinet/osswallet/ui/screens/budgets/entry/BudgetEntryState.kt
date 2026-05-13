package com.osscabinet.ossWallet.ui.screens.budgets.entry

import com.osscabinet.ossWallet.domain.model.*
import java.math.BigDecimal

data class BudgetEntryState(
    val name: String = "",
    val limitAmount: String = "",
    val currency: String = "USD",
    val period: BudgetPeriod = BudgetPeriod.MONTHLY,
    val selectedTrackTypes: Set<BudgetTrackType> = setOf(BudgetTrackType.EXPENSE),
    val alertThreshold: Float = 0.8f,
    val rollover: Boolean = false,
    val note: String = "",
    val selectedCategoryIds: Set<Long> = emptySet(),
    val selectedAccountIds: Set<String> = emptySet(),
    val selectedTags: Set<String> = emptySet(),
    val customStartDate: Long = System.currentTimeMillis(),
    val customEndDate: Long = System.currentTimeMillis(),

    // UI State
    val showCustomStartPicker: Boolean = false,
    val showCustomEndPicker: Boolean = false,
    val showNumpad: Boolean = false,
    val showAccountSheet: Boolean = false,
    val showTrackTypeSheet: Boolean = false,
    val showCategorySheet: Boolean = false,
    val showTagSheet: Boolean = false,
    val showPeriodSheet: Boolean = false,
    val accountQuery: String = "",
    val categoryQuery: String = "",
    val isEditMode: Boolean = false,
    val budgetId: Long? = null,
    val navTriggered: Boolean = false,

    // External data
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val availableTags: List<Tag> = emptyList(),
    val uniqueNotes: List<String> = emptyList(),
    val currentProfileId: Int = 1,
    val existingBudget: Budget? = null
) {
    val canSave: Boolean
        get() = name.isNotBlank() && 
                 limitAmount.toBigDecimalOrNull() != null && 
                 selectedCategoryIds.isNotEmpty()
}

sealed class BudgetEntryEvent {
    data class SetName(val name: String) : BudgetEntryEvent()
    data class SetLimitAmount(val amount: String) : BudgetEntryEvent()
    data class SetCurrency(val currency: String) : BudgetEntryEvent()
    data class SetPeriod(val period: BudgetPeriod) : BudgetEntryEvent()
    data class SetTrackTypes(val types: Set<BudgetTrackType>) : BudgetEntryEvent()
    data class SetAlertThreshold(val threshold: Float) : BudgetEntryEvent()
    data class SetRollover(val rollover: Boolean) : BudgetEntryEvent()
    data class SetNote(val note: String) : BudgetEntryEvent()
    data class SetCategoryIds(val ids: Set<Long>) : BudgetEntryEvent()
    data class SetAccountIds(val ids: Set<String>) : BudgetEntryEvent()
    data class SetTags(val tags: Set<String>) : BudgetEntryEvent()
    data class SetCustomStartDate(val date: Long) : BudgetEntryEvent()
    data class SetCustomEndDate(val date: Long) : BudgetEntryEvent()
    
    // Visibility
    data class ShowCustomStartPicker(val show: Boolean) : BudgetEntryEvent()
    data class ShowCustomEndPicker(val show: Boolean) : BudgetEntryEvent()
    data class ShowNumpad(val show: Boolean) : BudgetEntryEvent()
    data class ShowAccountSheet(val show: Boolean) : BudgetEntryEvent()
    data class ShowTrackTypeSheet(val show: Boolean) : BudgetEntryEvent()
    data class ShowCategorySheet(val show: Boolean) : BudgetEntryEvent()
    data class ShowTagSheet(val show: Boolean) : BudgetEntryEvent()
    data class ShowPeriodSheet(val show: Boolean) : BudgetEntryEvent()
    
    data class SetAccountQuery(val query: String) : BudgetEntryEvent()
    data class SetCategoryQuery(val query: String) : BudgetEntryEvent()
    
    data object TriggerNavigation : BudgetEntryEvent()
}
