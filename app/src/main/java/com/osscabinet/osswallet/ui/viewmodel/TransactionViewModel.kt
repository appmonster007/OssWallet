package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.data.local.TransactionSectionSettings
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.model.filter.FilterStack
import com.osscabinet.ossWallet.domain.model.filter.UnifiedFilter
import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
import com.osscabinet.ossWallet.domain.model.toDomain
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
    @Deprecated("Use recordsFilterStack instead for better filter composition.", ReplaceWith("recordsFilterStack"))
    val recordsFilter = _recordsFilter.asStateFlow()

    private val _accountDetailFilter = MutableStateFlow(TransactionFilter())
    @Deprecated("Use accountDetailFilterStack instead for better filter composition.", ReplaceWith("accountDetailFilterStack"))
    val accountDetailFilter = _accountDetailFilter.asStateFlow()

    // New FilterStack support for unified filtering
    private val _recordsFilterStack = MutableStateFlow(FilterStack.Empty)
    val recordsFilterStack = _recordsFilterStack.asStateFlow()

    private val _accountDetailFilterStack = MutableStateFlow(FilterStack.Empty)
    val accountDetailFilterStack = _accountDetailFilterStack.asStateFlow()

    val savedFilters = filterDao.getAllFilters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories = categoryDao.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val decimalPrecision: StateFlow<Int> = settingsDataStore.decimalPrecision
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2)

    val recordsSettings: StateFlow<TransactionSectionSettings> = settingsDataStore.recordsSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TransactionSectionSettings())

    @Deprecated("Use transactionsWithBalanceFromStack instead for FilterStack support.", ReplaceWith("transactionsWithBalanceFromStack"))
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

    // New FilterStack-based flow for unified filtering
    // Screens can migrate to use this flow gradually
    val transactionsWithBalanceFromStack: StateFlow<List<TransactionWithBalance>> = settingsDataStore.currentProfileId
        .flatMapLatest { profileId ->
            combine(
                _recordsFilterStack.flatMapLatest { transactionUseCases.getFilteredTransactions(profileId, it) },
                accountRepository.getAllAccounts(profileId),
                analyticsViewModel.uiState
            ) { filteredTransactions, accounts, analyticsState ->
                val rates = analyticsState.exchangeRates
                val precision = analyticsState.decimalPrecision

                transactionUseCases.getRecentTransactions(
                    allTransactions = filteredTransactions,
                    accounts = accounts,
                    exchangeRates = rates,
                    precision = precision,
                    limit = Int.MAX_VALUE
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

    @Deprecated("Use FilterStack-based methods instead. Migrate to addRecordsFilterStack() for better filter composition.", ReplaceWith("addRecordsFilterStack()"))
    fun updateRecordsFilter(newFilter: TransactionFilter) {
        _recordsFilter.value = newFilter
    }

    @Deprecated("Use FilterStack-based methods instead. Migrate to addAccountDetailFilterStack() for better filter composition.", ReplaceWith("addAccountDetailFilterStack()"))
    fun updateAccountDetailFilter(newFilter: TransactionFilter) {
        _accountDetailFilter.value = newFilter
    }

    @Deprecated("Use FilterStack-based methods instead. Migrate to addRecordsFilterStack() with AccountFilter.", ReplaceWith("addRecordsFilterStack(AccountFilter(...))"))
    fun setRecordsAccountFilter(accountId: String?) {
        _recordsFilter.value = _recordsFilter.value.copy(accountIds = if (accountId != null) listOf(accountId) else emptyList())
    }

    @Deprecated("Use FilterStack-based methods instead. Migrate to addRecordsFilterStack() with DateRangeFilter.", ReplaceWith("addRecordsFilterStack(DateRangeFilter(...))"))
    fun setRecordsDateRangeFilter(start: Long?, end: Long?) {
        _recordsFilter.value = _recordsFilter.value.copy(startDate = start, endDate = end)
    }

    @Deprecated("Use FilterStack-based methods instead. Migrate to addAccountDetailFilterStack() with DateRangeFilter.", ReplaceWith("addAccountDetailFilterStack(DateRangeFilter(...))"))
    fun setAccountDetailDateRangeFilter(start: Long?, end: Long?) {
        _accountDetailFilter.value = _accountDetailFilter.value.copy(startDate = start, endDate = end)
    }

    @Deprecated("Use FilterStack-based methods instead. Migrate to clearRecordsFilterStack().", ReplaceWith("clearRecordsFilterStack()"))
    fun clearRecordsFilters() {
        _recordsFilter.value = TransactionFilter()
    }

    @Deprecated("Use FilterStack-based methods instead. Migrate to clearAccountDetailFilterStack().", ReplaceWith("clearAccountDetailFilterStack()"))
    fun clearAccountDetailFilters() {
        _accountDetailFilter.value = TransactionFilter()
    }

    // ========== FilterStack Manipulation Methods ==========

    fun addRecordsFilterStack(filter: UnifiedFilter) {
        _recordsFilterStack.value = _recordsFilterStack.value.addFilter(filter)
    }

    fun addRecordsInstantFilter(filter: UnifiedFilter) {
        _recordsFilterStack.value = _recordsFilterStack.value.addInstantFilter(filter)
    }

    fun removeRecordsFilterStack(filter: UnifiedFilter) {
        _recordsFilterStack.value = _recordsFilterStack.value.removeFilter(filter)
    }

    fun clearRecordsFilterStack() {
        _recordsFilterStack.value = FilterStack.Empty
    }

    fun clearRecordsInstantFilters() {
        _recordsFilterStack.value = _recordsFilterStack.value.clearInstantFilters()
    }

    fun addAccountDetailFilterStack(filter: UnifiedFilter) {
        _accountDetailFilterStack.value = _accountDetailFilterStack.value.addFilter(filter)
    }

    fun addAccountDetailInstantFilter(filter: UnifiedFilter) {
        _accountDetailFilterStack.value = _accountDetailFilterStack.value.addInstantFilter(filter)
    }

    fun removeAccountDetailFilterStack(filter: UnifiedFilter) {
        _accountDetailFilterStack.value = _accountDetailFilterStack.value.removeFilter(filter)
    }

    fun clearAccountDetailFilterStack() {
        _accountDetailFilterStack.value = FilterStack.Empty
    }

    fun clearAccountDetailInstantFilters() {
        _accountDetailFilterStack.value = _accountDetailFilterStack.value.clearInstantFilters()
    }

    // ========== Load Saved Filters into Stack ==========

    /**
     * Loads a saved filter into the records filter stack.
     * Converts FilterEntity to TransactionFilter, then to FilterStack, and adds it.
     */
    fun loadSavedFilterToRecordsStack(filterEntity: com.osscabinet.ossWallet.data.local.entity.FilterEntity) {
        val transactionFilter = filterEntity.toDomain()
        val filterStack = FilterStack.fromTransactionFilter(transactionFilter)
        // Add all filters from the saved filter to the current stack
        _recordsFilterStack.value = filterStack.activeFilters.fold(_recordsFilterStack.value) { acc, filter ->
            acc.addFilter(filter)
        }
    }

    /**
     * Loads multiple saved filters into the records filter stack.
     */
    fun loadSavedFiltersToRecordsStack(filterEntities: List<com.osscabinet.ossWallet.data.local.entity.FilterEntity>) {
        filterEntities.forEach { loadSavedFilterToRecordsStack(it) }
    }

    /**
     * Loads a saved filter into the account detail filter stack.
     */
    fun loadSavedFilterToAccountDetailStack(filterEntity: com.osscabinet.ossWallet.data.local.entity.FilterEntity) {
        val transactionFilter = filterEntity.toDomain()
        val filterStack = FilterStack.fromTransactionFilter(transactionFilter)
        _accountDetailFilterStack.value = filterStack.activeFilters.fold(_accountDetailFilterStack.value) { acc, filter ->
            acc.addFilter(filter)
        }
    }

    /**
     * Loads multiple saved filters into the account detail filter stack.
     */
    fun loadSavedFiltersToAccountDetailStack(filterEntities: List<com.osscabinet.ossWallet.data.local.entity.FilterEntity>) {
        filterEntities.forEach { loadSavedFilterToAccountDetailStack(it) }
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
