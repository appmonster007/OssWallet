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
    val structureType: AccountStructureType = AccountStructureType.BANK,
    val primaryCurrency: String = "USD",
    val allowedCurrencies: Set<String> = emptySet(),
    // Snapshot, taken on load, of every currency (allowed or not) that holds an opening balance or
    // adjustment row in the DB. Drives orphan detection and the removal-confirmation gate below.
    val currenciesWithBalanceData: Set<String> = emptySet(),
    val initialBalances: Map<String, String> = emptyMap(),  // signed amounts: positive=inflow, negative=outflow
    val selectedIconName: String = AccountStructureType.BANK.iconKey,
    val accentColorHex: String = AccountStructureType.BANK.colorHex,
    val currentBalance: BigDecimal = BigDecimal.ZERO,

    // Credit Card Details (Active when structureType == CREDIT)
    val creditLimitInput: String = "",
    val statementClosingDayInput: String = "",
    val paymentDueDayInput: String = "",
    val interestRateAprInput: String = "",

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
    // TODO(delete): dead adjustment-dialog fields (issue 9, option A). See AccountEntryViewModel.
    // val showAdjustmentDialog: Boolean = false,
    // val adjustmentDialogCurrency: String? = null,
    // val adjustmentDialogNewValue: String? = null,
    val pendingAdjustmentDeltas: Map<String, BigDecimal> = emptyMap(),
    // Currencies the user has asked to remove while they still hold an opening balance; applied only on confirm
    val pendingCurrencyRemoval: Set<String> = emptySet(),
    val isEditMode: Boolean = false,
    val accountId: String? = null,
    val navTriggered: Boolean = false,

    // When true, saveAccount() never touches the repository — it just hands the configured
    // fields back via AccountEntryResult.Draft for the caller to persist on its own timeline
    // (e.g. the CSV import wizard defers account creation to its final "Import" step).
    val isDraftMode: Boolean = false,
    val pendingResult: AccountEntryResult? = null,

    // External data
    val existingAccount: Account? = null,
    val currencyUsageMap: Map<String, Boolean> = emptyMap(),
    val exchangeRates: Map<String, BigDecimal> = emptyMap(),
    val defaultCurrency: String = "USD",
    val shortlistedCurrencies: List<String> = emptyList()
) {
    /** Currencies holding existing balance data but no longer in allowedCurrencies. */
    val orphanedCurrencies: Set<String>
        get() = currenciesWithBalanceData - allowedCurrencies

    /** All currencies shown to the user: truly allowed ones plus orphaned ones awaiting removal. */
    val displayedCurrencies: Set<String>
        get() = allowedCurrencies + orphanedCurrencies

    /** True when [currency] holds a non-zero opening balance, which removing the currency would delete. */
    fun hasOpeningBalance(currency: String): Boolean =
        (initialBalances[currency]?.toBigDecimalOrNull() ?: BigDecimal.ZERO).signum() != 0

    /**
     * True when removing [currency] would orphan an existing DB row - either a non-zero opening
     * balance, or a pre-existing balance/adjustment row that isn't reflected in [initialBalances]
     * (e.g. a currency whose only history is a reconciliation adjustment).
     */
    fun hasExistingBalanceData(currency: String): Boolean =
        hasOpeningBalance(currency) || currency in currenciesWithBalanceData

    val pendingRemovalWithBalance: Set<String>
        get() = pendingCurrencyRemoval.filter { hasExistingBalanceData(it) }.toSet()

    val canSave: Boolean
        get() {
            val isBalanceValid =
                initialBalances.values.all { it.isEmpty() || it.toBigDecimalOrNull() != null }
            return accountName.isNotBlank() && primaryCurrency.isNotBlank() &&
                    allowedCurrencies.isNotEmpty() && isBalanceValid
        }
}
