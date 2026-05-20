package com.osscabinet.ossWallet.ui.screens.accounts.entry

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import java.math.BigDecimal

data class AccountEntryState(
    val accountName: String = "",
    val structureType: AccountStructureType = AccountStructureType.SAVINGS,
    val primaryCurrency: String = "USD",
    val allowedCurrencies: Set<String> = emptySet(),
    val initialBalances: Map<String, String> = emptyMap(),
    val selectedIconName: String = "AccountBalance",
    val accentColorHex: String = "#004D5F",
    val currentBalance: BigDecimal = BigDecimal.ZERO,
    
    // UI State
    val showStructureSheet: Boolean = false,
    val showIconSheet: Boolean = false,
    val showColorSheet: Boolean = false,
    val showCurrencySheet: Boolean = false,
    val showCalculator: Boolean = false,
    val activeBalanceCurrency: String? = null,
    val isEditMode: Boolean = false,
    val accountId: String? = null,
    val navTriggered: Boolean = false,
    
    // External data
    val existingAccount: Account? = null,
    val currencyUsageMap: Map<String, Boolean> = emptyMap(),
    val exchangeRates: Map<String, BigDecimal> = emptyMap(),
    val defaultCurrency: String = "USD"
) {
    val canSave: Boolean
        get() {
            val isBalanceValid = initialBalances.values.all { it.isEmpty() || it.toBigDecimalOrNull() != null }
            return accountName.isNotBlank() && primaryCurrency.isNotBlank() &&
                    allowedCurrencies.isNotEmpty() && (isEditMode || isBalanceValid)
        }
}
