package com.osscabinet.ossWallet.ui.screens.accounts.entry

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.service.aggregation.AggregationService
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.ComputationRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import com.osscabinet.ossWallet.domain.usecase.AccountUseCases
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

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
                _state.update { it.copy(shortlistedCurrencies = currencies) }
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
                computationRepository.exchangeRates
            ) { defaultCurrency, transactions, exchangeRates ->
                val accountId = _state.value.accountId
                val currentBalance = if (_state.value.isEditMode && accountId != null) {
                    AggregationService.calculateAccountBalance(
                        accountId = accountId,
                        allTransactions = transactions,
                        accountCurrency = _state.value.primaryCurrency,
                        exchangeRates = exchangeRates
                    )
                } else BigDecimal.ZERO

                val currencyUsageMap = if (_state.value.isEditMode && accountId != null) {
                    transactions.filter { it.accountId == accountId }
                        .groupBy { it.currency }
                        .mapValues { it.value.isNotEmpty() }
                } else emptyMap()

                _state.update {
                    it.copy(
                        defaultCurrency = defaultCurrency,
                        exchangeRates = exchangeRates,
                        currentBalance = currentBalance,
                        currencyUsageMap = currencyUsageMap
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
            accountRepository.getAllAccountsSync(profileId).find { it.id == accountId }?.let { account ->
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
                        accentColorHex = account.colorHex
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
                    } else if (state.primaryCurrency.isNotEmpty() && !event.currencies.contains(state.primaryCurrency)) {
                        newPrimary = event.currencies.first()
                    }
                    state.copy(allowedCurrencies = event.currencies, primaryCurrency = newPrimary)
                }
            }
            is AccountEntryEvent.SetInitialBalance -> {
                _state.update { 
                    it.copy(initialBalances = it.initialBalances.toMutableMap().apply { put(event.currency, event.balance) })
                }
            }
            is AccountEntryEvent.SetIcon -> _state.update { it.copy(selectedIconName = event.icon) }
            is AccountEntryEvent.SetColor -> _state.update { it.copy(accentColorHex = event.color) }
            is AccountEntryEvent.ShowStructureSheet -> _state.update { it.copy(showStructureSheet = event.show) }
            is AccountEntryEvent.ShowIconSheet -> _state.update { it.copy(showIconSheet = event.show) }
            is AccountEntryEvent.ShowColorSheet -> _state.update { it.copy(showColorSheet = event.show) }
            is AccountEntryEvent.ShowCurrencySheet -> _state.update { it.copy(showCurrencySheet = event.show) }
            is AccountEntryEvent.OpenCalculator -> _state.update { it.copy(showCalculator = true, activeBalanceCurrency = event.currency) }
            is AccountEntryEvent.CloseCalculator -> _state.update { it.copy(showCalculator = false, activeBalanceCurrency = null) }
            is AccountEntryEvent.TriggerNavigation -> _state.update { it.copy(navTriggered = true) }
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
            accountUseCases.deleteAccount(id)
            onSuccess()
        }
    }
}
