package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.FilterManager
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import com.osscabinet.ossWallet.domain.model.dto.TransactionPrefillDTO
import com.osscabinet.ossWallet.domain.model.dto.TemplatePrefillDTO
import com.osscabinet.ossWallet.domain.model.dto.PlannedPaymentPrefillDTO
import com.osscabinet.ossWallet.domain.model.dto.TransactionRequest
import com.osscabinet.ossWallet.domain.model.forAccount
import com.osscabinet.ossWallet.domain.model.forCategory
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.ComputationRepository
import com.osscabinet.ossWallet.domain.repository.FilterRepository
import com.osscabinet.ossWallet.domain.repository.PlannedPaymentRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TemplateRepository
import com.osscabinet.ossWallet.domain.service.management.TransactionManager
import com.osscabinet.ossWallet.domain.service.discovery.PatternDiscoveryService
import com.osscabinet.ossWallet.domain.service.discovery.DiscoveredPattern
import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
import com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionViewModel(
    private val transactionUseCases: TransactionUseCases,
    settingsRepository: SettingsRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val filterRepository: FilterRepository,
    private val computationRepository: ComputationRepository,
    private val transactionManager: TransactionManager,
    private val plannedPaymentRepository: PlannedPaymentRepository,
    private val templateRepository: TemplateRepository
) : BaseViewModel(settingsRepository) {

    // Replace multiple filter states with single FilterManager
    private val recordsFilterManager = FilterManager()

    // Expose merged filter as StateFlow directly from FilterManager
    val recordsFilter: StateFlow<TransactionFilter> = recordsFilterManager.mergedFilter

    // Keep account detail filter separate (used in AccountDetailScreen)
    private val _accountDetailFilter = MutableStateFlow(TransactionFilter())
    val accountDetailFilter = _accountDetailFilter.asStateFlow()

    // Track the initial filter context from navigation (for backward compatibility)
    private val _initialFilterContext =
        MutableStateFlow<RecordsFilterContext>(RecordsFilterContext.None)
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

    private val _templatePrefill = MutableStateFlow<TemplatePrefillDTO?>(null)
    val templatePrefill = _templatePrefill.asStateFlow()

    private val _plannedPaymentPrefill = MutableStateFlow<PlannedPaymentPrefillDTO?>(null)
    val plannedPaymentPrefill = _plannedPaymentPrefill.asStateFlow()

    val savedFilters = filterRepository.getAllFilters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories = categoryRepository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val decimalPrecision: StateFlow<Int> = settingsRepository.decimalPrecision
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2)

    val use24HourFormat: StateFlow<Boolean> = settingsRepository.use24HourFormat
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val exchangeRates: StateFlow<Map<String, BigDecimal>> = computationRepository.exchangeRates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val recordsSettings: StateFlow<com.osscabinet.ossWallet.data.local.TransactionSectionSettings> =
        settingsRepository.recordsSettings
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                com.osscabinet.ossWallet.data.local.TransactionSectionSettings()
            )

    val transactionsWithBalance: StateFlow<List<TransactionWithBalance>> =
        settingsRepository.currentProfileId
            .flatMapLatest { profileId ->
                combine(
                    recordsFilter.flatMapLatest {
                        transactionUseCases.getFilteredTransactions(
                            profileId,
                            it
                        )
                    },
                    accountRepository.getAllAccounts(profileId),
                    computationRepository.exchangeRates,
                    computationRepository.hasLoadedExchangeRates,
                    recordsFilter
                ) { filteredTransactions, accounts, rates, hasLoadedExchangeRates, currentFilter ->
                    if (!hasLoadedExchangeRates) return@combine emptyList<TransactionWithBalance>()

                    val precision = decimalPrecision.value

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

    /**
     * Smart discovery of recurring patterns based on all historical transactions.
     * Runs completely offline using heuristic analysis.
     * Filters out patterns that already exist as both Template and Planned Payment.
     */
    val discoveredPatterns: StateFlow<List<DiscoveredPattern>> = settingsRepository.currentProfileId
        .flatMapLatest { profileId ->
            combine(
                transactionUseCases.getFilteredTransactions(
                    profileId,
                    TransactionFilter(timelineRange = TimelineRange.AllTime)
                ),
                templateRepository.getAllTemplates(),
                plannedPaymentRepository.getAllPlannedPayments(profileId)
            ) { transactions, templates, plannedPayments ->
                val rawPatterns = PatternDiscoveryService.discoverPatterns(transactions)
                
                rawPatterns.map { pattern ->
                    val existsAsTemplate = templates.any { 
                        it.name.equals(pattern.title, ignoreCase = true) || 
                        it.payee?.equals(pattern.payee, ignoreCase = true) == true
                    }
                    val existsAsPlannedPayment = plannedPayments.any { 
                        it.title.equals(pattern.title, ignoreCase = true) || 
                        it.payee.equals(pattern.payee, ignoreCase = true)
                    }
                    
                    pattern.copy(
                        existsAsTemplate = existsAsTemplate,
                        existsAsPlannedPayment = existsAsPlannedPayment
                    )
                }.filter { !it.existsAsTemplate || !it.existsAsPlannedPayment }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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
        recordsFilterManager.setFilter("date", TransactionFilter(
            timelineRange = TimelineRange.CustomRange(start, end)
        ))
    }

    fun updateRecordsTimelineRange(range: TimelineRange) {
        recordsFilterManager.setFilter("date", TransactionFilter(timelineRange = range))
        _recordsFilterInitialized.value = true
    }

    fun setIncomeTypeFilter(type: String?) {
        if (type != null) {
            recordsFilterManager.setFilter("type", TransactionFilter(transactionType = type))
        } else {
            recordsFilterManager.removeFilter("type")
        }
    }

    fun setAccountDetailDateRangeFilter(start: Long?, end: Long?) {
        _accountDetailFilter.value =
            _accountDetailFilter.value.copy(timelineRange = TimelineRange.CustomRange(start, end))
    }

    fun clearRecordsFilters() {
        recordsFilterManager.clear()
        _recordsFilterInitialized.value = false
    }

    fun clearAccountDetailFilters() {
        _accountDetailFilter.value = TransactionFilter()
    }

    /**
     * Converts RecordsFilterContext to TransactionFilter
     */
    private fun RecordsFilterContext.toTransactionFilter(): TransactionFilter = when (this) {
        is RecordsFilterContext.None -> TransactionFilter()
        is RecordsFilterContext.ByAccount -> TransactionFilter(accountIds = listOf(accountId))
        is RecordsFilterContext.ByAccountAndType -> TransactionFilter(
            accountIds = listOf(accountId),
            transactionType = if (isIncome) "income" else "expense"
        )

        is RecordsFilterContext.ByAccountAndCurrency -> TransactionFilter(
            accountIds = listOf(accountId),
            currencies = listOf(currency)
        )

        is RecordsFilterContext.Custom -> TransactionFilter(
            accountIds = accountIds,
            transactionType = transactionType,
            timelineRange = TimelineRange.CustomRange(startDate, endDate),
            categoryIds = categoryIds.map { it.toLong() },
            currencies = currencies,
            paymentMethods = paymentMethods,
            queryNote = queryNote,
            minAmount = minAmount?.let { BigDecimal(it) },
            maxAmount = maxAmount?.let { BigDecimal(it) },
            queryPayee = queryPayee,
            tags = tags
        )
    }

    /**
     * Initialize the filter from a navigation context
     * This should be called when the screen opens with a context parameter
     * Converts RecordsFilterContext to TransactionFilter and sets it in FilterManager
     */
    fun initializeRecordsFilter(context: RecordsFilterContext) {
        _initialFilterContext.value = context
        recordsFilterManager.clear()
        recordsFilterManager.setFilter("initial", context.toTransactionFilter())
        _recordsFilterInitialized.value = true
    }

    /**
     * Check if the current filter has been modified from the initial context
     */
    fun hasModifiedFilter(): Boolean {
        val initialFilter = _initialFilterContext.value.toTransactionFilter()
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
            withProfile { profileId ->
                val id = transactionManager.handleSaveTransaction(null, request, profileId)
                onComplete?.invoke(id)
            }
        }
    }

    fun updateTransaction(
        oldTransaction: Transaction,
        request: TransactionRequest
    ) {
        viewModelScope.launch {
            withProfile { profileId ->
                transactionManager.handleSaveTransaction(oldTransaction, request, profileId)
            }
        }
    }

    fun saveTransfer(
        sourceTransaction: Transaction,
        targetTransaction: Transaction,
        onComplete: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            withProfile { profileId ->
                transactionUseCases.saveTransfer(sourceTransaction, targetTransaction, profileId)
                onComplete?.invoke()
            }
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
            categoryRepository.upsertCategory(
                com.osscabinet.ossWallet.domain.model.Category(
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

    fun deleteCategory(category: com.osscabinet.ossWallet.domain.model.Category) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(category)
        }
    }

    fun updateRecordsSettings(update: (com.osscabinet.ossWallet.data.local.TransactionSectionSettings) -> com.osscabinet.ossWallet.data.local.TransactionSectionSettings) {
        viewModelScope.launch {
            settingsRepository.updateRecordsSettings(update)
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

    fun quickUpdateStatus(
        transactionId: Long,
        newStatus: com.osscabinet.ossWallet.domain.model.TransactionStatus
    ) {
        viewModelScope.launch {
            withProfile { profileId ->
                if (transactionManager.updateStatus(transactionId, newStatus, profileId)) {
                    onItemCollapsed(transactionId)
                }
            }
        }
    }

    fun quickCloneTransaction(transactionId: Long) {
        viewModelScope.launch {
            withProfile { profileId ->
                if (transactionManager.cloneTransaction(transactionId, profileId) != null) {
                    onItemCollapsed(transactionId)
                }
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
                var prefill = TransactionPrefillDTO.fromTransaction(
                    transaction,
                    clonedFromId = transactionId
                )

                // If it's a transfer, try to find the linked transaction to get the target account and rate
                if (transaction.categoryId == 1003L) {
                    val linkedTxs = transactionUseCases.getLinkedTransactions(transactionId).first()
                    val targetTx = linkedTxs.find { 
                        it.categoryId == 1003L && 
                        it.accountId != transaction.accountId &&
                        it.transactionType == "income" 
                    }
                    if (targetTx != null) {
                        val rate = if (transaction.amount.signum() != 0) {
                            targetTx.amount.abs().divide(transaction.amount.abs(), 6, java.math.RoundingMode.HALF_UP)
                        } else BigDecimal.ONE
                        
                        prefill = prefill.copy(
                            targetAccountId = targetTx.accountId,
                            targetCurrency = targetTx.currency,
                            exchangeRate = rate.toPlainString()
                        )
                    }
                }
                
                _prefillDto.value = prefill
            } else {
                _prefillDto.value = null
            }
        }
    }

    /**
     * Set the prefill DTO from a planned payment.
     * This is used when manually confirming a planned payment.
     */
    fun setPlannedPaymentPrefill(plannedPaymentId: Long, scheduledDate: Long? = null) {
        viewModelScope.launch {
            val payment = plannedPaymentRepository.getById(plannedPaymentId)
            if (payment != null) {
                _prefillDto.value = TransactionPrefillDTO.fromPlannedPayment(payment, scheduledDate)
            } else {
                _prefillDto.value = null
            }
        }
    }

    /**
     * Set the prefill DTO from a template.
     * This is used when creating a transaction from a template.
     */
    fun setTemplatePrefill(templateId: Long) {
        viewModelScope.launch {
            val templates = templateRepository.getAllTemplates().first()
            val template = templates.find { it.id == templateId }
            if (template != null) {
                _prefillDto.value = TransactionPrefillDTO.fromTemplate(template)
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
            withProfile { profileId ->
                if (transactionManager.toggleStatus(transactionId, profileId) != null) {
                    onItemCollapsed(transactionId)
                }
            }
        }
    }

    // Link management methods
    fun addTransactionLink(transactionId: Long, linkedTransactionId: Long) {
        viewModelScope.launch {
            transactionUseCases.addTransactionLink(transactionId, linkedTransactionId)
        }
    }

    fun removeTransactionLink(transactionId: Long, linkedTransactionId: Long) {
        viewModelScope.launch {
            transactionUseCases.removeTransactionLink(transactionId, linkedTransactionId)
        }
    }

    fun clearTransactionLinks(transactionId: Long) {
        viewModelScope.launch {
            transactionUseCases.clearTransactionLinks(transactionId)
        }
    }

    fun getLinkedTransactionIds(transactionId: Long): Flow<List<Long>> {
        return transactionUseCases.getLinkedTransactionIds(transactionId)
    }

    fun getLinkedTransactions(transactionId: Long): Flow<List<Transaction>> {
        return transactionUseCases.getLinkedTransactions(transactionId)
    }

    fun saveTemplateFromPattern(pattern: DiscoveredPattern) {
        val template = PatternDiscoveryService.toTemplate(pattern)
        _templatePrefill.value = TemplatePrefillDTO(
            name = template.name,
            categoryId = template.categoryId,
            amount = template.amount,
            transactionType = template.transactionType,
            accountId = template.accountId,
            note = template.note,
            payee = template.payee,
            location = template.location,
            paymentMethod = template.paymentMethod,
            currency = template.currency,
            tags = template.tags
        )
    }

    fun clearTemplatePrefill() {
        _templatePrefill.value = null
    }

    fun savePlannedPaymentFromPattern(pattern: DiscoveredPattern) {
        val payment = PatternDiscoveryService.toPlannedPayment(pattern)
        if (payment != null) {
            _plannedPaymentPrefill.value = PlannedPaymentPrefillDTO(
                title = payment.title,
                amount = payment.amount,
                currency = payment.currency,
                transactionType = payment.transactionType,
                accountId = payment.accountId,
                categoryId = payment.categoryId,
                paymentMethod = payment.paymentMethod,
                tags = payment.tags,
                note = payment.note,
                payee = payment.payee,
                recurrenceRule = payment.recurrenceRule,
                startDate = payment.startDate
            )
        }
    }

    fun clearPlannedPaymentPrefill() {
        _plannedPaymentPrefill.value = null
    }
}

class TransactionViewModelFactory(
    private val transactionUseCases: TransactionUseCases,
    private val settingsRepository: SettingsRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val filterRepository: FilterRepository,
    private val computationRepository: ComputationRepository,
    private val transactionManager: TransactionManager,
    private val plannedPaymentRepository: PlannedPaymentRepository,
    private val templateRepository: TemplateRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TransactionViewModel(
            transactionUseCases,
            settingsRepository,
            categoryRepository,
            accountRepository,
            filterRepository,
            computationRepository,
            transactionManager,
            plannedPaymentRepository,
            templateRepository
        ) as T
    }
}
