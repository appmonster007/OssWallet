package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.*

class AccountViewModel(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val defaultCurrency: StateFlow<String> = settingsRepository.currency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "USD")

    val accounts: StateFlow<List<Account>> = settingsRepository.currentProfileId
        .flatMapLatest { profileId -> accountRepository.getAllAccounts(profileId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addAccount(
        name: String,
        initialBalance: BigDecimal,
        currencyCode: String,
        structureType: String,
        colorHex: String = "#004D5F",
        icon: String = "AccountBalance",
        allowedCurrencies: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            val profileId = settingsRepository.currentProfileId.first()
            val accountId = UUID.randomUUID().toString()
            accountRepository.insertAccount(
                Account(
                    id = accountId,
                    name = name,
                    currencyCode = currencyCode,
                    structureType = structureType,
                    colorHex = colorHex,
                    icon = icon,
                    allowedCurrencies = allowedCurrencies,
                    isArchived = false,
                    displayOrder = accounts.value.size
                ),
                profileId = profileId
            )

            // Create initial balance transaction if non-zero
            if (initialBalance.signum() != 0) {
                val transactionType = if (initialBalance.signum() > 0) "income" else "expense"
                val initialTransaction = Transaction(
                    title = "Initial Balance",
                    amount = initialBalance.abs(),
                    category = "Adjustment",
                    timestamp = System.currentTimeMillis(),
                    currency = currencyCode,
                    transactionType = transactionType,
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
        initialBalances: Map<String, String>,
        currency: String,
        structureType: String,
        colorHex: String,
        icon: String,
        allowedCurrencies: List<String>
    ) {
        viewModelScope.launch {
            val profileId = settingsRepository.currentProfileId.first()
            val accountId = UUID.randomUUID().toString()
            
            // 1. Create Account
            val account = Account(
                id = accountId,
                name = name,
                currencyCode = currency,
                structureType = structureType,
                colorHex = colorHex,
                icon = icon,
                allowedCurrencies = allowedCurrencies,
                isArchived = false,
                displayOrder = accounts.value.size
            )
            accountRepository.insertAccount(account, profileId)
            
            // 2. Create initial balance transactions for each currency
            allowedCurrencies.forEach { currencyCode ->
                val balanceStr = initialBalances[currencyCode]
                val balance = if (!balanceStr.isNullOrBlank()) {
                    try { BigDecimal(balanceStr) } catch (e: Exception) { BigDecimal.ZERO }
                } else BigDecimal.ZERO
                
                if (balance.signum() != 0) {
                    val transactionType = if (balance.signum() > 0) "income" else "expense"
                    val initialTransaction = Transaction(
                        title = "Initial Balance",
                        amount = balance.abs(),
                        category = "Adjustment",
                        timestamp = System.currentTimeMillis(),
                        currency = currencyCode,
                        transactionType = transactionType,
                        accountId = accountId,
                        note = "Initial balance for $name in $currencyCode",
                        paymentMethod = structureType,
                        payee = "System"
                    )
                    transactionRepository.insertTransaction(initialTransaction, profileId)
                }
            }
        }
    }

    fun updateAccount(account: Account) {
        viewModelScope.launch {
            val profileId = settingsRepository.currentProfileId.first()
            accountRepository.insertAccount(account, profileId)
        }
    }

    fun deleteAccount(accountId: String) {
        viewModelScope.launch {
            accountRepository.deleteAccount(accountId)
        }
    }

    fun updateAccountsOrder(updatedAccounts: List<Account>) {
        viewModelScope.launch {
            val profileId = settingsRepository.currentProfileId.first()
            val accountsWithOrder = updatedAccounts.mapIndexed { index, account ->
                account.copy(displayOrder = index)
            }
            accountRepository.updateAccounts(accountsWithOrder, profileId)
        }
    }

    fun toggleArchiveAccount(account: Account) {
        viewModelScope.launch {
            val profileId = settingsRepository.currentProfileId.first()
            accountRepository.insertAccount(account.copy(isArchived = !account.isArchived), profileId)
        }
    }
}

class AccountViewModelFactory(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val settingsRepository: SettingsRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AccountViewModel(
            accountRepository,
            transactionRepository,
            settingsRepository
        ) as T
    }
}
