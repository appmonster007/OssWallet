package com.osscabinet.ossWallet.ui.screens.accounts.entry

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import java.math.BigDecimal

enum class BalanceUpdateMode {
    INITIAL_BALANCE,
    RECORD
}

data class AccountEntryState(
    val accountName: String = "",
    val structureType: AccountStructureType = AccountStructureType.SAVINGS,
    val primaryCurrency: String = "USD",
    val allowedCurrencies: Set<String> = emptySet(),
    val initialBalances: Map<String, String> = emptyMap(),  // signed amounts: positive=inflow, negative=outflow
    val selectedIconName: String = "AccountBalance",
    val accentColorHex: String = "#004D5F",
    val currentBalance: BigDecimal = BigDecimal.ZERO,

    // Balance Update Flow
    val systemClosingBalances: Map<String, BigDecimal> = emptyMap(),
    val showBalanceUpdateSheet: Boolean = false,
    val balanceUpdateCurrency: String? = null,
    val balanceUpdateMode: BalanceUpdateMode = BalanceUpdateMode.RECORD,
    val balanceUpdateInputAmount: String = "0",

    // UI State
    val showStructureSheet: Boolean = false,
    val showIconSheet: Boolean = false,
    val showColorSheet: Boolean = false,
    val showCurrencySheet: Boolean = false,
    val showCalculator: Boolean = false,
    val activeBalanceCurrency: String? = null,
    val calculatorOriginalValue: String? = null,
    val showAdjustmentDialog: Boolean = false,
    val adjustmentDialogCurrency: String? = null,
    val adjustmentDialogNewValue: String? = null,
    val pendingAdjustmentDeltas: Map<String, BigDecimal> = emptyMap(),
    val isEditMode: Boolean = false,
    val accountId: String? = null,
    val navTriggered: Boolean = false,

    // External data
    val existingAccount: Account? = null,
    val currencyUsageMap: Map<String, Boolean> = emptyMap(),
    val exchangeRates: Map<String, BigDecimal> = emptyMap(),
    val defaultCurrency: String = "USD",
    val shortlistedCurrencies: List<String> = emptyList()
) {
    val canSave: Boolean
        get() {
            val isBalanceValid =
                initialBalances.values.all { it.isEmpty() || it.toBigDecimalOrNull() != null }
            return accountName.isNotBlank() && primaryCurrency.isNotBlank() &&
                    allowedCurrencies.isNotEmpty() && isBalanceValid
        }
}
