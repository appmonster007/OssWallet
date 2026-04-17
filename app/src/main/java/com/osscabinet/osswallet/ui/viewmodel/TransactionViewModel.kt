package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.usecase.DeleteTransactionUseCase
import com.osscabinet.ossWallet.domain.usecase.GetTransactionsUseCase
import com.osscabinet.ossWallet.domain.usecase.InsertTransactionUseCase
import com.osscabinet.ossWallet.domain.usecase.UpdateTransactionUseCase
import com.osscabinet.ossWallet.ui.util.convertAmount
import com.osscabinet.ossWallet.ui.util.calculateTotalInCurrency
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionViewModel(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val insertTransactionUseCase: InsertTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val settingsDataStore: SettingsDataStore,
    private val categoryDao: com.osscabinet.ossWallet.data.local.dao.CategoryDao,
    private val accountRepository: com.osscabinet.ossWallet.domain.repository.AccountRepository,
    private val analyticsViewModel: AnalyticsViewModel
) : ViewModel() {

    private val _filter = MutableStateFlow(TransactionFilter())
    val filter = _filter.asStateFlow()

    val categories = categoryDao.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val decimalPrecision: StateFlow<Int> = settingsDataStore.decimalPrecision
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2)

    val transactionsWithBalance: StateFlow<List<TransactionWithBalance>> = settingsDataStore.currentProfileId
        .flatMapLatest { profileId ->
            combine(
                _filter.flatMapLatest { getTransactionsUseCase.filtered(profileId, it) },
                accountRepository.getAllAccounts(profileId),
                analyticsViewModel.uiState,
                _filter
            ) { filteredTransactions, accounts, analyticsState, currentFilter ->
                val accountMap = accounts.associateBy { it.id }
                val rates = analyticsState.exchangeRates
                val precision = analyticsState.decimalPrecision
                val baseCurrency = analyticsState.selectedCurrency

                // Group by account to calculate running balance correctly per account
                val transactionsByAccount = filteredTransactions.groupBy { it.accountId }
                
                val allWithBalance = transactionsByAccount.flatMap { (accountId, accountTransactions) ->
                    val currencyBalances = mutableMapOf<String, BigDecimal>()
                    
                    accountTransactions.sortedBy { it.timestamp }.map { transaction ->
                        val currentBalance = currencyBalances.getOrDefault(transaction.currency, BigDecimal.ZERO)
                        val balanceBefore = currentBalance
                        val newBalance = if (transaction.isIncome) {
                            currentBalance.add(transaction.amount)
                        } else {
                            currentBalance.subtract(transaction.amount)
                        }
                        currencyBalances[transaction.currency] = newBalance
                        
                        TransactionWithBalance(
                            transaction = transaction,
                            balanceBefore = balanceBefore,
                            balanceAfter = newBalance
                        )
                    }
                }

                allWithBalance.sortedByDescending { it.transaction.timestamp }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val transactions: StateFlow<List<Transaction>> = transactionsWithBalance
        .map { list -> list.map { it.transaction } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateFilter(newFilter: TransactionFilter) {
        _filter.value = newFilter
    }

    fun setCategoryFilter(category: String?) {
        // Find categoryId for the name if possible, or just update the name filter if we add it
        // For now, let's update the categoryIds if we have a way to resolve it, 
        // but it's better to use updateFilter directly from UI
    }

    fun setAccountFilter(accountId: String?) {
        _filter.value = _filter.value.copy(accountIds = if (accountId != null) listOf(accountId) else emptyList())
    }

    fun setDateRangeFilter(start: Long?, end: Long?) {
        _filter.value = _filter.value.copy(startDate = start, endDate = end)
    }

    fun clearFilters() {
        _filter.value = TransactionFilter()
    }

    fun addTransaction(
        title: String,
        amount: BigDecimal,
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
        linkedTransactionId: Long? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        warrantyExpiration: Long? = null,
        onComplete: ((Long) -> Unit)? = null
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
                linkedTransactionId = linkedTransactionId,
                latitude = latitude,
                longitude = longitude,
                warrantyExpiration = warrantyExpiration
            )
            val id = insertTransactionUseCase(transaction, profileId)
            onComplete?.invoke(id)
        }
    }

    fun updateTransaction(
        oldTransaction: Transaction,
        title: String,
        amount: BigDecimal,
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
        linkedTransactionId: Long? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        warrantyExpiration: Long? = null
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
                categoryId = categoryId,
                tags = tags,
                paymentMethod = paymentMethod,
                payee = payee,
                note = note,
                linkedTransactionId = linkedTransactionId,
                latitude = latitude,
                longitude = longitude,
                warrantyExpiration = warrantyExpiration
            )
            updateTransactionUseCase(oldTransaction, newTransaction, profileId)
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            deleteTransactionUseCase(id)
        }
    }

    fun upsertCategory(
        id: Long = 0,
        name: String,
        iconName: String,
        colorHex: Int,
        parentId: Long? = null,
        isDefault: Boolean = false
    ) {
        viewModelScope.launch {
            categoryDao.insertCategory(
                com.osscabinet.ossWallet.data.local.entity.CategoryEntity(
                    id = id,
                    name = name,
                    iconName = iconName,
                    colorHex = colorHex,
                    parentId = parentId,
                    isDefault = isDefault
                )
            )
        }
    }

    fun deleteCategory(category: com.osscabinet.ossWallet.data.local.entity.CategoryEntity) {
        viewModelScope.launch {
            categoryDao.deleteCategory(category)
        }
    }
}

class TransactionViewModelFactory(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val insertTransactionUseCase: InsertTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val settingsDataStore: SettingsDataStore,
    private val categoryDao: com.osscabinet.ossWallet.data.local.dao.CategoryDao,
    private val accountRepository: com.osscabinet.ossWallet.domain.repository.AccountRepository,
    private val analyticsViewModel: AnalyticsViewModel
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TransactionViewModel(getTransactionsUseCase, insertTransactionUseCase, deleteTransactionUseCase, updateTransactionUseCase, settingsDataStore, categoryDao, accountRepository, analyticsViewModel) as T
    }
}
