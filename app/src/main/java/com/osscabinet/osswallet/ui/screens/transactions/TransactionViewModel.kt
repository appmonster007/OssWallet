package com.osscabinet.ossWallet.ui.screens.transactions

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.FilterManager
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionListState
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.model.OccurrenceStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.RecordUiModel
import com.osscabinet.ossWallet.domain.model.UpcomingPaymentItem
import com.osscabinet.ossWallet.domain.model.dto.PlannedPaymentPrefillDTO
import com.osscabinet.ossWallet.domain.model.dto.TemplatePrefillDTO
import com.osscabinet.ossWallet.domain.model.dto.TransactionPrefillDTO
import com.osscabinet.ossWallet.domain.model.dto.TransactionRequest
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
import com.osscabinet.ossWallet.domain.service.management.PlannedPaymentService
import com.osscabinet.ossWallet.domain.service.management.TransactionManager
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
import com.osscabinet.ossWallet.ui.components.transactions.TransactionItemEvent
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

    enum class RecordNavigationMode {
        PERSISTENT, // "Home Base" - remembers everything
        TRANSIENT   // "Link View" - isolated, one-off filter
    }

    // Two independent filter managers for dual-mode persistence
    private val persistentFilterManager = FilterManager()
    private val transientFilterManager = FilterManager()
    
    private val _navigationMode = MutableStateFlow(RecordNavigationMode.PERSISTENT)
    val navigationMode = _navigationMode.asStateFlow()

    // Expose merged filter based on active mode
    val recordsFilter: StateFlow<TransactionFilter> = _navigationMode
        .flatMapLatest { mode ->
            if (mode == RecordNavigationMode.TRANSIENT) transientFilterManager.mergedFilter
            else persistentFilterManager.mergedFilter
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TransactionFilter())

    // Track the initial filter from navigation (for resetting)
    private val _initialFilter = MutableStateFlow(TransactionFilter())
    val initialFilter = _initialFilter.asStateFlow()

    // Track revealed card IDs for swipe actions
    private val _revealedCardIdsList = MutableStateFlow<List<String>>(emptyList())
    val revealedCardIdsList: StateFlow<List<String>> = _revealedCardIdsList.asStateFlow()

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

    init {
        // Sync initial state to the persistent records filter
        viewModelScope.launch {
            val baseFilter = TransactionFilter(timelineRange = TimelineRange.AllTime(excludeFuture = true))
            _initialFilter.value = baseFilter
            persistentFilterManager.setFilter("initial", baseFilter)
        }
    }

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
                    computationRepository.hasLoadedExchangeRates
                ) { allTransactions, accounts, rates, hasLoadedExchangeRates ->
                    if (!hasLoadedExchangeRates) return@combine emptyList()

                    // includeArchived = true so the base list covers all accounts;
                    // filtering to active-only happens downstream in transactionsWithBalance.
                    transactionUseCases.getRecentTransactions(
                        allTransactions = allTransactions,
                        accounts = accounts,
                        exchangeRates = rates,
                        limit = Int.MAX_VALUE,
                        excludeFuture = false,
                        includeArchived = true
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
            accounts
        ) { absoluteTxs, currentFilter, allAccounts ->
            val activeIds = allAccounts.filter { !it.isArchived }.map { it.id }.toSet()
            val effectiveFilter = currentFilter.copy(
                accountIds = currentFilter.accountIds.ifEmpty { activeIds }
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
            // Only recompute when timeline or account scope changes, not on text query/amount changes.
            recordsFilter.map { it.timelineRange to it.accountIds }.distinctUntilChanged(),
            accounts
        ) { absoluteTxs, filterKey, allAccounts ->
            val (timelineRange, accountIds) = filterKey
            val activeIds = allAccounts.filter { !it.isArchived }.map { it.id }.toSet()
            val effectiveAccountIds = accountIds.ifEmpty { activeIds }
            val (_, endTime) = TimelineService.getDateRange(timelineRange)

            absoluteTxs
                .filter { it.transaction.accountId in effectiveAccountIds }
                .let { if (endTime == null) it else it.filter { tx -> tx.transaction.timestamp <= endTime } }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val upcomingPayments: StateFlow<List<UpcomingPaymentItem>> =
        combine(
            settingsRepository.currentProfileId.flatMapLatest { plannedPaymentRepository.getActivePlannedPayments(it) },
            recordsFilter,
            recordsSettings
        ) { payments, filter, settings ->
            if (!settings.showPlannedPayments) return@combine emptyList()

            val now = System.currentTimeMillis()
            val (startTime, endTime) = TimelineService.getDateRange(filter.timelineRange)
            
            val limitDate = now + 365L * 24 * 60 * 60 * 1000
            val effectiveEndTime = when {
                endTime != null -> minOf(endTime, limitDate)
                else -> limitDate
            }

            val occurrences = PlannedPaymentService.generateUpcoming(
                payments = payments,
                fromDate = startTime ?: minOf(now, payments.minOfOrNull { it.schedule.nextDueDate } ?: now),
                daysAhead = ((effectiveEndTime - (startTime ?: now)) / (24 * 60 * 60 * 1000)).toInt().coerceAtLeast(0)
            )

            if (filter.timelineRange.excludeFuture) {
                occurrences.filter { it.scheduledDate <= now }
            } else {
                occurrences
            }
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
            recordsSettings,
            upcomingPayments
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

            @Suppress("UNCHECKED_CAST")
            val planned = args[8] as List<UpcomingPaymentItem>

            if (filtered.isEmpty() && planned.isEmpty()) return@combine TransactionListState()

            val groups = TimelineService.getGroupedData(
                transactions = filtered,
                absoluteHistory = absolute,
                groupingType = settings.groupBy,
                accounts = accounts,
                categories = categories,
                allTags = tags,
                baseCurrency = base,
                exchangeRates = rates,
                plannedPayments = planned
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
                    TransactionFilter(timelineRange = TimelineRange.AllTime())
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

    private fun getActiveManager() = if (_navigationMode.value == RecordNavigationMode.TRANSIENT) {
        transientFilterManager
    } else {
        persistentFilterManager
    }

    fun updateRecordsFilter(newFilter: TransactionFilter) {
        getActiveManager().setFilter("manual", newFilter)
    }

    fun setRecordsAccountFilter(accountId: String?) {
        val manager = getActiveManager()
        if (accountId != null) {
            manager.setFilter("account", TransactionFilter().forAccount(accountId))
        } else {
            manager.removeFilter("account")
        }
    }

    fun setRecordsCategoryFilter(categoryId: Long?) {
        val manager = getActiveManager()
        if (categoryId != null) {
            manager.setFilter("category", TransactionFilter().forCategory(categoryId))
        } else {
            manager.removeFilter("category")
        }
    }

    fun setRecordsDateRangeFilter(start: Long?, end: Long?) {
        getActiveManager().setFilter(
            "date",
            TransactionFilter(timelineRange = TimelineRange.CustomRange(start, end))
        )
    }

    fun updateRecordsTimelineRange(range: TimelineRange) {
        getActiveManager().setFilter("date", TransactionFilter(timelineRange = range))
    }

    fun setIncomeTypeFilter(type: TransactionType?) {
        val manager = getActiveManager()
        if (type != null) {
            manager.setFilter(
                "type",
                TransactionFilter(transactionTypes = setOf(type))
            )
        } else {
            manager.removeFilter("type")
        }
    }

    fun clearRecordsFilters() {
        getActiveManager().clear()
    }

    /**
     * Set the navigation mode and initialize filters accordingly.
     * Requirement: Persistent mode remembers state, Transient mode is isolated.
     */
    fun setRecordsMode(topLevel: Boolean, filter: TransactionFilter? = null) {
        if (topLevel) {
            _navigationMode.value = RecordNavigationMode.PERSISTENT
            // persistentFilterManager already has its state preserved
        } else {
            _navigationMode.value = RecordNavigationMode.TRANSIENT
            transientFilterManager.clear()
            if (filter != null) {
                transientFilterManager.setFilter("transient_base", filter)
            }
        }
    }

    /**
     * Reset the filter to the initial state for the CURRENT mode
     */
    fun resetToInitialFilter() {
        if (_navigationMode.value == RecordNavigationMode.TRANSIENT) {
            // Re-apply the transient base if it exists
            val base = transientFilterManager.getFilter("transient_base")
            transientFilterManager.clear()
            if (base != null) transientFilterManager.setFilter("transient_base", base)
        } else {
            persistentFilterManager.clear()
            persistentFilterManager.setFilter("initial", _initialFilter.value)
        }
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
    fun onItemExpanded(cardId: String) {
        if (_revealedCardIdsList.value.contains(cardId)) return
        _revealedCardIdsList.value = _revealedCardIdsList.value.toMutableList().also { list ->
            list.add(cardId)
        }
    }

    fun onItemCollapsed(cardId: String) {
        if (!_revealedCardIdsList.value.contains(cardId)) return
        _revealedCardIdsList.value = _revealedCardIdsList.value.toMutableList().also { list ->
            list.remove(cardId)
        }
    }

    fun confirmPlannedPayment(paymentId: Long, scheduledDate: Long) {
        viewModelScope.launch {
            val payment = plannedPaymentRepository.getById(paymentId) ?: return@launch
            val profileId = settingsRepository.currentProfileId.first()
            val now = System.currentTimeMillis();
            val request = TransactionRequest(
                name = payment.name,
                amount = payment.amount,
                transactionType = payment.transactionType,
                accountId = payment.accountId,
                categoryId = payment.categoryId,
                currency = payment.currency,
                timestamp = now,
                tags = payment.tags,
                payee = payment.payee ?: "",
                note = payment.note ?: "",
                status = payment.transactionStatus,
                sourcePlannedPaymentId = paymentId,
                isTransfer = payment.transactionType == TransactionType.TRANSFER,
                targetAccountId = payment.targetAccountId,
                targetCurrency = payment.targetCurrency,
                transferRate = payment.transferRate
            )

            transactionManager.handleSaveTransaction(
                existingTransaction = null,
                request = request,
                profileId = profileId,
                sourcePlannedPaymentId = paymentId,
                scheduledDate = scheduledDate
            )
            onItemCollapsed("planned_${paymentId}_$scheduledDate")
        }
    }

    fun cancelPlannedPayment(paymentId: Long, scheduledDate: Long) {
        viewModelScope.launch {
            transactionManager.skipPlannedPayment(paymentId, scheduledDate)
            onItemCollapsed("planned_${paymentId}_$scheduledDate")
        }
    }

    fun quickUpdateStatus(
        transactionId: Long,
        newStatus: com.osscabinet.ossWallet.domain.model.TransactionStatus
    ) {
        viewModelScope.launch {
            withProfile { profileId ->
                if (transactionManager.updateStatus(transactionId, newStatus, profileId)) {
                    onItemCollapsed("tx_$transactionId")
                }
            }
        }
    }

    fun quickCloneTransaction(transactionId: Long) {
        viewModelScope.launch {
            withProfile { profileId ->
                if (transactionManager.cloneTransaction(transactionId, profileId) != null) {
                    onItemCollapsed("tx_$transactionId")
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
                                it.transactionType == TransactionType.INCOME
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
            onItemCollapsed("tx_$transactionId")
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
                    onItemCollapsed("tx_$transactionId")
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
            onItemCollapsed("tx_${pattern.representativeTransactionId}")
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
