package com.osscabinet.ossWallet.ui.screens.accounts.entry

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.ComputationRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import com.osscabinet.ossWallet.domain.service.aggregation.AggregationService
import com.osscabinet.ossWallet.domain.usecase.AccountUseCases
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AccountEntryViewModel @Inject constructor(
    private val accountUseCases: AccountUseCases,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val computationRepository: ComputationRepository,
    settingsRepository: SettingsRepository
) : BaseViewModel(settingsRepository) {

    private val _state = MutableStateFlow(AccountEntryState())
    val state: StateFlow<AccountEntryState> = _state.asStateFlow()

    init {
        loadInitialData()
        viewModelScope.launch {
            effectiveShortlistedCurrencies.collect { currencies ->
                val popularAssets = setOf(
                    "USD", "EUR", "JPY", "GBP", "AUD", "CAD", "CHF", "CNY",
                    "KRW", "INR", "BRL", "RUB", "MXN", "SGD", "HKD", "TRY",
                    "SEK", "NOK", "NZD", "ZAR"
                )
                val refinedShortlist = currencies.filter { it.uppercase() in popularAssets }
                _state.update { it.copy(shortlistedCurrencies = refinedShortlist) }
            }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            combine(
                settingsRepository.currency,
                settingsRepository.currentProfileId.flatMapLatest { profileId ->
                    transactionRepository.getAllTransactions(profileId)
                },
                computationRepository.exchangeRates,
                _state
            ) { defaultCurrency, transactions, exchangeRates, state ->
                val accountId = state.accountId
                val now = System.currentTimeMillis()

                if (accountId == null) {
                    _state.update {
                        it.copy(
                            defaultCurrency = defaultCurrency,
                            exchangeRates = exchangeRates
                        )
                    }
                    return@combine
                }

                // 1. Get all transactions for this account up to now
                val accountTransactions =
                    transactions.filter { it.accountId == accountId && it.timestamp <= now }

                // 2. Calculate current database state using reusable domain service
                val dbSummaries = AggregationService.calculateSummaryByCurrency(accountTransactions)

                // 3. Identify the database's "Initial Balance" components
                val dbInitialBalances = accountTransactions
                    .filter { Category.isInitialBalance(it.categoryId) }
                    .groupBy { it.currency }
                    .mapValues { entry -> entry.value.sumOf { it.amount } }

                // 4. Compute projected closing balances for all relevant currencies
                // Projected = (DB Total) - (DB Initial) + (UI Initial) + (UI Pending Deltas)
                val allRelevantCurrencies =
                    (dbSummaries.keys + state.initialBalances.keys + state.pendingAdjustmentDeltas.keys).toSet()

                val projectedClosingBalances = allRelevantCurrencies.associateWith { currency ->
                    val dbTotal = dbSummaries[currency]?.totalAmount ?: BigDecimal.ZERO
                    val dbInitial = dbInitialBalances[currency] ?: BigDecimal.ZERO
                    val uiInitial =
                        state.initialBalances[currency]?.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    val uiPending = state.pendingAdjustmentDeltas[currency] ?: BigDecimal.ZERO

                    dbTotal - dbInitial + uiInitial + uiPending
                }

                // 5. Calculate account-level net balance in primary currency
                val currentBalance =
                    com.osscabinet.ossWallet.domain.service.currency.CurrencyService.calculateTotalInCurrency(
                        projectedClosingBalances,
                        state.primaryCurrency,
                        exchangeRates
                    )

                val currencyUsageMap = accountTransactions
                    .groupBy { it.currency }
                    .mapValues { it.value.isNotEmpty() }

                _state.update {
                    it.copy(
                        defaultCurrency = defaultCurrency,
                        exchangeRates = exchangeRates,
                        currentBalance = currentBalance,
                        currencyUsageMap = currencyUsageMap,
                        systemClosingBalances = projectedClosingBalances
                    )
                }
            }.collect()
        }
    }

    fun initialize(accountId: String? = null) {
        if (accountId == null) {
            _state.update {
                it.copy(
                    isEditMode = false,
                    accountId = null,
                    primaryCurrency = it.defaultCurrency,
                    allowedCurrencies = if (it.defaultCurrency.isNotBlank()) setOf(it.defaultCurrency) else emptySet()
                )
            }
            return
        }

        viewModelScope.launch {
            val profileId = settingsRepository.currentProfileId.first()
            accountRepository.getAllAccountsSync(profileId).find { it.id == accountId }
                ?.let { account ->
                    val txByCurrency = account.allowedCurrencies.mapNotNull { currency ->
                        val tx = transactionRepository.getInitialBalanceTransaction(
                            accountId,
                            currency,
                            profileId
                        )
                        tx?.let { currency to it }
                    }.toMap()

                    val initialBalances = txByCurrency.mapValues { (_, tx) ->
                        tx.amount.stripTrailingZeros().toPlainString()
                    }

                    _state.update {
                        it.copy(
                            accountId = accountId,
                            isEditMode = true,
                            existingAccount = account,
                            accountName = account.name,
                            structureType = account.structureType,
                            primaryCurrency = account.currencyCode,
                            allowedCurrencies = account.allowedCurrencies.toSet(),
                            selectedIconName = account.icon,
                            accentColorHex = account.colorHex,
                            initialBalances = initialBalances
                        )
                    }
                }
        }
    }

    fun onEvent(event: AccountEntryEvent) {
        when (event) {
            is AccountEntryEvent.SetName -> _state.update { it.copy(accountName = event.name) }
            is AccountEntryEvent.SetStructureType -> _state.update { it.copy(structureType = event.type) }
            is AccountEntryEvent.SetPrimaryCurrency -> _state.update { it.copy(primaryCurrency = event.currency) }
            is AccountEntryEvent.SetAllowedCurrencies -> {
                _state.update { state ->
                    var newPrimary = state.primaryCurrency
                    if (event.currencies.isEmpty()) {
                        newPrimary = ""
                    } else if (event.currencies.size == 1) {
                        newPrimary = event.currencies.first()
                    } else if (state.primaryCurrency.isNotEmpty() && !event.currencies.contains(
                            state.primaryCurrency
                        )
                    ) {
                        newPrimary = event.currencies.first()
                    }
                    state.copy(allowedCurrencies = event.currencies, primaryCurrency = newPrimary)
                }
            }

            is AccountEntryEvent.SetInitialBalance -> {
                _state.update {
                    it.copy(
                        initialBalances = it.initialBalances.toMutableMap()
                            .apply { put(event.currency, event.balance) })
                }
            }

            is AccountEntryEvent.SetIcon -> _state.update { it.copy(selectedIconName = event.icon) }
            is AccountEntryEvent.SetColor -> _state.update { it.copy(accentColorHex = event.color) }

            is AccountEntryEvent.OpenBalanceUpdate -> {
                val currency = event.currency
                val currentProjectedTotal =
                    _state.value.systemClosingBalances[currency] ?: BigDecimal.ZERO

                _state.update {
                    it.copy(
                        showBalanceUpdateSheet = true,
                        balanceUpdateCurrency = currency,
                        balanceUpdateInputAmount = currentProjectedTotal.stripTrailingZeros()
                            .toPlainString(),
                        balanceUpdateMode = BalanceUpdateMode.RECORD
                    )
                }
            }

            is AccountEntryEvent.SetBalanceUpdateMode -> _state.update { it.copy(balanceUpdateMode = event.mode) }
            is AccountEntryEvent.SetBalanceUpdateAmount -> _state.update {
                it.copy(
                    balanceUpdateInputAmount = event.amount
                )
            }

            is AccountEntryEvent.OpenCalculatorForUpdate -> {
                val currency = _state.value.balanceUpdateCurrency
                if (currency != null) {
                    _state.update {
                        it.copy(
                            showCalculator = true,
                            activeBalanceCurrency = currency,
                            calculatorOriginalValue = it.balanceUpdateInputAmount
                        )
                    }
                }
            }

            is AccountEntryEvent.ConfirmBalanceUpdate -> {
                val s = _state.value
                val currency = s.balanceUpdateCurrency
                if (currency != null) {
                    val newUserTotal =
                        s.balanceUpdateInputAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    val currentProjectedTotal = s.systemClosingBalances[currency] ?: BigDecimal.ZERO
                    val totalAdjustmentNeeded = newUserTotal - currentProjectedTotal

                    if (s.balanceUpdateMode == BalanceUpdateMode.INITIAL_BALANCE) {
                        val currentInitial =
                            s.initialBalances[currency]?.toBigDecimalOrNull() ?: BigDecimal.ZERO
                        val newOpening = currentInitial + totalAdjustmentNeeded
                        _state.update {
                            it.copy(
                                initialBalances = it.initialBalances + (currency to newOpening.stripTrailingZeros()
                                    .toPlainString()),
                                showBalanceUpdateSheet = false,
                                balanceUpdateCurrency = null
                            )
                        }
                    } else {
                        val pendingDelta = s.pendingAdjustmentDeltas[currency] ?: BigDecimal.ZERO
                        val newPendingDelta = pendingDelta + totalAdjustmentNeeded
                        _state.update {
                            it.copy(
                                pendingAdjustmentDeltas = it.pendingAdjustmentDeltas + (currency to newPendingDelta),
                                showBalanceUpdateSheet = false,
                                balanceUpdateCurrency = null
                            )
                        }
                    }
                }
            }

            is AccountEntryEvent.CloseBalanceUpdate -> _state.update {
                it.copy(
                    showBalanceUpdateSheet = false,
                    balanceUpdateCurrency = null
                )
            }

            is AccountEntryEvent.ShowStructureSheet -> _state.update { it.copy(showStructureSheet = event.show) }
            is AccountEntryEvent.ShowIconSheet -> _state.update { it.copy(showIconSheet = event.show) }
            is AccountEntryEvent.ShowColorSheet -> _state.update { it.copy(showColorSheet = event.show) }
            is AccountEntryEvent.ShowCurrencySheet -> _state.update { it.copy(showCurrencySheet = event.show) }
            is AccountEntryEvent.OpenCalculator -> _state.update {
                it.copy(
                    showCalculator = true,
                    activeBalanceCurrency = event.currency,
                    calculatorOriginalValue = it.initialBalances[event.currency] ?: "0"
                )
            }

            is AccountEntryEvent.CloseCalculator -> _state.update {
                it.copy(
                    showCalculator = false,
                    activeBalanceCurrency = null,
                    calculatorOriginalValue = null
                )
            }

            is AccountEntryEvent.ShowAdjustmentDialog -> _state.update {
                it.copy(
                    showCalculator = false,
                    showAdjustmentDialog = true,
                    adjustmentDialogCurrency = event.currency,
                    adjustmentDialogNewValue = event.newValue
                )
            }

            is AccountEntryEvent.DismissAdjustmentDialog -> _state.update {
                val orig = it.calculatorOriginalValue
                val currency = it.adjustmentDialogCurrency
                val revertedBalances = if (orig != null && currency != null)
                    it.initialBalances.toMutableMap().apply { put(currency, orig) }
                else it.initialBalances
                it.copy(
                    showAdjustmentDialog = false,
                    adjustmentDialogCurrency = null,
                    adjustmentDialogNewValue = null,
                    activeBalanceCurrency = null,
                    calculatorOriginalValue = null,
                    initialBalances = revertedBalances
                )
            }

            is AccountEntryEvent.ConfirmAsNewTransaction -> {
                val origBD = event.originalValue.toBigDecimalOrNull() ?: BigDecimal.ZERO
                val delta = (event.newValue.toBigDecimalOrNull() ?: BigDecimal.ZERO) - origBD
                _state.update {
                    it.copy(
                        showAdjustmentDialog = false,
                        adjustmentDialogCurrency = null,
                        adjustmentDialogNewValue = null,
                        activeBalanceCurrency = null,
                        calculatorOriginalValue = null,
                        initialBalances = it.initialBalances.toMutableMap().apply {
                            put(event.currency, event.originalValue)
                        },
                        pendingAdjustmentDeltas = it.pendingAdjustmentDeltas + (event.currency to delta)
                    )
                }
            }

            is AccountEntryEvent.ConfirmAsInitialBalanceUpdate -> _state.update {
                it.copy(
                    showAdjustmentDialog = false,
                    adjustmentDialogCurrency = null,
                    adjustmentDialogNewValue = null,
                    activeBalanceCurrency = null,
                    calculatorOriginalValue = null
                )
            }

            is AccountEntryEvent.TriggerNavigation -> _state.update { it.copy(navTriggered = true) }
            is AccountEntryEvent.DeleteAccount -> {
                // Handled via separate callback to allow navigation control
            }
        }
    }

    fun saveAccount(onSuccess: () -> Unit) {
        val s = _state.value
        if (!s.canSave) return

        viewModelScope.launch {
            val profileId = settingsRepository.currentProfileId.first()
            if (s.isEditMode && s.existingAccount != null) {
                accountRepository.updateAccount(
                    s.existingAccount.copy(
                        name = s.accountName,
                        structureType = s.structureType,
                        currencyCode = s.primaryCurrency,
                        allowedCurrencies = s.allowedCurrencies.toList(),
                        colorHex = s.accentColorHex,
                        icon = s.selectedIconName
                    ),
                    profileId
                )
                s.pendingAdjustmentDeltas.forEach { (currency, delta) ->
                    if (delta.signum() != 0) {
                        val absAmount = delta.abs()
                        transactionRepository.insertTransaction(
                            Transaction(
                                name = "Balance Adjustment",
                                accountId = s.accountId!!,
                                amount = if (delta.signum() > 0) absAmount else absAmount.negate(),
                                timestamp = System.currentTimeMillis(),
                                currency = currency,
                                categoryId = Category.SYSTEM_TRANSFER,
                                transactionType = if (delta.signum() > 0) TransactionType.TRANSFER_INFLOW
                                else TransactionType.TRANSFER_OUTFLOW,
                                note = "Balance adjustment"
                            ),
                            profileId
                        )
                    }
                }
                s.allowedCurrencies.forEach { currency ->
                    val signed =
                        s.initialBalances[currency]?.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    accountUseCases.upsertInitialBalance(s.accountId!!, currency, signed, profileId)
                }
            } else {
                accountUseCases.createAccountWithInitialBalances(
                    name = s.accountName,
                    initialBalances = s.initialBalances,
                    currency = s.primaryCurrency,
                    structureType = s.structureType,
                    colorHex = s.accentColorHex,
                    icon = s.selectedIconName,
                    allowedCurrencies = s.allowedCurrencies.toList(),
                    profileId = profileId
                )
            }
            onSuccess()
        }
    }

    fun deleteAccount(onSuccess: () -> Unit) {
        val id = _state.value.accountId ?: return
        viewModelScope.launch {
            withProfile { profileId ->
                accountUseCases.deleteAccount(id, profileId)
            }
            onSuccess()
        }
    }
}
