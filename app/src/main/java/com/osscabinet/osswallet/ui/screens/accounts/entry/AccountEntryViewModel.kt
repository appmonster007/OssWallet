package com.osscabinet.ossWallet.ui.screens.accounts.entry

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Account
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
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
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
        observeBalanceProjection()
        viewModelScope.launch {
            effectiveShortlistedCurrencies.collect { currencies ->
                _state.update {
                    it.copy(shortlistedCurrencies = currencies.filter { code -> code.uppercase() in POPULAR_CURRENCIES })
                }
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

    private fun observeBalanceProjection() {
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
            ) { defaultCurrency, profileId, _, exchangeRatesSnapshot, input ->
                val projection = input.accountId?.let { projectStoredBalances(it, input, profileId, exchangeRatesSnapshot) }
                _state.update {
                    it.copy(
                        defaultCurrency = defaultCurrency,
                        exchangeRates = exchangeRatesSnapshot.rates,
                        // A new account has no stored balance yet: the opening balances being typed
                        // are its closing balances.
                        systemClosingBalances = projection?.closingBalances ?: input.initialBalances.toBigDecimals(),
                        currentBalance = projection?.currentBalance ?: it.currentBalance,
                        currencyUsageMap = projection?.currencyUsage ?: it.currencyUsageMap
                    )
                }
            }.collect()
        }
    }

    private class BalanceProjection(
        val closingBalances: Map<String, BigDecimal>,
        val currentBalance: BigDecimal,
        val currencyUsage: Map<String, Boolean>
    )

    // Closing balance per currency = stored total - stored opening balance + typed opening balance
    // + pending adjustment, so the screen previews the balance the save would produce.
    private suspend fun projectStoredBalances(
        accountId: String,
        input: ProjectionInputs,
        profileId: Int,
        exchangeRates: ExchangeRateSnapshot
    ): BalanceProjection {
        val accountFilter = TransactionFilter(
            accountIds = setOf(accountId),
            timelineRange = TimelineRange.CustomRange(end = System.currentTimeMillis())
        )
        val stored = TransactionAggregationService.getSummaryByCurrency(queryService, profileId, accountFilter)
            .getOrDefault(emptyMap())
        val storedOpening = TransactionAggregationService.getSummaryByCurrency(
            queryService,
            profileId,
            accountFilter.copy(categoryIds = setOf(Category.SYSTEM_INITIAL_BALANCE))
        ).getOrDefault(emptyMap())

        val typedOpening = input.initialBalances.toBigDecimals()
        val currencies = stored.keys + typedOpening.keys + input.pendingAdjustmentDeltas.keys
        val closingBalances = currencies.associateWith { currency ->
            (stored[currency]?.totalAmount ?: BigDecimal.ZERO) -
                (storedOpening[currency]?.totalAmount ?: BigDecimal.ZERO) +
                (typedOpening[currency] ?: BigDecimal.ZERO) +
                (input.pendingAdjustmentDeltas[currency] ?: BigDecimal.ZERO)
        }
        return BalanceProjection(
            closingBalances = closingBalances,
            currentBalance = CurrencyService.calculateTotalInCurrency(closingBalances, input.primaryCurrency, exchangeRates),
            currencyUsage = stored.mapValues { it.value.transactionCount > 0 }
        )
    }

    private fun Map<String, String>.toBigDecimals(): Map<String, BigDecimal> =
        mapValues { (_, value) -> value.toBigDecimalOrNull() ?: BigDecimal.ZERO }

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

            is AccountEntryEvent.SetInitialBalance -> _state.update {
                it.copy(initialBalances = it.initialBalances + (event.currency to event.balance))
            }

            is AccountEntryEvent.SetIcon -> _state.update { it.copy(selectedIconName = event.icon) }
            is AccountEntryEvent.SetColor -> _state.update { it.copy(accentColorHex = event.color) }

            // Credit details events
            is AccountEntryEvent.SetCreditLimit -> _state.update { it.copy(creditLimitInput = event.limit) }
            is AccountEntryEvent.SetStatementClosingDay -> _state.update { it.copy(statementClosingDayInput = event.day) }
            is AccountEntryEvent.SetPaymentDueDay -> _state.update { it.copy(paymentDueDayInput = event.day) }
            is AccountEntryEvent.SetInterestRateApr -> _state.update { it.copy(interestRateAprInput = event.apr) }

            is AccountEntryEvent.OpenBalanceUpdate -> _state.update {
                it.copy(
                    showBalanceUpdateSheet = true,
                    balanceUpdateCurrency = event.currency,
                    balanceUpdateInputAmount = (it.systemClosingBalances[event.currency] ?: BigDecimal.ZERO)
                        .stripTrailingZeros().toPlainString(),
                    // Before the account exists there is no history to record against
                    balanceUpdateMode = if (it.accountId == null) BalanceUpdateMode.INITIAL_BALANCE
                    else BalanceUpdateMode.RECORD
                )
            }

            is AccountEntryEvent.SetBalanceUpdateMode -> _state.update { it.copy(balanceUpdateMode = event.mode) }
            is AccountEntryEvent.SetBalanceUpdateAmount -> _state.update {
                it.copy(balanceUpdateInputAmount = event.amount)
            }

            is AccountEntryEvent.OpenCalculatorForUpdate -> _state.update { state ->
                state.balanceUpdateCurrency?.let { currency ->
                    state.copy(
                        showCalculator = true,
                        activeBalanceCurrency = currency,
                        calculatorOriginalValue = state.balanceUpdateInputAmount
                    )
                } ?: state
            }

            is AccountEntryEvent.ConfirmBalanceUpdate -> _state.update { it.withConfirmedBalanceUpdate() }
            is AccountEntryEvent.CloseBalanceUpdate -> _state.update { it.withBalanceUpdateClosed() }

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
            val currencies = (state.allowedCurrencies - removed) + (newDisplayed - oldDisplayed)
            state.copy(
                allowedCurrencies = currencies,
                currenciesWithBalanceData = state.currenciesWithBalanceData - removed,
                primaryCurrency = primaryCurrencyFor(currencies, state.primaryCurrency),
                initialBalances = state.initialBalances +
                    removed.filter { it in state.initialBalances }.associateWith { "0" },
                pendingAdjustmentDeltas = state.pendingAdjustmentDeltas - removed,
                pendingCurrencyRemoval = emptySet()
            )
        }
    }

    // A lone currency is always the primary one; otherwise the current primary survives while allowed.
    private fun primaryCurrencyFor(allowed: Set<String>, current: String): String = when {
        allowed.isEmpty() -> ""
        allowed.size == 1 -> allowed.first()
        current.isNotEmpty() && current !in allowed -> allowed.first()
        else -> current
    }

    // Create/draft mode has no history, so a recorded amount there is the opening balance.
    private fun AccountEntryState.withConfirmedBalanceUpdate(): AccountEntryState {
        val currency = balanceUpdateCurrency ?: return this
        val newTotal = balanceUpdateInputAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val adjustment = newTotal - (systemClosingBalances[currency] ?: BigDecimal.ZERO)

        val updated = if (balanceUpdateMode == BalanceUpdateMode.INITIAL_BALANCE || accountId == null) {
            val opening = (initialBalances[currency]?.toBigDecimalOrNull() ?: BigDecimal.ZERO) + adjustment
            copy(initialBalances = initialBalances + (currency to opening.stripTrailingZeros().toPlainString()))
        } else {
            val pending = (pendingAdjustmentDeltas[currency] ?: BigDecimal.ZERO) + adjustment
            copy(pendingAdjustmentDeltas = pendingAdjustmentDeltas + (currency to pending))
        }
        return updated.withBalanceUpdateClosed()
    }

    private fun AccountEntryState.withBalanceUpdateClosed() =
        copy(showBalanceUpdateSheet = false, balanceUpdateCurrency = null)

    fun saveAccount(onSuccess: (AccountEntryResult) -> Unit) {
        val s = _state.value
        if (!s.canSave) return

        if (s.isDraftMode) {
            onSuccess(AccountEntryResult.Draft(s.toDraft()))
            return
        }

        // A second tap before the first save finishes would repeat its adjustments or create a duplicate account
        if (isSaveInFlight) return
        isSaveInFlight = true

        viewModelScope.launch {
            try {
                val profileId = settingsRepository.currentProfileId.first()
                val existing = s.existingAccount
                val savedAccountId = if (s.isEditMode && existing != null) {
                    updateAccount(s, existing, profileId)
                } else {
                    createAccount(s, profileId)
                }
                onSuccess(AccountEntryResult.Persisted(savedAccountId))
            } finally {
                isSaveInFlight = false
            }
        }
    }

    private fun AccountEntryState.toDraft() = AccountEntryDraft(
        name = accountName,
        structureType = structureType.name,
        primaryCurrency = primaryCurrency,
        allowedCurrencies = allowedCurrencies.toList(),
        initialBalances = initialBalances,
        iconName = selectedIconName,
        colorHex = accentColorHex
    )

    private suspend fun updateAccount(s: AccountEntryState, existing: Account, profileId: Int): String {
        accountUseCases.saveAccountEdits(
            account = existing.copy(
                name = s.accountName,
                structureType = s.structureType,
                currencyCode = s.primaryCurrency,
                allowedCurrencies = s.allowedCurrencies.toList(),
                colorHex = s.accentColorHex,
                icon = s.selectedIconName
            ),
            creditDetails = creditDetailsFor(s, existing.id),
            // Removed currencies are included at zero, so their stored balance is deleted
            openingBalances = (s.allowedCurrencies + s.initialBalances.keys).associateWith { currency ->
                s.initialBalances[currency]?.toBigDecimalOrNull() ?: BigDecimal.ZERO
            },
            pendingAdjustments = s.pendingAdjustmentDeltas,
            profileId = profileId
        )
        return existing.id
    }

    private suspend fun createAccount(s: AccountEntryState, profileId: Int): String {
        val accountId = UUID.randomUUID().toString()
        accountUseCases.createAccountWithInitialBalances(
            name = s.accountName,
            initialBalances = s.initialBalances,
            currency = s.primaryCurrency,
            structureType = s.structureType,
            colorHex = s.accentColorHex,
            icon = s.selectedIconName,
            allowedCurrencies = s.allowedCurrencies.toList(),
            profileId = profileId,
            accountId = accountId,
            creditDetails = creditDetailsFor(s, accountId)
        )
        return accountId
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

    private companion object {
        val POPULAR_CURRENCIES = setOf(
            "USD", "EUR", "JPY", "GBP", "AUD", "CAD", "CHF", "CNY", "KRW", "INR",
            "BRL", "RUB", "MXN", "SGD", "HKD", "TRY", "SEK", "NOK", "NZD", "ZAR"
        )
    }
}
