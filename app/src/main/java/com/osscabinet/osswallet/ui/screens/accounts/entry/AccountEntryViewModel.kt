package com.osscabinet.ossWallet.ui.screens.accounts.entry

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.ComputationRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import com.osscabinet.ossWallet.domain.usecase.AccountUseCases
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class AccountEntryViewModel(
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
                _state.update {
                    it.copy(
                        defaultCurrency = defaultCurrency,
                        allTransactions = transactions,
                        exchangeRates = exchangeRates
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
