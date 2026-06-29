package com.osscabinet.ossWallet.ui.screens.transactions

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.FilterManager
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.PaymentSchedule
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.RecurrenceRule
import com.osscabinet.ossWallet.domain.model.RecurrenceType
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionListState
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.model.UpcomingPaymentItem
import com.osscabinet.ossWallet.domain.model.dto.PlannedPaymentPrefillDTO
import com.osscabinet.ossWallet.domain.model.dto.TemplatePrefillDTO
import com.osscabinet.ossWallet.domain.model.dto.TransactionPrefillDTO
import com.osscabinet.ossWallet.domain.model.dto.TransactionRequest
import com.osscabinet.ossWallet.domain.model.isTransfer
import com.osscabinet.ossWallet.domain.model.settings.TransactionSectionSettings
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.model.toEntryMode
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.ComputationRepository
import com.osscabinet.ossWallet.domain.repository.FilterRepository
import com.osscabinet.ossWallet.domain.repository.PlannedPaymentRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TagRepository
import com.osscabinet.ossWallet.domain.repository.TemplateRepository
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.domain.service.discovery.DiscoveredPattern
import com.osscabinet.ossWallet.domain.service.discovery.PatternDiscoveryService
import com.osscabinet.ossWallet.domain.service.filter.TransactionFilterService
import com.osscabinet.ossWallet.domain.service.management.PlannedPaymentService
import com.osscabinet.ossWallet.domain.service.management.TransactionManager
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
import com.osscabinet.ossWallet.ui.mapper.TransactionUiMapper
import com.osscabinet.ossWallet.ui.screens.transactions.model.TransactionListItem
import com.osscabinet.ossWallet.ui.screens.transactions.model.TransactionListUiState
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val transactionUseCases: TransactionUseCases,
    settingsRepository: SettingsRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val filterRepository: FilterRepository,
    private val computationRepository: ComputationRepository,
    private val transactionManager: TransactionManager,
    private val plannedPaymentRepository: PlannedPaymentRepository,
    private val templateRepository: TemplateRepository,
    private val tagRepository: TagRepository
) : BaseViewModel(settingsRepository) {

    enum class RecordNavigationMode {
        PERSISTENT,
        TRANSIENT
    }

    private val persistentFilterManager = FilterManager()
    private val transientFilterManager = FilterManager()

    private val _navigationMode =
        MutableStateFlow<RecordNavigationMode>(RecordNavigationMode.PERSISTENT)
    val navigationMode: StateFlow<RecordNavigationMode> = _navigationMode.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val recordsFilter: StateFlow<TransactionFilter> = _navigationMode
        .flatMapLatest { mode ->
            if (mode == RecordNavigationMode.PERSISTENT) persistentFilterManager.mergedFilter
            else transientFilterManager.mergedFilter
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TransactionFilter())

    private val _initialFilter = MutableStateFlow<TransactionFilter>(TransactionFilter())
    val initialFilter: StateFlow<TransactionFilter> = _initialFilter.asStateFlow()

    private val _revealedCardIdsList = MutableStateFlow<List<String>>(emptyList())
    val revealedCardIdsList: StateFlow<List<String>> = _revealedCardIdsList.asStateFlow()

    private val _highlightedId = MutableStateFlow<String?>(null)
    val highlightedId: StateFlow<String?> = _highlightedId.asStateFlow()

    private val _scrollToId = MutableStateFlow<String?>(null)
    val scrollToId: StateFlow<String?> = _scrollToId.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _linkedTransactionsPopup = MutableStateFlow<List<TransactionListItem>?>(null)
    val linkedTransactionsPopup: StateFlow<List<TransactionListItem>?> =
        _linkedTransactionsPopup.asStateFlow()

    private val _prefillDto = MutableStateFlow<TransactionPrefillDTO?>(null)
    val prefillDto: StateFlow<TransactionPrefillDTO?> = _prefillDto.asStateFlow()

    private val _templatePrefill = MutableStateFlow<TemplatePrefillDTO?>(null)
    val templatePrefill: StateFlow<TemplatePrefillDTO?> = _templatePrefill.asStateFlow()

    private val _plannedPaymentPrefill = MutableStateFlow<PlannedPaymentPrefillDTO?>(null)
    val plannedPaymentPrefill: StateFlow<PlannedPaymentPrefillDTO?> =
        _plannedPaymentPrefill.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val savedFilters: StateFlow<List<com.osscabinet.ossWallet.domain.model.Filter>> =
        settingsRepository.currentProfileId
            .flatMapLatest { filterRepository.getAllFilters() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTags: StateFlow<List<Tag>> = tagRepository.getAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = categoryRepository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exchangeRates: StateFlow<ExchangeRateSnapshot> = computationRepository.exchangeRatesSnapshot
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ExchangeRateSnapshot.EMPTY)

    val recordsSettings: StateFlow<TransactionSectionSettings> = settingsRepository.recordsSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TransactionSectionSettings())

    init {
        val baseFilter =
            TransactionFilter(timelineRange = TimelineRange.AllTime(excludeFuture = true))
        persistentFilterManager.setFilter("base", baseFilter)
        _initialFilter.value = baseFilter
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val accounts: StateFlow<List<Account>> = settingsRepository.currentProfileId
        .flatMapLatest { accountRepository.getAllAccounts(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val baseCurrency: StateFlow<String> = computationRepository.selectedCurrency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "USD")

    val allTransactionLinks: StateFlow<Map<Long, List<Long>>> =
        transactionManager.getAllTransactionLinks()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uniquePayees: StateFlow<List<String>> = settingsRepository.currentProfileId
        .flatMapLatest { profileId -> flow { emit(transactionUseCases.getUniquePayees(profileId)) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uniqueNotes: StateFlow<List<String>> = settingsRepository.currentProfileId
        .flatMapLatest { profileId -> flow { emit(transactionUseCases.getUniqueNotes(profileId)) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val absoluteTransactionsWithBalance: StateFlow<List<TransactionWithBalance>> =
        settingsRepository.currentProfileId
            .flatMapLatest { profileId: Int ->
                combine(
                    baseCurrency,
                    exchangeRates,
                    computationRepository.hasLoadedExchangeRates
                ) { base, rates, loaded -> Triple(base, rates, loaded) }
                    .distinctUntilChanged()
                    .flatMapLatest { (base: String, rates: ExchangeRateSnapshot, loaded: Boolean) ->
                        if (!loaded) {
                            kotlinx.coroutines.flow.flowOf(emptyList<TransactionWithBalance>())
                        } else {
                            transactionUseCases.observeHistoryWithBalances(
                                profileId = profileId,
                                filter = TransactionFilter(
                                    timelineRange = TimelineRange.AllTime(
                                        excludeFuture = false
                                    )
                                ),
                                baseCurrency = base,
                                exchangeRates = rates
                            ).map { result: Result<List<TransactionWithBalance>> ->
                                result.getOrNull() ?: emptyList<TransactionWithBalance>()
                            }
                        }
                    }
                    .flowOn(kotlinx.coroutines.Dispatchers.Default)
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

    @OptIn(ExperimentalCoroutinesApi::class)
    val upcomingPayments: StateFlow<List<UpcomingPaymentItem>> =
        combine(
            settingsRepository.currentProfileId.flatMapLatest {
                plannedPaymentRepository.getActivePlannedPayments(
                    it
                )
            },
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
                fromDate = startTime ?: minOf(
                    now,
                    payments.minOfOrNull { it.schedule.nextDueDate } ?: now),
                daysAhead = ((effectiveEndTime - (startTime
                    ?: now)) / (24 * 60 * 60 * 1000)).toInt().coerceAtLeast(0)
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

    private data class StructuralSnapshot(
        val filteredTransactions: List<TransactionWithBalance>,
        val absoluteHistory: List<TransactionWithBalance>,
        val accounts: List<Account>,
        val categories: List<Category>,
        val links: Map<Long, List<Long>>
    )

    private data class MetadataSnapshot(
        val tags: List<Tag>,
        val upcomingPayments: List<UpcomingPaymentItem>
    )

    private data class ConfigSnapshot(
        val baseCurrency: String,
        val exchangeRates: ExchangeRateSnapshot,
        val settings: TransactionSectionSettings
    )

    val listState: StateFlow<TransactionListUiState> =
        combine(
            combine(
                transactionsWithBalance,
                scopedHistoryWithBalance,
                accounts,
                categories,
                allTransactionLinks
            ) { filtered, absolute, accounts, categories, links ->
                StructuralSnapshot(filtered, absolute, accounts, categories, links)
            },
            combine(allTags, upcomingPayments) { tags, upcoming ->
                MetadataSnapshot(tags, upcoming)
            },
            combine(baseCurrency, exchangeRates, recordsSettings) { base, rates, settings ->
                ConfigSnapshot(base, rates, settings)
            }
        ) { structural, metadata, config ->
            if (structural.filteredTransactions.isEmpty() && metadata.upcomingPayments.isEmpty()) {
                return@combine TransactionListUiState()
            }

            val initialBalances = structural.accounts.associate { acc ->
                val earliestTx =
                    structural.absoluteHistory.findLast { it.transaction.accountId == acc.id }
                acc.id to (earliestTx?.balanceBefore ?: BigDecimal.ZERO)
            }

            val domainGroups = TimelineService.getGroupedData(
                transactions = structural.filteredTransactions,
                absoluteHistory = structural.absoluteHistory,
                groupingType = config.settings.groupBy,
                accounts = structural.accounts,
                categories = structural.categories,
                allTags = metadata.tags,
                baseCurrency = config.baseCurrency,
                exchangeRates = config.exchangeRates,
                plannedPayments = metadata.upcomingPayments,
                linkedTransactionIds = structural.links,
                initialBalances = initialBalances,
                allTxnBreakdown = config.settings.allTxnBreakdown,
            )

            TransactionUiMapper.mapToUiState(
                domainState = TransactionListState(groups = domainGroups, isEmpty = false),
                baseCurrency = config.baseCurrency
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TransactionListUiState(),
        )

    val transactions: StateFlow<List<Transaction>> = absoluteTransactionsWithBalance
        .map { list -> list.map { tx -> tx.transaction } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val discoveredPatterns: StateFlow<List<DiscoveredPattern>> = settingsRepository.currentProfileId
        .flatMapLatest { profileId ->
            combine(
                transactionUseCases.getTransactions(profileId),
                plannedPaymentRepository.getActivePlannedPayments(profileId),
                templateRepository.getAllTemplates(profileId)
            ) { txs, _, _ ->
                PatternDiscoveryService.discoverPatterns(txs)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun getActiveManager(): FilterManager {
        return if (_navigationMode.value == RecordNavigationMode.PERSISTENT) persistentFilterManager
        else transientFilterManager
    }

    fun updateRecordsFilter(filter: TransactionFilter) {
        getActiveManager().setFilter("manual", filter)
    }

    fun setRecordsAccountFilter(accountId: String?) {
        val current = getActiveManager().mergedFilter.value
        val newIds = if (accountId == null) emptySet() else setOf(accountId)
        if (current.accountIds == newIds) return
        updateRecordsFilter(current.copy(accountIds = newIds))
    }

    fun setRecordsCategoryFilter(categoryId: Long?) {
        val current = getActiveManager().mergedFilter.value
        val newIds = if (categoryId == null) emptySet() else setOf(categoryId)
        if (current.categoryIds == newIds) return
        updateRecordsFilter(current.copy(categoryIds = newIds))
    }

    fun setRecordsDateRangeFilter(start: Long?, end: Long?) {
        val current = getActiveManager().mergedFilter.value
        updateRecordsFilter(
            current.copy(
                timelineRange = TimelineRange.CustomRange(start ?: 0L, end ?: Long.MAX_VALUE)
            )
        )
    }

    fun updateRecordsTimelineRange(range: TimelineRange) {
        updateRecordsFilter(getActiveManager().mergedFilter.value.copy(timelineRange = range))
    }

    fun setIncomeTypeFilter(type: TransactionType?) {
        val current = getActiveManager().mergedFilter.value
        val newTypes = when (type) {
            TransactionType.INCOME -> setOf(TransactionType.INCOME)
            TransactionType.EXPENSE -> setOf(TransactionType.EXPENSE)
            TransactionType.TRANSFER_INFLOW, TransactionType.TRANSFER_OUTFLOW -> TransactionType.TRANSFER_TYPES
            else -> emptySet()
        }
        updateRecordsFilter(current.copy(transactionTypes = newTypes))
    }

    fun clearRecordsFilters() {
        getActiveManager().clear()
        updateRecordsFilter(initialFilter.value)
    }

    fun setRecordsMode(transient: Boolean, filter: TransactionFilter? = null) {
        if (transient) {
            _navigationMode.value = RecordNavigationMode.TRANSIENT
            if (filter != null) {
                transientFilterManager.setFilter("transient", filter)
            }
        } else {
            _navigationMode.value = RecordNavigationMode.PERSISTENT
        }
    }

    fun resetToInitialFilter() {
        _navigationMode.value = RecordNavigationMode.PERSISTENT
        updateRecordsFilter(_initialFilter.value)

        // Also clear transient if it exists
        transientFilterManager.clear()
    }

    fun addTag(name: String, color: Int) {
        viewModelScope.launch {
            tagRepository.insertTag(Tag(name = name, colorHex = color))
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            withProfile { profileId ->
                transactionManager.deleteTransaction(id, profileId)
            }
        }
    }

    fun updateRecordsSettings(transform: (TransactionSectionSettings) -> TransactionSectionSettings) {
        viewModelScope.launch {
            settingsRepository.updateRecordsSettings(transform)
        }
    }

    fun onItemExpanded(id: String) {
        _revealedCardIdsList.update { current ->
            if (id in current) current else current + id
        }
    }

    fun onItemCollapsed(id: String) {
        _revealedCardIdsList.update { current ->
            current.filterNot { it == id }
        }
    }

    fun confirmPlannedPayment(paymentId: Long, scheduledDate: Long) {
        viewModelScope.launch {
            val profileId = settingsRepository.currentProfileId.first()
            val payment = plannedPaymentRepository.getById(paymentId) ?: return@launch

            val request = TransactionRequest(
                name = payment.name,
                amount = payment.amount,
                entryMode = payment.entryMode,
                accountId = payment.accountId ?: "",
                currency = payment.currency,
                categoryId = payment.categoryId,
                tags = payment.tags,
                paymentMethod = payment.paymentMethod ?: PaymentMethod.CASH,
                timestamp = System.currentTimeMillis(),
                payee = payment.payee ?: "",
                note = payment.note ?: "",
                targetAccountId = payment.targetAccountId,
                targetCurrency = payment.targetCurrency,
                transferRate = payment.transferRate,
                sourcePlannedPaymentId = payment.id,
                warrantyExpiration = payment.warrantyExpiration,
                gpsLocation = payment.gpsLocation
            )

            transactionManager.handleSaveTransaction(
                existingTransaction = null,
                request = request,
                profileId = profileId,
                sourcePlannedPaymentId = payment.id,
                scheduledDate = scheduledDate
            )
            _snackbarMessage.value = "Payment confirmed"
        }
    }

    fun cancelPlannedPayment(paymentId: Long, scheduledDate: Long) {
        viewModelScope.launch {
            transactionManager.skipPlannedPayment(paymentId, scheduledDate)
            _snackbarMessage.value = "Payment skipped"
        }
    }

    fun quickUpdateStatus(id: Long, status: TransactionStatus) {
        viewModelScope.launch {
            withProfile { profileId ->
                transactionManager.updateStatus(id, status, profileId)
            }
        }
    }

    fun quickCloneTransaction(id: Long) {
        viewModelScope.launch {
            withProfile { profileId ->
                transactionManager.cloneTransaction(id, profileId)
                _snackbarMessage.value = "Transaction cloned"
            }
        }
    }

    fun setCloneTransactionId(id: Long) {
        viewModelScope.launch {
            withProfile { profileId ->
                val tx = transactionUseCases.getTransactionById(id, profileId) ?: return@withProfile
                val dto = TransactionPrefillDTO(
                    name = tx.name,
                    amount = tx.amount.abs().toPlainString(),
                    categoryId = tx.categoryId,
                    entryMode = tx.transactionType.toEntryMode(),
                    accountId = tx.accountId ?: "",
                    currency = tx.currency,
                    tags = tx.tags,
                    paymentMethod = tx.paymentMethod,
                    payee = tx.payee,
                    note = tx.note,
                    transactionType = tx.transactionType,
                    gpsLocation = tx.gpsLocation,
                    warrantyExpiration = tx.warrantyExpiration
                )

                // Handle transfer specifics
                if (tx.transactionType.isTransfer) {
                    val links = transactionUseCases.getLinkedTransactions(tx.id).first()
                    val other = links.find { it.id != tx.id }
                    if (other != null) {
                        _prefillDto.value = dto.copy(
                            targetAccountId = other.accountId,
                            targetCurrency = other.currency,
                            exchangeRate = tx.transferRate.toPlainString()
                        )
                        return@withProfile
                    }
                }

                _prefillDto.value = dto
            }
        }
    }

    fun setPlannedPaymentPrefill(id: Long, scheduledDate: Long?) {
        viewModelScope.launch {
            val pp = plannedPaymentRepository.getById(id) ?: return@launch
            _prefillDto.value = TransactionPrefillDTO.fromPlannedPayment(pp, scheduledDate)
        }
    }

    fun setTemplatePrefill(id: Long) {
        viewModelScope.launch {
            val template = templateRepository.getTemplateById(id) ?: return@launch
            _templatePrefill.value = TemplatePrefillDTO(
                name = template.name,
                categoryId = template.categoryId,
                amount = template.amount,
                entryMode = template.entryMode,
                accountId = template.accountId,
                note = template.note,
                payee = template.payee,
                location = template.location,
                gpsLocation = template.gpsLocation,
                warrantyExpiration = template.warrantyExpiration,
                currency = template.currency,
                tags = template.tags
            )
        }
    }

    fun clearPrefillDto() {
        _prefillDto.value = null
        _templatePrefill.value = null
        _plannedPaymentPrefill.value = null
    }

    fun quickDeleteTransaction(id: Long) {
        viewModelScope.launch {
            deleteTransaction(id)
            _snackbarMessage.value = "Transaction deleted"
        }
    }

    fun setSplitTransactionId(id: Long) {
        viewModelScope.launch {
            withProfile { profileId ->
                val tx = transactionUseCases.getTransactionById(id, profileId) ?: return@withProfile
                _prefillDto.value = TransactionPrefillDTO(
                    name = tx.name,
                    amount = tx.amount.abs().toPlainString(),
                    categoryId = tx.categoryId,
                    entryMode = tx.transactionType.toEntryMode(),
                    accountId = tx.accountId ?: "",
                    currency = tx.currency,
                    tags = tx.tags,
                    paymentMethod = tx.paymentMethod,
                    payee = tx.payee,
                    note = tx.note,
                    transactionType = tx.transactionType,
                    clonedFromTransactionId = tx.id,
                    isSplitMode = true
                )
            }
        }
    }

    fun toggleStatusClearedUncleared(id: Long) {
        viewModelScope.launch {
            withProfile { profileId ->
                val tx = transactionUseCases.getTransactionById(id, profileId) ?: return@withProfile
                val newStatus =
                    if (tx.status == TransactionStatus.CLEARED) TransactionStatus.UNCLEARED else TransactionStatus.CLEARED
                quickUpdateStatus(id, newStatus)
            }
        }
    }

    fun handleJumpRequest(id: Long, availableIds: Set<Long>? = null) {
        viewModelScope.launch {
            withProfile { profileId ->
                val links = transactionUseCases.getLinkedTransactionIds(id).first()
                if (links.isEmpty()) {
                    _snackbarMessage.value = "No linked transactions found"
                    return@withProfile
                }

                if (links.size == 1) {
                    performJump(links[0], availableIds)
                } else {
                    // Show selection sheet
                    val enriched = transactionUseCases.getLinkedTransactions(id).first()
                        .let {
                            TimelineService.enrichTransactions(
                                it,
                                accounts.value,
                                categories.value,
                                allTags.value,
                                baseCurrency.value,
                                exchangeRates.value
                            )
                        }

                    _linkedTransactionsPopup.value = enriched.map {
                        TransactionUiMapper.mapRecord(it, baseCurrency.value)
                    }
                }
            }
        }
    }

    fun performJump(id: Long, availableIds: Set<Long>? = null) {
        viewModelScope.launch {
            // Check if the transaction exists in the current list
            val itemExists = if (availableIds != null) {
                id in availableIds
            } else {
                transactionsWithBalance.value.any { it.transaction.id == id }
            }

            if (!itemExists) {
                _snackbarMessage.value = "Linked transaction not in the current list"
                _linkedTransactionsPopup.value = null
                return@launch
            }

            // Trigger scroll and highlight
            _scrollToId.value = "tx_$id"
            _highlightedId.value = "tx_$id"

            // Clear popup if open
            _linkedTransactionsPopup.value = null

            // Clear highlight after 1 second (quick blink)
            delay(1000)
            if (_highlightedId.value == "tx_$id") {
                _highlightedId.value = null
            }
        }
    }

    fun clearScrollTo() {
        _scrollToId.value = null
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun closeLinkedPopup() {
        _linkedTransactionsPopup.value = null
    }

    fun addTransactionLink(id1: Long, id2: Long) {
        viewModelScope.launch {
            transactionUseCases.addTransactionLink(id1, id2)
        }
    }

    fun removeTransactionLink(id1: Long, id2: Long) {
        viewModelScope.launch {
            transactionUseCases.removeTransactionLink(id1, id2)
        }
    }

    fun clearTransactionLinks(id: Long) {
        viewModelScope.launch {
            transactionUseCases.clearTransactionLinks(id)
        }
    }

    fun getLinkedTransactionIds(id: Long): Flow<List<Long>> =
        transactionUseCases.getLinkedTransactionIds(id)

    fun getLinkedTransactions(id: Long): Flow<List<Transaction>> =
        transactionUseCases.getLinkedTransactions(id)

    /**
     * Search transactions for linking.
     * Uses FTS-backed repository search.
     */
    fun searchTransactions(query: String): Flow<List<Transaction>> = flow {
        if (query.trim().length < 2) {
            emit(emptyList())
            return@flow
        }
        withProfile { profileId ->
            emitAll(transactionUseCases.searchTransactions(profileId, query))
        }
    }

    fun saveTemplateFromPattern(pattern: DiscoveredPattern) {
        setTemplatePrefillFromPattern(pattern)
    }

    fun setTemplatePrefillFromPattern(pattern: DiscoveredPattern) {
        _templatePrefill.value = TemplatePrefillDTO(
            name = pattern.name.ifBlank { pattern.payee.ifBlank { "Template" } },
            categoryId = pattern.categoryId.takeIf { it != 0L } ?: Category.SYSTEM_OTHERS,
            amount = pattern.averageAmount,
            entryMode = pattern.entryMode,
            accountId = pattern.accountId,
            note = pattern.note.ifBlank { null },
            payee = pattern.payee.ifBlank { null },
            location = null,
            gpsLocation = pattern.gpsLocation,
            warrantyExpiration = pattern.warrantyExpiration,
            paymentMethod = pattern.paymentMethod,
            currency = pattern.currency,
            tags = pattern.tags
        )
    }

    fun clearTemplatePrefill() {
        _templatePrefill.value = null
    }

    fun savePlannedPaymentFromPattern(pattern: DiscoveredPattern) {
        setPlannedPaymentPrefillFromPattern(pattern)
    }

    fun setPlannedPaymentPrefillFromPattern(pattern: DiscoveredPattern) {
        val template = com.osscabinet.ossWallet.domain.model.Template(
            id = 0,
            name = pattern.name.ifBlank { pattern.payee.ifBlank { "Planned Payment" } },
            categoryId = pattern.categoryId.takeIf { it != 0L } ?: Category.SYSTEM_OTHERS,
            amount = pattern.averageAmount,
            entryMode = pattern.entryMode,
            accountId = pattern.accountId ?: accounts.value.firstOrNull()?.id,
            currency = pattern.currency,
            note = pattern.note.ifBlank { null },
            payee = pattern.payee.ifBlank { null },
            gpsLocation = pattern.gpsLocation,
            warrantyExpiration = pattern.warrantyExpiration,
            paymentMethod = pattern.paymentMethod,
            tags = pattern.tags,
            profileId = 0
        )
        _plannedPaymentPrefill.value = PlannedPaymentPrefillDTO(
            template = template,
            schedule = PaymentSchedule(
                recurrenceRule = pattern.recurrenceRule ?: RecurrenceRule(RecurrenceType.MONTHLY, 1),
                startDate = System.currentTimeMillis(),
                nextDueDate = pattern.lastDate
            )
        )
    }

    fun setPlannedPaymentPrefillFromTemplate(template: com.osscabinet.ossWallet.domain.model.Template) {
        _plannedPaymentPrefill.value = PlannedPaymentPrefillDTO(
            template = template,
            schedule = PaymentSchedule(
                recurrenceRule = RecurrenceRule(RecurrenceType.MONTHLY, 1),
                startDate = System.currentTimeMillis(),
                nextDueDate = System.currentTimeMillis()
            )
        )
    }

    fun clearPlannedPaymentPrefill() {
        _plannedPaymentPrefill.value = null
    }
}
