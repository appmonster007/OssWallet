package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.usecase.DeleteTransactionUseCase
import com.osscabinet.ossWallet.domain.usecase.GetTransactionsUseCase
import com.osscabinet.ossWallet.domain.usecase.InsertTransactionUseCase
import com.osscabinet.ossWallet.domain.usecase.UpdateTransactionUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionViewModel(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val insertTransactionUseCase: InsertTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val settingsDataStore: SettingsDataStore,
    private val categoryDao: com.osscabinet.ossWallet.data.local.dao.CategoryDao,
    private val accountRepository: com.osscabinet.ossWallet.domain.repository.AccountRepository
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _selectedAccountId = MutableStateFlow<String?>(null)
    val selectedAccountId = _selectedAccountId.asStateFlow()

    private val _dateRange = MutableStateFlow<Pair<Long?, Long?>>(null to null)
    val dateRange = _dateRange.asStateFlow()

    val categories = categoryDao.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactionsWithBalance: StateFlow<List<TransactionWithBalance>> = settingsDataStore.currentProfileId
        .flatMapLatest { profileId ->
            combine(
                getTransactionsUseCase(profileId),
                accountRepository.getAllAccounts(profileId),
                _selectedCategory,
                _selectedAccountId,
                _dateRange
            ) { allTransactions, accounts, category, accountId, range ->
                val accountMap = accounts.associateBy { it.id }

                // Calculate running balances per account
                val transactionsByAccount = allTransactions.sortedBy { it.timestamp }.groupBy { it.accountId }
                val allWithBalance = transactionsByAccount.flatMap { (accId, accountTransactions) ->
                    val initialBalance = accountMap[accId]?.initialBalance ?: 0.0
                    var currentBalance = initialBalance
                    accountTransactions.map { transaction ->
                        currentBalance += if (transaction.isIncome) transaction.amount else -transaction.amount
                        TransactionWithBalance(transaction, currentBalance)
                    }
                }

                // Apply filters
                allWithBalance.filter { item ->
                    val transaction = item.transaction
                    val matchesCategory = category == null || transaction.category.equals(category, ignoreCase = true)
                    val matchesAccount = accountId == null || transaction.accountId == accountId
                    val matchesDate = (range.first == null || transaction.timestamp >= range.first!!) &&
                            (range.second == null || transaction.timestamp <= range.second!!)
                    matchesCategory && matchesAccount && matchesDate
                }.sortedByDescending { it.transaction.timestamp }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val transactions: StateFlow<List<Transaction>> = transactionsWithBalance
        .map { list -> list.map { it.transaction } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setCategoryFilter(category: String?) {
        _selectedCategory.value = category
    }

    fun setAccountFilter(accountId: String?) {
        _selectedAccountId.value = accountId
    }

    fun setDateRangeFilter(start: Long?, end: Long?) {
        _dateRange.value = start to end
    }

    fun clearFilters() {
        _selectedCategory.value = null
        _selectedAccountId.value = null
        _dateRange.value = null to null
    }

    fun addTransaction(
        title: String,
        amount: Double,
        category: String,
        isIncome: Boolean,
        accountId: String,
        currency: String,
        categoryId: Long? = null,
        tags: List<String> = emptyList(),
        paymentMethod: String = "Cash",
        timestamp: Long = System.currentTimeMillis(),
        payee: String = "",
        note: String = "",
        linkedTransactionId: Long? = null
    ) {
        viewModelScope.launch {
            val profileId = settingsDataStore.currentProfileId.first()
            val transaction = Transaction(
                title = title,
                amount = amount,
                category = category,
                timestamp = timestamp,
                currency = currency,
                isIncome = isIncome,
                accountId = accountId,
                categoryId = categoryId,
                tags = tags,
                paymentMethod = paymentMethod,
                payee = payee,
                note = note,
                linkedTransactionId = linkedTransactionId
            )
            insertTransactionUseCase(transaction, profileId)
        }
    }

    fun updateTransaction(
        oldTransaction: Transaction,
        title: String,
        amount: Double,
        category: String,
        isIncome: Boolean,
        accountId: String,
        currency: String,
        tags: List<String> = emptyList(),
        paymentMethod: String = "Cash",
        timestamp: Long = System.currentTimeMillis(),
        payee: String = "",
        note: String = "",
        linkedTransactionId: Long? = null
    ) {
        viewModelScope.launch {
            val profileId = settingsDataStore.currentProfileId.first()
            val newTransaction = oldTransaction.copy(
                title = title,
                amount = amount,
                category = category,
                timestamp = timestamp,
                currency = currency,
                isIncome = isIncome,
                accountId = accountId,
                tags = tags,
                paymentMethod = paymentMethod,
                payee = payee,
                note = note,
                linkedTransactionId = linkedTransactionId
            )
            updateTransactionUseCase(oldTransaction, newTransaction, profileId)
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            deleteTransactionUseCase(id)
        }
    }
}

class TransactionViewModelFactory(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val insertTransactionUseCase: InsertTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val settingsDataStore: com.osscabinet.ossWallet.data.local.SettingsDataStore,
    private val categoryDao: com.osscabinet.ossWallet.data.local.dao.CategoryDao,
    private val accountRepository: com.osscabinet.ossWallet.domain.repository.AccountRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return TransactionViewModel(getTransactionsUseCase, insertTransactionUseCase, deleteTransactionUseCase, updateTransactionUseCase, settingsDataStore, categoryDao, accountRepository) as T
    }
}
