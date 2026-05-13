package com.osscabinet.ossWallet.ui.screens.accounts.entry

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import java.math.BigDecimal

data class AccountEntryState(
    val accountName: String = "",
    val structureType: String = "Savings",
    val primaryCurrency: String = "USD",
    val allowedCurrencies: Set<String> = emptySet(),
    val initialBalances: Map<String, String> = emptyMap(),
    val selectedIconName: String = "AccountBalance",
    val accentColorHex: String = "#004D5F",
    
    // UI State
    val showStructureSheet: Boolean = false,
    val isEditMode: Boolean = false,
    val accountId: String? = null,
    val navTriggered: Boolean = false,
    
    // External data
    val existingAccount: Account? = null,
    val allTransactions: List<Transaction> = emptyList(),
    val exchangeRates: Map<String, BigDecimal> = emptyMap(),
    val defaultCurrency: String = "USD"
) {
    val canSave: Boolean
        get() {
            val isBalanceValid = initialBalances.values.all { it.isEmpty() || it.toBigDecimalOrNull() != null }
            return accountName.isNotBlank() && primaryCurrency.isNotBlank() &&
                    allowedCurrencies.isNotEmpty() && (isEditMode || isBalanceValid)
        }

    val currencyUsageMap: Map<String, Boolean>
        get() = if (isEditMode) {
            allTransactions.filter { it.accountId == accountId }
                .groupBy { it.currency }
                .mapValues { it.value.isNotEmpty() }
        } else emptyMap()
}

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
