package com.osscabinet.ossWallet.ui.screens.transactions

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.FilterManager
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionListState
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.model.dto.PlannedPaymentPrefillDTO
import com.osscabinet.ossWallet.domain.model.dto.TemplatePrefillDTO
import com.osscabinet.ossWallet.domain.model.dto.TransactionPrefillDTO
import com.osscabinet.ossWallet.domain.model.forAccount
import com.osscabinet.ossWallet.domain.model.forCategory
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.ComputationRepository
import com.osscabinet.ossWallet.domain.repository.FilterRepository
import com.osscabinet.ossWallet.domain.repository.PlannedPaymentRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TagRepository
import com.osscabinet.ossWallet.domain.repository.TemplateRepository
import com.osscabinet.ossWallet.domain.service.discovery.DiscoveredPattern
import com.osscabinet.ossWallet.domain.service.discovery.PatternDiscoveryService
import com.osscabinet.ossWallet.domain.service.filter.TransactionFilterService
import com.osscabinet.ossWallet.domain.service.management.TransactionManager
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val transactionUseCases: TransactionUseCases,
    settingsRepository: SettingsRepository,
    categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val filterRepository: FilterRepository,
    private val computationRepository: ComputationRepository,
    private val transactionManager: TransactionManager,
    private val plannedPaymentRepository: PlannedPaymentRepository,
    private val templateRepository: TemplateRepository,
    private val tagRepository: TagRepository,
) : BaseViewModel(settingsRepository) {

    // Replace multiple filter states with single FilterManager
    private val recordsFilterManager = FilterManager()

    // Expose merged filter as StateFlow directly from FilterManager
    val recordsFilter: StateFlow<TransactionFilter> = recordsFilterManager.mergedFilter

    // Track the initial filter from navigation (for resetting)
    private val _initialFilter = MutableStateFlow(TransactionFilter())
    val initialFilter = _initialFilter.asStateFlow()

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

    val allTags = tagRepository.getAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories = categoryRepository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exchangeRates: StateFlow<Map<String, BigDecimal>> = computationRepository.exchangeRates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val recordsSettings: StateFlow<com.osscabinet.ossWallet.domain.model.settings.TransactionSectionSettings> =
        settingsRepository.recordsSettings
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                com.osscabinet.ossWallet.domain.model.settings.TransactionSectionSettings()
            )

    val accounts: StateFlow<List<com.osscabinet.ossWallet.domain.model.Account>> =
        settingsRepository.currentProfileId
            .flatMapLatest { accountRepository.getAllAccounts(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val baseCurrency: StateFlow<String> = computationRepository.selectedCurrency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "USD")

    // Expose unique payees and notes for suggestions
    val uniquePayees: StateFlow<List<String>> = settingsRepository.currentProfileId
        .map { profileId -> transactionUseCases.getUniquePayees(profileId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uniqueNotes: StateFlow<List<String>> = settingsRepository.currentProfileId
        .map { profileId -> transactionUseCases.getUniqueNotes(profileId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val absoluteTransactionsWithBalance: StateFlow<List<TransactionWithBalance>> =
        settingsRepository.currentProfileId
            .flatMapLatest { profileId ->
                combine(
                    transactionUseCases.getTransactions(profileId),
                    accounts,
                    exchangeRates,
                    computationRepository.hasLoadedExchangeRates,
                    recordsSettings.map { it.excludeFutureTransactions }.distinctUntilChanged()
                ) { allTransactions, accounts, rates, hasLoadedExchangeRates, excludeFuture ->
                    if (!hasLoadedExchangeRates) return@combine emptyList()

                    transactionUseCases.getRecentTransactions(
                        allTransactions = allTransactions,
                        accounts = accounts,
                        exchangeRates = rates,
                        limit = Int.MAX_VALUE,
                        excludeFuture = excludeFuture
                    )
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )

    val transactionsWithBalance: StateFlow<List<TransactionWithBalance>> =
        combine(
            absoluteTransactionsWithBalance,
            recordsFilter,
            recordsSettings.map { it.excludeFutureTransactions }.distinctUntilChanged()
        ) { absoluteTxs, currentFilter, globalExcludeFuture ->
            // Respect both current filter and global setting
            val effectiveFilter = currentFilter.copy(
                excludeFuture = currentFilter.excludeFuture && globalExcludeFuture
            )
            TransactionFilterService.filterTransactionsWithBalance(absoluteTxs, effectiveFilter)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val scopedHistoryWithBalance: StateFlow<List<TransactionWithBalance>> =
        combine(
            absoluteTransactionsWithBalance,
            recordsFilter.map { it.timelineRange }.distinctUntilChanged()
        ) { absoluteTxs, timelineRange ->
            val (_, endTime) = TimelineService.getDateRange(timelineRange)

            if (endTime == null) absoluteTxs
            else absoluteTxs.filter { it.transaction.timestamp <= endTime }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val listState: StateFlow<TransactionListState> =
        combine(
            transactionsWithBalance,
            scopedHistoryWithBalance,
            accounts,
            categories,
            allTags,
            baseCurrency,
            exchangeRates,
            recordsSettings
        ) { args: Array<*> ->
            @Suppress("UNCHECKED_CAST")
            val filtered = args[0] as List<TransactionWithBalance>

            @Suppress("UNCHECKED_CAST")
            val absolute = args[1] as List<TransactionWithBalance>

            @Suppress("UNCHECKED_CAST")
            val accounts = args[2] as List<com.osscabinet.ossWallet.domain.model.Account>

            @Suppress("UNCHECKED_CAST")
            val categories = args[3] as List<Category>

            @Suppress("UNCHECKED_CAST")
            val tags = args[4] as List<com.osscabinet.ossWallet.domain.model.Tag>
            val base = args[5] as String

            @Suppress("UNCHECKED_CAST")
            val rates = args[6] as Map<String, BigDecimal>
            val settings =
                args[7] as com.osscabinet.ossWallet.domain.model.settings.TransactionSectionSettings

            if (filtered.isEmpty()) return@combine TransactionListState()

            val groups = TimelineService.getGroupedData(
                transactions = filtered,
                absoluteHistory = absolute,
                groupingType = settings.groupBy,
                accounts = accounts,
                categories = categories,
                allTags = tags,
                baseCurrency = base,
                exchangeRates = rates
            )
            TransactionListState(groups = groups, isEmpty = false)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TransactionListState(),
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
                        (it.name.equals(pattern.name, ignoreCase = true) ||
                                it.payee?.equals(pattern.payee, ignoreCase = true) == true)
                    }
                    val existsAsPlannedPayment = plannedPayments.any {
                        it.name.equals(pattern.name, ignoreCase = true) ||
                                it.payee?.equals(pattern.payee, ignoreCase = true) == true
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
        recordsFilterManager.setFilter(
            "date",
            TransactionFilter(timelineRange = TimelineRange.CustomRange(start, end))
        )
    }

    fun updateRecordsTimelineRange(range: TimelineRange) {
        recordsFilterManager.setFilter("date", TransactionFilter(timelineRange = range))
    }

    fun setIncomeTypeFilter(type: com.osscabinet.ossWallet.domain.model.TransactionType?) {
        if (type != null) {
            recordsFilterManager.setFilter(
                "type",
                TransactionFilter(transactionTypes = setOf(type))
            )
        } else {
            recordsFilterManager.removeFilter("type")
        }
    }

    fun clearRecordsFilters() {
        recordsFilterManager.clear()
    }

    /**
     * Initialize the filter from a navigation context
     * This should be called when the screen opens with a filter parameter
     * Sets the initial filter in FilterManager
     */
    fun initializeRecordsFilter(filter: TransactionFilter) {
        _initialFilter.value = filter
        recordsFilterManager.clear()
        recordsFilterManager.setFilter("initial", filter)
    }

    /**
     * Reset the filter to the initial state
     */
    fun resetToInitialFilter() {
        initializeRecordsFilter(_initialFilter.value)
    }

    fun addTag(name: String, color: Int) {
        viewModelScope.launch {
            tagRepository.insertTag(
                com.osscabinet.ossWallet.domain.model.Tag(
                    name = name,
                    colorHex = color
                )
            )
        }
    }


    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            transactionUseCases.deleteTransaction(id)
        }
    }

    fun updateRecordsSettings(update: (com.osscabinet.ossWallet.domain.model.settings.TransactionSectionSettings) -> com.osscabinet.ossWallet.domain.model.settings.TransactionSectionSettings) {
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
                if (Category.isTransfer(transaction.categoryId)) {
                    val linkedTxs = transactionUseCases.getLinkedTransactions(transactionId).first()
                    val targetTx = linkedTxs.find {
                        Category.isTransfer(it.categoryId) &&
                                it.accountId != transaction.accountId &&
                                it.transactionType == com.osscabinet.ossWallet.domain.model.TransactionType.INCOME
                    }
                    if (targetTx != null) {
                        val rate = if (transaction.amount.signum() != 0) {
                            targetTx.amount.abs()
                                .divide(transaction.amount.abs(), 6, RoundingMode.HALF_UP)
                        } else BigDecimal.ONE

                        prefill = prefill.copy(
                            accountId = transaction.accountId,
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

    /**
     * Set the split transaction ID and fetch the prefill DTO.
     * This allows the UI to navigate to the entry screen with split mode prefilled.
     */
    fun setSplitTransactionId(transactionId: Long) {
        viewModelScope.launch {
            val transaction = transactionUseCases.getTransactionById(transactionId)
            if (transaction != null) {
                _prefillDto.value = TransactionPrefillDTO.fromTransaction(
                    transaction,
                    clonedFromId = transactionId,
                    isSplitMode = true
                )
            } else {
                _prefillDto.value = null
            }
        }
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

    /**
     * Search transactions for linking.
     * Uses FTS-backed repository search.
     */
    fun searchTransactions(query: String): Flow<List<Transaction>> = flow {
        withProfile { profileId ->
            emitAll(transactionUseCases.searchTransactions(profileId, query))
        }
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
                template = PatternDiscoveryService.toTemplate(pattern),
                schedule = payment.schedule
            )
        }
    }

    fun setPlannedPaymentPrefillFromTemplate(template: com.osscabinet.ossWallet.domain.model.Template) {
        val now = java.time.LocalDate.now(java.time.ZoneOffset.UTC)
        val startEpoch = now.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()

        _plannedPaymentPrefill.value = PlannedPaymentPrefillDTO(
            template = template,
            schedule = com.osscabinet.ossWallet.domain.model.PaymentSchedule(
                recurrenceRule = com.osscabinet.ossWallet.domain.model.RecurrenceRule(type = com.osscabinet.ossWallet.domain.model.RecurrenceType.MONTHLY),
                startDate = startEpoch,
                nextDueDate = startEpoch
            )
        )
    }

    fun clearPlannedPaymentPrefill() {
        _plannedPaymentPrefill.value = null
    }
}
