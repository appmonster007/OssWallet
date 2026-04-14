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
        seedDefaultAccount()
    }

    private fun seedDefaultAccount() {
        viewModelScope.launch {
            settingsDataStore.currentProfileId.collectLatest { profileId ->
                val existingAccounts = repository.getAllAccounts(profileId).first()
                if (existingAccounts.isEmpty()) {
                    val defaultCurrency = settingsDataStore.currency.first()
                    val cashAccount = Account(
                        id = "default-cash-id",
                        name = "Cash",
                        balance = BigDecimal.ZERO,
                        initialBalance = BigDecimal.ZERO,
                        currencyCode = defaultCurrency,
                        structureType = "Cash",
                        colorHex = "#4CAF50",
                        icon = "Payments",
                        allowedCurrencies = listOf(defaultCurrency),
                        isArchived = false,
                        displayOrder = 0
                    )
                    repository.insertAccount(cashAccount, profileId)
                }
            }
        }
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
                initialBalance = BigDecimal.ZERO, // We use transactions for initial balance now
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
                    isIncome = true,
                    accountId = accountId,
                    note = "Initial balance for $name",
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
