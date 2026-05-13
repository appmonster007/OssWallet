package com.osscabinet.ossWallet.ui.screens.accounts.entry

sealed class AccountEntryEvent {
    data class SetName(val name: String) : AccountEntryEvent()
    data class SetStructureType(val type: String) : AccountEntryEvent()
    data class SetPrimaryCurrency(val currency: String) : AccountEntryEvent()
    data class SetAllowedCurrencies(val currencies: Set<String>) : AccountEntryEvent()
    data class SetInitialBalance(val currency: String, val balance: String) : AccountEntryEvent()
    data class SetIcon(val icon: String) : AccountEntryEvent()
    data class SetColor(val color: String) : AccountEntryEvent()
    
    data class ShowStructureSheet(val show: Boolean) : AccountEntryEvent()
    data object TriggerNavigation : AccountEntryEvent()
}
