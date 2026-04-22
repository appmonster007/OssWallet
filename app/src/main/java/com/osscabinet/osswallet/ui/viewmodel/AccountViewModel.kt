package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModel(
    private val repository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val defaultCurrency: StateFlow<String> = settingsDataStore.currency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "USD")

    val accounts: StateFlow<List<Account>> = settingsDataStore.currentProfileId
        .flatMapLatest { profileId ->
            repository.getAllAccounts(profileId)
        }
        .map { it.sortedBy { account -> account.displayOrder } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // We've moved default account creation to onboarding
    }

    private fun seedDefaultAccount() {
        // Logic removed as it's now handled in OnboardingScreen explicitly with user input
    }

    fun addAccount(
        name: String,
        balance: BigDecimal,
        currency: String,
        structureType: String,
        colorHex: String,
        icon: String,
        allowedCurrencies: List<String>
    ) {
        viewModelScope.launch {
            val profileId = settingsDataStore.currentProfileId.first()
            val accountId = UUID.randomUUID().toString()
            val account = Account(
                id = accountId,
                name = name,
                balance = BigDecimal.ZERO, // Will be updated by transactions
                currencyCode = currency,
                structureType = structureType,
                colorHex = colorHex,
                icon = icon,
                allowedCurrencies = allowedCurrencies,
                isArchived = false,
                displayOrder = accounts.value.size
            )
            repository.insertAccount(account, profileId)

            if (balance > BigDecimal.ZERO) {
                val initialTransaction = Transaction(
                    title = "Initial Balance",
                    amount = balance,
                    category = "Adjustment",
                    timestamp = System.currentTimeMillis(),
                    currency = currency,
                    transactionType = "income",
                    accountId = accountId,
                    note = "Initial balance for $name",
                    paymentMethod = structureType,
                    payee = "System"
                )
                transactionRepository.insertTransaction(initialTransaction, profileId)
            }
        }
    }

    fun addAccountWithInitialBalances(
        name: String,
        initialBalances: Map<String, String>, // currency -> balance string
        currency: String,
        structureType: String,
        colorHex: String,
        icon: String,
        allowedCurrencies: List<String>
    ) {
        viewModelScope.launch {
            val profileId = settingsDataStore.currentProfileId.first()
            val accountId = UUID.randomUUID().toString()
            val account = Account(
                id = accountId,
                name = name,
                balance = BigDecimal.ZERO, // Will be updated by transactions
                currencyCode = currency,
                structureType = structureType,
                colorHex = colorHex,
                icon = icon,
                allowedCurrencies = allowedCurrencies,
                isArchived = false,
                displayOrder = accounts.value.size
            )
            repository.insertAccount(account, profileId)

            // Create initial balance transactions for all selected currencies
            allowedCurrencies.forEach { currencyCode ->
                val balanceStr = initialBalances[currencyCode] ?: "0"
                val balance = balanceStr.toBigDecimalOrNull() ?: BigDecimal.ZERO
                val initialTransaction = Transaction(
                    title = "Initial Balance",
                    amount = balance,
                    category = "Adjustment",
                    timestamp = System.currentTimeMillis(),
                    currency = currencyCode,
                    transactionType = "income",
                    accountId = accountId,
                    note = "Initial balance for $name in $currencyCode",
                    paymentMethod = structureType,
                    payee = "System"
                )
                transactionRepository.insertTransaction(initialTransaction, profileId)
            }
        }
    }

    fun updateAccount(account: Account) {
        viewModelScope.launch {
            val profileId = settingsDataStore.currentProfileId.first()
            repository.insertAccount(account, profileId)
        }
    }

    fun updateBalance(accountId: String, amount: BigDecimal) {
        viewModelScope.launch {
            repository.updateBalance(accountId, amount)
        }
    }

    fun deleteAccount(accountId: String) {
        viewModelScope.launch {
            repository.deleteAccount(accountId)
        }
    }

    fun updateAccountsOrder(orderedAccounts: List<Account>) {
        viewModelScope.launch {
            val profileId = settingsDataStore.currentProfileId.first()
            val entities = orderedAccounts.mapIndexed { index, account ->
                account.copy(displayOrder = index)
            }
            repository.updateAccounts(entities, profileId)
        }
    }

    fun toggleArchiveAccount(account: Account) {
        viewModelScope.launch {
            val profileId = settingsDataStore.currentProfileId.first()
            repository.insertAccount(account.copy(isArchived = !account.isArchived), profileId)
        }
    }
}

class AccountViewModelFactory(
    private val repository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AccountViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AccountViewModel(repository, transactionRepository, settingsDataStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
