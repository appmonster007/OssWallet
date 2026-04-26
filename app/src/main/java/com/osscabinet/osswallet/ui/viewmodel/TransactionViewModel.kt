package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.data.local.TransactionSectionSettings
import com.osscabinet.ossWallet.domain.model.FilterManager
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.dto.TransactionPrefillDTO
import com.osscabinet.ossWallet.domain.model.dto.TransactionRequest
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.model.forAccount
import com.osscabinet.ossWallet.domain.model.forCategory
import com.osscabinet.ossWallet.domain.model.forIncome
import com.osscabinet.ossWallet.domain.model.forExpense
import com.osscabinet.ossWallet.domain.model.withDateRange
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
    private val analyticsViewModel: AnalyticsViewModel,
    private val transactionManager: com.osscabinet.ossWallet.domain.service.TransactionManager
) : ViewModel() {

    // Replace multiple filter states with single FilterManager
    private val recordsFilterManager = FilterManager()
    
    // Expose merged filter as StateFlow directly from FilterManager
    val recordsFilter: StateFlow<TransactionFilter> = recordsFilterManager.mergedFilter
    
    // Keep account detail filter separate (used in AccountDetailScreen)
    private val _accountDetailFilter = MutableStateFlow(TransactionFilter())
    val accountDetailFilter = _accountDetailFilter.asStateFlow()

    // Track the initial filter context from navigation (for backward compatibility)
    private val _initialFilterContext = MutableStateFlow<RecordsFilterContext>(RecordsFilterContext.None)
    val initialFilterContext = _initialFilterContext.asStateFlow()

    // Track if records filter has been manually initialized (to prevent reinitialization from navigation)
    private val _recordsFilterInitialized = MutableStateFlow(false)
    val recordsFilterInitialized = _recordsFilterInitialized.asStateFlow()

    // Track revealed card IDs for swipe actions
    private val _revealedCardIdsList = MutableStateFlow<List<Long>>(emptyList())
    val revealedCardIdsList: StateFlow<List<Long>> = _revealedCardIdsList.asStateFlow()

    // Prefill DTO for clone functionality
    private val _prefillDto = MutableStateFlow<TransactionPrefillDTO?>(null)
    val prefillDto: StateFlow<TransactionPrefillDTO?> = _prefillDto.asStateFlow()

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
                recordsFilter.flatMapLatest { transactionUseCases.getFilteredTransactions(profileId, it) },
                accountRepository.getAllAccounts(profileId),
                analyticsViewModel.uiState,
                recordsFilter
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
        recordsFilterManager.setFilter("manual", newFilter)
        _recordsFilterInitialized.value = true
    }

    fun updateAccountDetailFilter(newFilter: TransactionFilter) {
        _accountDetailFilter.value = newFilter
    }

    fun setRecordsAccountFilter(accountId: String?) {
        if (accountId != null) {
            recordsFilterManager.setFilter("account", TransactionFilter().forAccount(accountId))
        } else {
            recordsFilterManager.removeFilter("account")
        }
    }

    fun setRecordsCategoryFilter(categoryId: Long?) {
        if (categoryId != null) {
            recordsFilterManager.setFilter("category", TransactionFilter().forCategory(categoryId))
        } else {
            recordsFilterManager.removeFilter("category")
        }
    }

    fun setRecordsDateRangeFilter(start: Long?, end: Long?) {
        if (start != null || end != null) {
            recordsFilterManager.setFilter("date", TransactionFilter().withDateRange(start, end))
        } else {
            recordsFilterManager.removeFilter("date")
        }
    }

    fun setIncomeTypeFilter(isIncome: Boolean?) {
        if (isIncome != null) {
            recordsFilterManager.setFilter("type", TransactionFilter().run {
                if (isIncome) forIncome() else forExpense()
            })
        } else {
            recordsFilterManager.removeFilter("type")
        }
    }

    fun setAccountDetailDateRangeFilter(start: Long?, end: Long?) {
        _accountDetailFilter.value = _accountDetailFilter.value.copy(startDate = start, endDate = end)
    }

    fun clearRecordsFilters() {
        recordsFilterManager.clear()
        _recordsFilterInitialized.value = false
    }

    fun clearAccountDetailFilters() {
        _accountDetailFilter.value = TransactionFilter()
    }

    /**
     * Initialize the filter from a navigation context
     * This should be called when the screen opens with a context parameter
     * Converts RecordsFilterContext to TransactionFilter and sets it in FilterManager
     */
    fun initializeRecordsFilter(context: RecordsFilterContext) {
        _initialFilterContext.value = context
        recordsFilterManager.clear()
        
        val filter = when (context) {
            is RecordsFilterContext.None -> TransactionFilter()
            is RecordsFilterContext.ByAccount -> TransactionFilter(accountIds = listOf(context.accountId))
            is RecordsFilterContext.ByAccountAndType -> TransactionFilter(
                accountIds = listOf(context.accountId),
                transactionType = if (context.isIncome) "income" else "expense"
            )
            is RecordsFilterContext.ByAccountAndCurrency -> TransactionFilter(
                accountIds = listOf(context.accountId),
                currencies = listOf(context.currency)
            )
            is RecordsFilterContext.Custom -> TransactionFilter(
                accountIds = context.accountIds,
                transactionType = context.isIncome?.let { if (it) "income" else "expense" },
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
        
        recordsFilterManager.setFilter("initial", filter)
        _recordsFilterInitialized.value = true
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
                TransactionFilter(accountIds = listOf(ctx.accountId), transactionType = if (ctx.isIncome) "income" else "expense")
            }
            is RecordsFilterContext.ByAccountAndCurrency -> {
                val ctx = _initialFilterContext.value as RecordsFilterContext.ByAccountAndCurrency
                TransactionFilter(accountIds = listOf(ctx.accountId), currencies = listOf(ctx.currency))
            }
            is RecordsFilterContext.Custom -> {
                val ctx = _initialFilterContext.value as RecordsFilterContext.Custom
                TransactionFilter(
                    accountIds = ctx.accountIds,
                    transactionType = ctx.isIncome?.let { if (it) "income" else "expense" },
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
        return recordsFilter.value != initialFilter
    }

    /**
     * Reset the filter to the initial context
     */
    fun resetToInitialFilter() {
        initializeRecordsFilter(_initialFilterContext.value)
    }

    fun addTransaction(
        request: TransactionRequest,
        onComplete: ((Long) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val profileId = settingsDataStore.currentProfileId.first()
            val transaction = Transaction(
                title = request.title,
                amount = request.amount,
                category = request.category,
                timestamp = request.timestamp,
                currency = request.currency,
                transactionType = request.transactionType,
                accountId = request.accountId,
                categoryId = request.categoryId,
                tags = request.tags,
                paymentMethod = request.paymentMethod,
                payee = request.payee,
                note = request.note,
                latitude = request.latitude,
                longitude = request.longitude,
                warrantyExpiration = request.warrantyExpiration,
                status = request.status
            )
            val id = transactionUseCases.insertTransaction(transaction, profileId)
            onComplete?.invoke(id)
        }
    }

    fun updateTransaction(
        oldTransaction: Transaction,
        request: TransactionRequest
    ) {
        viewModelScope.launch {
            val profileId = settingsDataStore.currentProfileId.first()
            val newTransaction = oldTransaction.copy(
                title = request.title,
                amount = request.amount,
                category = request.category,
                timestamp = request.timestamp,
                currency = request.currency,
                transactionType = request.transactionType,
                accountId = request.accountId,
                categoryId = request.categoryId,
                tags = request.tags,
                paymentMethod = request.paymentMethod,
                payee = request.payee,
                note = request.note,
                latitude = request.latitude,
                longitude = request.longitude,
                warrantyExpiration = request.warrantyExpiration,
                status = request.status
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

    // Swipe action methods
    fun onItemExpanded(cardId: Long) {
        if (_revealedCardIdsList.value.contains(cardId)) return
        _revealedCardIdsList.value = _revealedCardIdsList.value.toMutableList().also { list ->
            list.add(cardId)
        }
    }

    fun onItemCollapsed(cardId: Long) {
        if (!_revealedCardIdsList.value.contains(cardId)) return
        _revealedCardIdsList.value = _revealedCardIdsList.value.toMutableList().also { list ->
            list.remove(cardId)
        }
    }

    fun quickUpdateStatus(transactionId: Long, newStatus: com.osscabinet.ossWallet.domain.model.TransactionStatus) {
        viewModelScope.launch {
            val profileId = settingsDataStore.currentProfileId.first()
            if (transactionManager.updateStatus(transactionId, newStatus, profileId)) {
                onItemCollapsed(transactionId)
            }
        }
    }

    fun quickCloneTransaction(transactionId: Long) {
        viewModelScope.launch {
            val profileId = settingsDataStore.currentProfileId.first()
            if (transactionManager.cloneTransaction(transactionId, profileId) != null) {
                onItemCollapsed(transactionId)
            }
        }
    }

    /**
     * Set the clone transaction ID and fetch the prefill DTO.
     * This allows the UI to navigate to the entry screen with prefilled data.
     * Uses the use case to fetch the transaction directly, bypassing any filters.
     */
    fun setCloneTransactionId(transactionId: Long) {
        viewModelScope.launch {
            val transaction = transactionUseCases.getTransactionById(transactionId)
            if (transaction != null) {
                _prefillDto.value = TransactionPrefillDTO.fromTransaction(transaction, clonedFromTransactionId = transactionId)
            } else {
                _prefillDto.value = null
            }
        }
    }

    /**
     * Clear the prefill DTO.
     * Call this when opening the entry screen without clone data.
     */
    fun clearPrefillDto() {
        _prefillDto.value = null
    }

    fun quickDeleteTransaction(transactionId: Long) {
        viewModelScope.launch {
            deleteTransaction(transactionId)
            onItemCollapsed(transactionId)
        }
    }

    fun quickSplitTransaction(transactionId: Long) {
        // Note: Split transaction logic requires additional UI for split configuration
        // To implement properly, you would need:
        // 1. A split transaction dialog to configure how to split the transaction
        // 2. A data model for split transactions (e.g., TransactionSplit with amount, category, etc.)
        // 3. Logic to create multiple child transactions from the parent
        // 4. Update the original transaction to mark it as split
        // For now, this function collapses the item as a placeholder
        onItemCollapsed(transactionId)
    }

    fun toggleStatusClearedUncleared(transactionId: Long) {
        viewModelScope.launch {
            val profileId = settingsDataStore.currentProfileId.first()
            if (transactionManager.toggleStatus(transactionId, profileId) != null) {
                onItemCollapsed(transactionId)
            }
        }
    }

    // Link management methods
    fun addTransactionLink(transactionId: Long, linkedTransactionId: Long) {
        println("ViewModel: addTransactionLink called with txId=$transactionId, linkedId=$linkedTransactionId")
        viewModelScope.launch {
            transactionUseCases.addTransactionLink(transactionId, linkedTransactionId)
            println("ViewModel: addTransactionLink completed")
        }
    }

    fun removeTransactionLink(transactionId: Long, linkedTransactionId: Long) {
        println("ViewModel: removeTransactionLink called with txId=$transactionId, linkedId=$linkedTransactionId")
        viewModelScope.launch {
            transactionUseCases.removeTransactionLink(transactionId, linkedTransactionId)
            println("ViewModel: removeTransactionLink completed")
        }
    }

    fun clearTransactionLinks(transactionId: Long) {
        viewModelScope.launch {
            transactionUseCases.clearTransactionLinks(transactionId)
        }
    }

    fun getLinkedTransactionIds(transactionId: Long): kotlinx.coroutines.flow.Flow<List<Long>> {
        return transactionUseCases.getLinkedTransactionIds(transactionId)
    }

    fun getLinkedTransactions(transactionId: Long): kotlinx.coroutines.flow.Flow<List<com.osscabinet.ossWallet.domain.model.Transaction>> {
        println("ViewModel: getLinkedTransactions called for txId=$transactionId")
        return transactionUseCases.getLinkedTransactions(transactionId)
    }
}

class TransactionViewModelFactory(
    private val transactionUseCases: TransactionUseCases,
    private val settingsDataStore: SettingsDataStore,
    private val categoryDao: com.osscabinet.ossWallet.data.local.dao.CategoryDao,
    private val accountRepository: com.osscabinet.ossWallet.domain.repository.AccountRepository,
    private val filterDao: com.osscabinet.ossWallet.data.local.dao.FilterDao,
    private val analyticsViewModel: AnalyticsViewModel,
    private val transactionManager: com.osscabinet.ossWallet.domain.service.TransactionManager
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TransactionViewModel(
            transactionUseCases,
            settingsDataStore,
            categoryDao,
            accountRepository,
            filterDao,
            analyticsViewModel,
            transactionManager
        ) as T
    }
}
