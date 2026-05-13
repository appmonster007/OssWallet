package com.osscabinet.ossWallet.ui.screens.budgets.entry

import com.osscabinet.ossWallet.domain.model.BudgetPeriod
import com.osscabinet.ossWallet.domain.model.BudgetTrackType

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
