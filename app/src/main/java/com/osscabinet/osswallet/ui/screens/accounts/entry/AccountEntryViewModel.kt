package com.osscabinet.ossWallet.ui.screens.accounts.entry

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.CreditCardDetails
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.ComputationRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import com.osscabinet.ossWallet.domain.service.aggregation.TransactionAggregationService
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.query.DatabaseQueryService
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AccountEntryViewModel @Inject constructor(
    private val accountUseCases: AccountUseCases,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val computationRepository: ComputationRepository,
    private val queryService: DatabaseQueryService,
    settingsRepository: SettingsRepository
) : BaseViewModel(settingsRepository) {

    private val _state = MutableStateFlow(AccountEntryState())
    val state: StateFlow<AccountEntryState> = _state.asStateFlow()

    private var isSaveInFlight = false

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

    // Only the fields that change the balance drive a recompute. Typing a name or picking an icon
    // leaves these unchanged, so it does not re-run the queries.
    private data class ProjectionInputs(
        val accountId: String?,
        val initialBalances: Map<String, String>,
        val pendingAdjustmentDeltas: Map<String, BigDecimal>,
        val primaryCurrency: String
    )

    private fun loadInitialData() {
        viewModelScope.launch {
            val inputs = _state
                .map {
                    ProjectionInputs(it.accountId, it.initialBalances, it.pendingAdjustmentDeltas, it.primaryCurrency)
                }
                .distinctUntilChanged()
            val versions = settingsRepository.currentProfileId.flatMapLatest { profileId ->
                transactionRepository.getLatestTransactionVersion(profileId)
            }
            combine(
                settingsRepository.currency,
                settingsRepository.currentProfileId,
                versions,
                computationRepository.exchangeRatesSnapshot,
                inputs
            ) { defaultCurrency, profileId, _, exchangeRatesSnapshot, state ->
                val accountId = state.accountId
                val now = System.currentTimeMillis()

                if (accountId == null) {
                    // No stored balance yet, so the opening balances being typed are the closing balances.
                    _state.update {
                        it.copy(
                            defaultCurrency = defaultCurrency,
                            exchangeRates = exchangeRatesSnapshot.rates,
                            systemClosingBalances = state.initialBalances.mapValues { (_, value) ->
                                value.toBigDecimalOrNull() ?: BigDecimal.ZERO
                            }
                        )
                    }
                    return@combine
                }

                // 1. Calculate current database state using SQL aggregation
                val accountFilter = TransactionFilter(
                    accountIds = setOf(accountId),
                    timelineRange = TimelineRange.CustomRange(end = now)
                )

                val dbSummaries = TransactionAggregationService.getSummaryByCurrency(
                    queryService,
                    profileId,
                    accountFilter
                ).getOrDefault(emptyMap())

                // 2. Identify the database's "Initial Balance" components
                val initialBalanceFilter = accountFilter.copy(
                    categoryIds = setOf(Category.SYSTEM_INITIAL_BALANCE)
                )
                val dbInitialBalancesSummaries = TransactionAggregationService.getSummaryByCurrency(
                    queryService,
                    profileId,
                    initialBalanceFilter
                ).getOrDefault(emptyMap())
                val dbInitialBalances =
                    dbInitialBalancesSummaries.mapValues { it.value.totalAmount }

                // 3. Compute projected closing balances for all relevant currencies
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
                    CurrencyService.calculateTotalInCurrency(
                        projectedClosingBalances,
                        state.primaryCurrency,
                        exchangeRatesSnapshot
                    )

                val currencyUsageMap = dbSummaries.mapValues { it.value.transactionCount > 0 }

                _state.update {
                    it.copy(
                        defaultCurrency = defaultCurrency,
                        exchangeRates = exchangeRatesSnapshot.rates,
                        currentBalance = currentBalance,
                        currencyUsageMap = currencyUsageMap,
                        systemClosingBalances = projectedClosingBalances
                    )
                }
            }.collect()
        }
    }

    fun initialize(
        accountId: String? = null,
        prefillName: String? = null,
        prefillCurrency: String? = null,
        isDraftMode: Boolean = false
    ) {
        if (accountId == null) {
            _state.update {
                val currency = prefillCurrency ?: it.defaultCurrency
                it.copy(
                    isEditMode = false,
                    isDraftMode = isDraftMode,
                    accountId = null,
                    accountName = prefillName ?: it.accountName,
                    primaryCurrency = currency,
                    allowedCurrencies = if (currency.isNotBlank()) setOf(currency) else emptySet()
                )
            }
            return
        }

        viewModelScope.launch {
            val profileId = settingsRepository.currentProfileId.first()
            accountRepository.getAllAccountsSync(profileId).find { it.id == accountId }
                ?.let { account ->
                    // Currencies the account no longer allows but which still hold balance data are
                    // surfaced too (as orphanedCurrencies), so the user can remove them - but they
                    // must never silently rejoin allowedCurrencies if the user saves without touching them.
                    val balanceDataCurrencies =
                        transactionRepository.getCurrenciesWithBalanceData(accountId, profileId).toSet()
                    val displayCurrencies = (account.allowedCurrencies + balanceDataCurrencies).distinct()
                    val txByCurrency = displayCurrencies.mapNotNull { currency ->
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
                            currenciesWithBalanceData = balanceDataCurrencies,
                            selectedIconName = account.icon,
                            accentColorHex = account.colorHex,
                            initialBalances = initialBalances,
                            creditLimitInput = account.creditDetails?.creditLimit?.stripTrailingZeros()?.toPlainString() ?: "",
                            statementClosingDayInput = account.creditDetails?.statementClosingDay?.toString() ?: "",
                            paymentDueDayInput = account.creditDetails?.paymentDueDay?.toString() ?: "",
                            interestRateAprInput = account.creditDetails?.interestRateApr?.toString() ?: ""
                        )
                    }
                }
        }
    }

    fun onEvent(event: AccountEntryEvent) {
        when (event) {
            is AccountEntryEvent.SetName -> _state.update { it.copy(accountName = event.name) }
            is AccountEntryEvent.SetStructureType -> _state.update {
                it.copy(
                    structureType = event.type,
                    selectedIconName = event.type.iconKey,
                    accentColorHex = event.type.colorHex
                )
            }

            is AccountEntryEvent.SetPrimaryCurrency -> _state.update { it.copy(primaryCurrency = event.currency) }
            is AccountEntryEvent.SetAllowedCurrencies -> {
                // event.currencies is the new full *displayed* set (allowed + orphaned) after one
                // chip was added or removed in the UI - never assume it's purely "truly allowed".
                val s = _state.value
                val oldDisplayed = s.displayedCurrencies
                val removed = oldDisplayed - event.currencies
                if (removed.any { s.hasExistingBalanceData(it) }) {
                    // Hold the removal until the user confirms the balance data goes with it
                    _state.update { it.copy(pendingCurrencyRemoval = removed) }
                } else {
                    applyAllowedCurrencies(event.currencies, oldDisplayed, removed)
                }
            }

            is AccountEntryEvent.ConfirmCurrencyRemoval -> {
                val s = _state.value
                applyAllowedCurrencies(
                    s.displayedCurrencies - s.pendingCurrencyRemoval,
                    s.displayedCurrencies,
                    s.pendingCurrencyRemoval
                )
            }

            is AccountEntryEvent.CancelCurrencyRemoval -> _state.update {
                it.copy(pendingCurrencyRemoval = emptySet())
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

            // Credit details events
            is AccountEntryEvent.SetCreditLimit -> _state.update { it.copy(creditLimitInput = event.limit) }
            is AccountEntryEvent.SetStatementClosingDay -> _state.update { it.copy(statementClosingDayInput = event.day) }
            is AccountEntryEvent.SetPaymentDueDay -> _state.update { it.copy(paymentDueDayInput = event.day) }
            is AccountEntryEvent.SetInterestRateApr -> _state.update { it.copy(interestRateAprInput = event.apr) }

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
                        // Before the account exists there is no history to record against
                        balanceUpdateMode = if (it.accountId == null) BalanceUpdateMode.INITIAL_BALANCE
                        else BalanceUpdateMode.RECORD
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

                    // Create/draft mode has no history, so a recorded amount is the opening balance.
                    if (s.balanceUpdateMode == BalanceUpdateMode.INITIAL_BALANCE || s.accountId == null) {
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

            // TODO(delete): dead adjustment-dialog handlers (issue 9, option A). Nothing dispatches
            // these events and nothing renders showAdjustmentDialog. Delete them together with the
            // matching events in AccountEntryEvent and the state fields in AccountEntryState.
            // is AccountEntryEvent.ShowAdjustmentDialog -> _state.update {
            //     it.copy(
            //         showCalculator = false,
            //         showAdjustmentDialog = true,
            //         adjustmentDialogCurrency = event.currency,
            //         adjustmentDialogNewValue = event.newValue
            //     )
            // }
            //
            // is AccountEntryEvent.DismissAdjustmentDialog -> _state.update {
            //     val orig = it.calculatorOriginalValue
            //     val currency = it.adjustmentDialogCurrency
            //     val revertedBalances = if (orig != null && currency != null)
            //         it.initialBalances.toMutableMap().apply { put(currency, orig) }
            //     else it.initialBalances
            //     it.copy(
            //         showAdjustmentDialog = false,
            //         adjustmentDialogCurrency = null,
            //         adjustmentDialogNewValue = null,
            //         activeBalanceCurrency = null,
            //         calculatorOriginalValue = null,
            //         initialBalances = revertedBalances
            //     )
            // }
            //
            // is AccountEntryEvent.ConfirmAsNewTransaction -> {
            //     val origBD = event.originalValue.toBigDecimalOrNull() ?: BigDecimal.ZERO
            //     val delta = (event.newValue.toBigDecimalOrNull() ?: BigDecimal.ZERO) - origBD
            //     _state.update {
            //         it.copy(
            //             showAdjustmentDialog = false,
            //             adjustmentDialogCurrency = null,
            //             adjustmentDialogNewValue = null,
            //             activeBalanceCurrency = null,
            //             calculatorOriginalValue = null,
            //             initialBalances = it.initialBalances.toMutableMap().apply {
            //                 put(event.currency, event.originalValue)
            //             },
            //             pendingAdjustmentDeltas = it.pendingAdjustmentDeltas + (event.currency to delta)
            //         )
            //     }
            // }
            //
            // is AccountEntryEvent.ConfirmAsInitialBalanceUpdate -> _state.update {
            //     it.copy(
            //         showAdjustmentDialog = false,
            //         adjustmentDialogCurrency = null,
            //         adjustmentDialogNewValue = null,
            //         activeBalanceCurrency = null,
            //         calculatorOriginalValue = null
            //     )
            // }

            is AccountEntryEvent.TriggerNavigation -> _state.update {
                val accountId = (event.result as? AccountEntryResult.Persisted)?.accountId
                it.copy(
                    accountId = accountId ?: it.accountId,
                    pendingResult = event.result,
                    navTriggered = true
                )
            }

            is AccountEntryEvent.DeleteAccount -> {
                // Handled via separate callback to allow navigation control
            }
        }
    }

    // Applies a currency change. newDisplayed is the full displayed set (allowed + orphaned) after
    // the UI's single add/remove; oldDisplayed is what it replaced, so the delta tells us which
    // currencies were genuinely added/removed - never promoting an untouched orphan into
    // allowedCurrencies, and never leaving a removed one counted as still having balance data.
    // A removed currency loses its pending adjustments, and any opening balance it held is set to
    // zero so the save deletes that row.
    private fun applyAllowedCurrencies(newDisplayed: Set<String>, oldDisplayed: Set<String>, removed: Set<String>) {
        _state.update { state ->
            val added = newDisplayed - oldDisplayed
            val currencies = (state.allowedCurrencies - removed) + added
            var newPrimary = state.primaryCurrency
            if (currencies.isEmpty()) {
                newPrimary = ""
            } else if (currencies.size == 1) {
                newPrimary = currencies.first()
            } else if (state.primaryCurrency.isNotEmpty() && !currencies.contains(state.primaryCurrency)) {
                newPrimary = currencies.first()
            }
            state.copy(
                allowedCurrencies = currencies,
                currenciesWithBalanceData = state.currenciesWithBalanceData - removed,
                primaryCurrency = newPrimary,
                initialBalances = state.initialBalances +
                    removed.filter { it in state.initialBalances }.associateWith { "0" },
                pendingAdjustmentDeltas = state.pendingAdjustmentDeltas - removed,
                pendingCurrencyRemoval = emptySet()
            )
        }
    }

    fun saveAccount(onSuccess: (AccountEntryResult) -> Unit) {
        val s = _state.value
        if (!s.canSave) return

        if (s.isDraftMode) {
            onSuccess(
                AccountEntryResult.Draft(
                    AccountEntryDraft(
                        name = s.accountName,
                        structureType = s.structureType.name,
                        primaryCurrency = s.primaryCurrency,
                        allowedCurrencies = s.allowedCurrencies.toList(),
                        initialBalances = s.initialBalances,
                        iconName = s.selectedIconName,
                        colorHex = s.accentColorHex
                    )
                )
            )
            return
        }

        // A second tap before the first save finishes would repeat its adjustments or create a duplicate account
        if (isSaveInFlight) return
        isSaveInFlight = true

        viewModelScope.launch {
            try {
                val profileId = settingsRepository.currentProfileId.first()
                val savedAccountId: String
                if (s.isEditMode && s.existingAccount != null) {
                    val accountId = s.accountId!!
                    accountUseCases.saveAccountEdits(
                        account = s.existingAccount.copy(
                            name = s.accountName,
                            structureType = s.structureType,
                            currencyCode = s.primaryCurrency,
                            allowedCurrencies = s.allowedCurrencies.toList(),
                            colorHex = s.accentColorHex,
                            icon = s.selectedIconName
                        ),
                        creditDetails = creditDetailsFor(s, accountId),
                        // Removed currencies are included at zero, so their stored balance is deleted
                        openingBalances = (s.allowedCurrencies + s.initialBalances.keys).associateWith { currency ->
                            s.initialBalances[currency]?.toBigDecimalOrNull() ?: BigDecimal.ZERO
                        },
                        pendingAdjustments = s.pendingAdjustmentDeltas,
                        profileId = profileId
                    )
                    savedAccountId = accountId
                } else {
                    val newAccountId = UUID.randomUUID().toString()
                    accountUseCases.createAccountWithInitialBalances(
                        name = s.accountName,
                        initialBalances = s.initialBalances,
                        currency = s.primaryCurrency,
                        structureType = s.structureType,
                        colorHex = s.accentColorHex,
                        icon = s.selectedIconName,
                        allowedCurrencies = s.allowedCurrencies.toList(),
                        profileId = profileId,
                        accountId = newAccountId,
                        creditDetails = creditDetailsFor(s, newAccountId)
                    )
                    savedAccountId = newAccountId
                }

                onSuccess(AccountEntryResult.Persisted(savedAccountId))
            } finally {
                isSaveInFlight = false
            }
        }
    }

    private fun creditDetailsFor(s: AccountEntryState, accountId: String): CreditCardDetails? {
        val creditLimit = s.creditLimitInput.toBigDecimalOrNull()
        if (s.structureType != AccountStructureType.CREDIT || creditLimit == null) return null
        return CreditCardDetails(
            accountId = accountId,
            creditLimit = creditLimit,
            statementClosingDay = s.statementClosingDayInput.toIntOrNull(),
            paymentDueDay = s.paymentDueDayInput.toIntOrNull(),
            interestRateApr = s.interestRateAprInput.toFloatOrNull()
        )
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
