package com.osscabinet.ossWallet.ui.screens.accounts.entry

import com.osscabinet.ossWallet.domain.model.AccountStructureType

sealed class AccountEntryEvent {
    data class SetName(val name: String) : AccountEntryEvent()
    data class SetStructureType(val type: AccountStructureType) : AccountEntryEvent()
    data class SetPrimaryCurrency(val currency: String) : AccountEntryEvent()
    data class SetAllowedCurrencies(val currencies: Set<String>) : AccountEntryEvent()
    data class SetInitialBalance(val currency: String, val balance: String) : AccountEntryEvent()
    data class SetIcon(val icon: String) : AccountEntryEvent()
    data class SetColor(val color: String) : AccountEntryEvent()

    // Balance Update Flow
    data class OpenBalanceUpdate(val currency: String) : AccountEntryEvent()
    data class SetBalanceUpdateMode(val mode: BalanceUpdateMode) : AccountEntryEvent()
    data class SetBalanceUpdateAmount(val amount: String) : AccountEntryEvent()
    data object OpenCalculatorForUpdate : AccountEntryEvent()
    data object ConfirmBalanceUpdate : AccountEntryEvent()
    data object CloseBalanceUpdate : AccountEntryEvent()

    data class ShowStructureSheet(val show: Boolean) : AccountEntryEvent()
    data class ShowIconSheet(val show: Boolean) : AccountEntryEvent()
    data class ShowColorSheet(val show: Boolean) : AccountEntryEvent()
    data class ShowCurrencySheet(val show: Boolean) : AccountEntryEvent()
    data class OpenCalculator(val currency: String) : AccountEntryEvent()
    data object CloseCalculator : AccountEntryEvent()
    data class ShowAdjustmentDialog(val currency: String, val newValue: String) :
        AccountEntryEvent()

    data object DismissAdjustmentDialog : AccountEntryEvent()
    data class ConfirmAsNewTransaction(
        val currency: String,
        val originalValue: String,
        val newValue: String
    ) : AccountEntryEvent()

    data class ConfirmAsInitialBalanceUpdate(val currency: String, val newValue: String) :
        AccountEntryEvent()

    data class TriggerNavigation(val result: AccountEntryResult) : AccountEntryEvent()
    data object DeleteAccount : AccountEntryEvent()
}
