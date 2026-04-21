package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.data.local.TransactionSectionSettings
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
import com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionViewModel(
    private val transactionUseCases: TransactionUseCases,
    private val settingsDataStore: SettingsDataStore,
    private val categoryDao: com.osscabinet.ossWallet.data.local.dao.CategoryDao,
    private val accountRepository: com.osscabinet.ossWallet.domain.repository.AccountRepository,
    private val filterDao: com.osscabinet.ossWallet.data.local.dao.FilterDao,
    private val analyticsViewModel: AnalyticsViewModel
) : ViewModel() {

    private val _recordsFilter = MutableStateFlow(TransactionFilter())
    val recordsFilter = _recordsFilter.asStateFlow()

    private val _accountDetailFilter = MutableStateFlow(TransactionFilter())
    val accountDetailFilter = _accountDetailFilter.asStateFlow()

    // Track the initial filter context from navigation
    private val _initialFilterContext = MutableStateFlow<RecordsFilterContext>(RecordsFilterContext.None)
    val initialFilterContext = _initialFilterContext.asStateFlow()

    val savedFilters = filterDao.getAllFilters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories = categoryDao.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val decimalPrecision: StateFlow<Int> = settingsDataStore.decimalPrecision
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2)

    val recordsSettings: StateFlow<TransactionSectionSettings> = settingsDataStore.recordsSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TransactionSectionSettings())

    val transactionsWithBalance: StateFlow<List<TransactionWithBalance>> = settingsDataStore.currentProfileId
        .flatMapLatest { profileId ->
            combine(
                _recordsFilter.flatMapLatest { transactionUseCases.getFilteredTransactions(profileId, it) },
                accountRepository.getAllAccounts(profileId),
                analyticsViewModel.uiState,
                _recordsFilter
            ) { filteredTransactions, accounts, analyticsState, currentFilter ->
                val rates = analyticsState.exchangeRates
                val precision = analyticsState.decimalPrecision

                transactionUseCases.getRecentTransactions(
                    allTransactions = filteredTransactions,
                    accounts = accounts,
                    exchangeRates = rates,
                    precision = precision,
                    limit = Int.MAX_VALUE // Use MAX_VALUE for the full list with balances
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val transactions: StateFlow<List<Transaction>> = transactionsWithBalance
        .map { list -> list.map { it.transaction } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateRecordsFilter(newFilter: TransactionFilter) {
        _recordsFilter.value = newFilter
    }

    fun updateAccountDetailFilter(newFilter: TransactionFilter) {
        _accountDetailFilter.value = newFilter
    }

    fun setRecordsAccountFilter(accountId: String?) {
        _recordsFilter.value = _recordsFilter.value.copy(accountIds = if (accountId != null) listOf(accountId) else emptyList())
    }

    fun setRecordsDateRangeFilter(start: Long?, end: Long?) {
        _recordsFilter.value = _recordsFilter.value.copy(startDate = start, endDate = end)
    }

    fun setAccountDetailDateRangeFilter(start: Long?, end: Long?) {
        _accountDetailFilter.value = _accountDetailFilter.value.copy(startDate = start, endDate = end)
    }

    fun clearRecordsFilters() {
        _recordsFilter.value = TransactionFilter()
    }

    fun clearAccountDetailFilters() {
        _accountDetailFilter.value = TransactionFilter()
    }

    /**
     * Initialize the filter from a navigation context
     * This should be called when the screen opens with a context parameter
     */
    fun initializeRecordsFilter(context: RecordsFilterContext) {
        _initialFilterContext.value = context
        val filter = when (context) {
            is RecordsFilterContext.None -> TransactionFilter()
            is RecordsFilterContext.ByAccount -> TransactionFilter(accountIds = listOf(context.accountId))
            is RecordsFilterContext.ByAccountAndType -> TransactionFilter(
                accountIds = listOf(context.accountId),
                isIncome = context.isIncome
            )
            is RecordsFilterContext.ByAccountAndCurrency -> TransactionFilter(
                accountIds = listOf(context.accountId),
                currencies = listOf(context.currency)
            )
            is RecordsFilterContext.Custom -> TransactionFilter(
                accountIds = context.accountIds,
                isIncome = context.isIncome,
                startDate = context.startDate,
                endDate = context.endDate,
                categoryIds = context.categoryIds.map { it.toLong() },
                currencies = context.currencies,
                paymentMethods = context.paymentMethods,
                queryNote = context.queryNote,
                minAmount = context.minAmount?.let { java.math.BigDecimal(it) },
                maxAmount = context.maxAmount?.let { java.math.BigDecimal(it) },
                queryPayee = context.queryPayee
            )
        }
        _recordsFilter.value = filter
    }

    /**
     * Check if the current filter has been modified from the initial context
     */
    fun hasModifiedFilter(): Boolean {
        val initialFilter = when (_initialFilterContext.value) {
            is RecordsFilterContext.None -> TransactionFilter()
            is RecordsFilterContext.ByAccount -> TransactionFilter(accountIds = listOf((_initialFilterContext.value as RecordsFilterContext.ByAccount).accountId))
            is RecordsFilterContext.ByAccountAndType -> {
                val ctx = _initialFilterContext.value as RecordsFilterContext.ByAccountAndType
                TransactionFilter(accountIds = listOf(ctx.accountId), isIncome = ctx.isIncome)
            }
            is RecordsFilterContext.ByAccountAndCurrency -> {
                val ctx = _initialFilterContext.value as RecordsFilterContext.ByAccountAndCurrency
                TransactionFilter(accountIds = listOf(ctx.accountId), currencies = listOf(ctx.currency))
            }
            is RecordsFilterContext.Custom -> {
                val ctx = _initialFilterContext.value as RecordsFilterContext.Custom
                TransactionFilter(
                    accountIds = ctx.accountIds,
                    isIncome = ctx.isIncome,
                    startDate = ctx.startDate,
                    endDate = ctx.endDate,
                    categoryIds = ctx.categoryIds.map { it.toLong() },
                    currencies = ctx.currencies,
                    paymentMethods = ctx.paymentMethods,
                    queryNote = ctx.queryNote,
                    minAmount = ctx.minAmount?.let { java.math.BigDecimal(it) },
                    maxAmount = ctx.maxAmount?.let { java.math.BigDecimal(it) },
                    queryPayee = ctx.queryPayee
                )
            }
        }
        return _recordsFilter.value != initialFilter
    }

    /**
     * Reset the filter to the initial context
     */
    fun resetToInitialFilter() {
        initializeRecordsFilter(_initialFilterContext.value)
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
            val id = transactionUseCases.insertTransaction(transaction, profileId)
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
            transactionUseCases.updateTransaction(oldTransaction, newTransaction, profileId)
        }
    }

    fun saveTransfer(
        sourceTransaction: Transaction,
        targetTransaction: Transaction,
        onComplete: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            val profileId = settingsDataStore.currentProfileId.first()
            transactionUseCases.saveTransfer(sourceTransaction, targetTransaction, profileId)
            onComplete?.invoke()
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            transactionUseCases.deleteTransaction(id)
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

    fun updateRecordsSettings(update: (TransactionSectionSettings) -> TransactionSectionSettings) {
        viewModelScope.launch {
            settingsDataStore.updateRecordsSettings(update)
        }
    }
}

class TransactionViewModelFactory(
    private val transactionUseCases: TransactionUseCases,
    private val settingsDataStore: SettingsDataStore,
    private val categoryDao: com.osscabinet.ossWallet.data.local.dao.CategoryDao,
    private val accountRepository: com.osscabinet.ossWallet.domain.repository.AccountRepository,
    private val filterDao: com.osscabinet.ossWallet.data.local.dao.FilterDao,
    private val analyticsViewModel: AnalyticsViewModel
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TransactionViewModel(
            transactionUseCases,
            settingsDataStore,
            categoryDao,
            accountRepository,
            filterDao,
            analyticsViewModel
        ) as T
    }
}
