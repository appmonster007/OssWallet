package com.osscabinet.ossWallet.ui.screens.budgets.entry

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.repository.*
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetEntryViewModel(
    private val budgetRepository: BudgetRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val tagRepository: TagRepository,
    private val transactionRepository: TransactionRepository,
    private val filterRepository: FilterRepository,
    settingsRepository: SettingsRepository
) : BaseViewModel(settingsRepository) {

    private val _state = MutableStateFlow(BudgetEntryState())
    val state: StateFlow<BudgetEntryState> = _state.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            settingsRepository.currentProfileId.flatMapLatest { profileId ->
                combine(
                    accountRepository.getAllAccounts(profileId),
                    categoryRepository.categories,
                    tagRepository.getAllTags(),
                    transactionRepository.getAllTransactions(profileId),
                    filterRepository.getAllFilters()
                ) { accounts, categories, tags, transactions, filters ->
                    val uniqueNotes = transactions.map { it.note }.filter { it.isNotBlank() }.distinct().sorted()
                    _state.update {
                        it.copy(
                            accounts = accounts,
                            categories = categories,
                            allTransactions = transactions,
                            availableTags = tags,
                            uniqueNotes = uniqueNotes,
                            currentProfileId = profileId,
                            availableFilters = filters,
                            selectedAccountIds = if (it.isEditMode) it.selectedAccountIds else emptySet(),
                            selectedCategoryIds = if (it.isEditMode) it.selectedCategoryIds else emptySet(),
                            selectedPaymentMethods = if (it.isEditMode) it.selectedPaymentMethods else emptySet(),
                            selectedStatuses = if (it.isEditMode) it.selectedStatuses else emptySet(),
                            selectedTrackTypes = if (it.isEditMode) it.selectedTrackTypes else setOf(TransactionType.EXPENSE, TransactionType.INCOME),
                            selectedCurrencies = if (it.isEditMode) it.selectedCurrencies else emptySet(),
                            selectedTags = if (it.isEditMode) it.selectedTags else emptySet()
                        )
                    }
                }
            }.collect()
        }
    }

    fun initialize(budgetId: Long? = null) {
        if (budgetId == null) {
            _state.update { 
                it.copy(
                    isEditMode = false, 
                    budgetId = null,
                    selectedAccountIds = emptySet(),
                    selectedCategoryIds = emptySet(),
                    selectedPaymentMethods = emptySet(),
                    selectedStatuses = emptySet(),
                    selectedTrackTypes = setOf(TransactionType.EXPENSE, TransactionType.INCOME),
                    selectedCurrencies = emptySet(),
                    selectedTags = emptySet(),
                    queryNote = "",
                    queryPayee = "",
                    isNoteRegex = false,
                    isPayeeRegex = false,
                    minAmount = "",
                    maxAmount = "",
                    hasWarranty = null,
                    warrantyExpirationStart = null,
                    warrantyExpirationEnd = null,
                    excludeFuture = true
                ) 
            }
            return
        }

        viewModelScope.launch {
            budgetRepository.getById(budgetId)?.let { budget ->
                val filter = budget.filterId?.let { id -> filterRepository.getFilterById(id) }
                val usageCount = budget.filterId?.let { filterRepository.getFilterUsageCount(it) } ?: 0
                val tf = filter?.filter ?: TransactionFilter()
                _state.update {
                    it.copy(
                        budgetId = budgetId,
                        isEditMode = true,
                        existingBudget = budget,
                        linkedFilter = filter,
                        filterUsageCount = usageCount,
                        name = budget.name,
                        limitAmount = budget.limitAmount.toPlainString(),
                        currency = budget.currency,
                        timelineRange = budget.timelineRange,
                        selectedTrackTypes = budget.trackTypes.toSet(),
                        selectedCurrencies = budget.currencies.toSet(),
                        alertThreshold = budget.alertThreshold,
                        rollover = budget.rollover,
                        selectedCategoryIds = budget.categoryIds.toSet(),
                        selectedAccountIds = budget.accountIds.toSet(),
                        selectedTags = budget.tags.toSet(),
                        selectedPaymentMethods = budget.paymentMethods.toSet(),
                        selectedStatuses = budget.statuses.toSet(),
                        linkedTransactionIds = budget.linkedTransactionIds.toSet(),
                        filterId = budget.filterId,
                        queryNote = tf.queryNote ?: "",
                        isNoteRegex = tf.isNoteRegex,
                        queryPayee = tf.queryPayee ?: "",
                        isPayeeRegex = tf.isPayeeRegex,
                        minAmount = tf.minAmount?.toPlainString() ?: "",
                        maxAmount = tf.maxAmount?.toPlainString() ?: "",
                        hasWarranty = tf.hasWarranty,
                        warrantyExpirationStart = tf.warrantyExpirationStart,
                        warrantyExpirationEnd = tf.warrantyExpirationEnd,
                        excludeFuture = tf.excludeFuture
                    )
                }
            }
        }
    }

    fun onEvent(event: BudgetEntryEvent) {
        when (event) {
            is BudgetEntryEvent.SetName -> _state.update { it.copy(name = event.name) }
            is BudgetEntryEvent.SetLimitAmount -> _state.update { it.copy(limitAmount = event.amount) }
            is BudgetEntryEvent.SetCurrency -> _state.update { it.copy(currency = event.currency) }
            is BudgetEntryEvent.SetTimelineRange -> _state.update { it.copy(timelineRange = event.range) }
            is BudgetEntryEvent.SetTrackTypes -> _state.update { it.copy(selectedTrackTypes = event.types) }
            is BudgetEntryEvent.SetAlertThreshold -> _state.update { it.copy(alertThreshold = event.threshold) }
            is BudgetEntryEvent.SetRollover -> _state.update { it.copy(rollover = event.rollover) }
            is BudgetEntryEvent.SetNote -> _state.update { it.copy(note = event.note) }
            is BudgetEntryEvent.SetCategoryIds -> _state.update { it.copy(selectedCategoryIds = event.ids) }
            is BudgetEntryEvent.SetAccountIds -> _state.update { it.copy(selectedAccountIds = event.ids) }
            is BudgetEntryEvent.SetCurrencies -> _state.update { it.copy(selectedCurrencies = event.currencies) }
            is BudgetEntryEvent.SetTags -> _state.update { it.copy(selectedTags = event.tags) }
            is BudgetEntryEvent.SetQueryNote -> _state.update { it.copy(queryNote = event.query) }
            is BudgetEntryEvent.SetNoteRegex -> _state.update { it.copy(isNoteRegex = event.isRegex) }
            is BudgetEntryEvent.SetQueryPayee -> _state.update { it.copy(queryPayee = event.query) }
            is BudgetEntryEvent.SetPayeeRegex -> _state.update { it.copy(isPayeeRegex = event.isRegex) }
            is BudgetEntryEvent.SetMinAmount -> _state.update { it.copy(minAmount = event.amount) }
            is BudgetEntryEvent.SetMaxAmount -> _state.update { it.copy(maxAmount = event.amount) }
            is BudgetEntryEvent.SetHasWarranty -> _state.update { it.copy(hasWarranty = event.hasWarranty) }
            is BudgetEntryEvent.SetWarrantyExpirationStart -> _state.update { it.copy(warrantyExpirationStart = event.date) }
            is BudgetEntryEvent.SetWarrantyExpirationEnd -> _state.update { it.copy(warrantyExpirationEnd = event.date) }
            is BudgetEntryEvent.SetExcludeFuture -> _state.update { it.copy(excludeFuture = event.exclude) }
            is BudgetEntryEvent.ShowNumpad -> _state.update { it.copy(showNumpad = event.show) }
            is BudgetEntryEvent.ShowTimelineSheet -> _state.update { it.copy(showTimelineSheet = event.show) }
            is BudgetEntryEvent.ShowAccountSheet -> _state.update { it.copy(showAccountSheet = event.show) }
            is BudgetEntryEvent.ShowTrackTypeSheet -> _state.update { it.copy(showTrackTypeSheet = event.show) }
            is BudgetEntryEvent.ShowCategorySheet -> _state.update { it.copy(showCategorySheet = event.show) }
            is BudgetEntryEvent.ShowTagSheet -> _state.update { it.copy(showTagSheet = event.show) }
            is BudgetEntryEvent.ShowCurrencyFilterSheet -> _state.update { it.copy(showCurrencyFilterSheet = event.show) }
            is BudgetEntryEvent.ShowPaymentMethodSheet -> _state.update { it.copy(showPaymentMethodSheet = event.show) }
            is BudgetEntryEvent.ShowStatusSheet -> _state.update { it.copy(showStatusSheet = event.show) }
            is BudgetEntryEvent.ShowLinkSheet -> _state.update { it.copy(showLinkSheet = event.show) }
            is BudgetEntryEvent.ShowFilterSheet -> _state.update { it.copy(showFilterSheet = event.show) }
            is BudgetEntryEvent.ShowMinAmountSheet -> _state.update { it.copy(showMinAmountSheet = event.show) }
            is BudgetEntryEvent.ShowMaxAmountSheet -> _state.update { it.copy(showMaxAmountSheet = event.show) }
            is BudgetEntryEvent.ShowWarrantyStartPicker -> _state.update { it.copy(showWarrantyStartPicker = event.show) }
            is BudgetEntryEvent.ShowWarrantyEndPicker -> _state.update { it.copy(showWarrantyEndPicker = event.show) }
            is BudgetEntryEvent.SetPaymentMethods -> _state.update { it.copy(selectedPaymentMethods = event.methods) }
            is BudgetEntryEvent.SetStatuses -> _state.update { it.copy(selectedStatuses = event.statuses) }
            is BudgetEntryEvent.SetLinkedTransactions -> _state.update { it.copy(linkedTransactionIds = event.ids) }
            is BudgetEntryEvent.SetFilterId -> {
                val filter = _state.value.availableFilters.find { it.id == event.filterId }
                _state.update { state ->
                    if (filter != null) {
                        state.copy(
                            filterId = null, // DELINK
                            linkedFilter = null, // DELINK
                            selectedAccountIds = filter.filter.accountIds.toSet(),
                            selectedCategoryIds = filter.filter.categoryIds.toSet(),
                            selectedTags = filter.filter.tags.toSet(),
                            selectedPaymentMethods = filter.filter.paymentMethods.toSet(),
                            selectedStatuses = filter.filter.statuses.toSet(),
                            selectedCurrencies = filter.filter.currencies.toSet(),
                            linkedTransactionIds = filter.filter.linkedTransactionIds.toSet(),
                            queryNote = filter.filter.queryNote ?: "",
                            isNoteRegex = filter.filter.isNoteRegex,
                            queryPayee = filter.filter.queryPayee ?: "",
                            isPayeeRegex = filter.filter.isPayeeRegex,
                            minAmount = filter.filter.minAmount?.toPlainString() ?: "",
                            maxAmount = filter.filter.maxAmount?.toPlainString() ?: "",
                            hasWarranty = filter.filter.hasWarranty,
                            warrantyExpirationStart = filter.filter.warrantyExpirationStart,
                            warrantyExpirationEnd = filter.filter.warrantyExpirationEnd,
                            timelineRange = filter.filter.timelineRange,
                            excludeFuture = filter.filter.excludeFuture,
                            selectedTrackTypes = if (filter.filter.transactionTypes.isNotEmpty()) {
                                filter.filter.transactionTypes.toSet()
                            } else {
                                state.selectedTrackTypes
                            }
                        )
                    } else {
                        state.copy(filterId = null, linkedFilter = null, filterUsageCount = 0)
                    }
                }
            }
            BudgetEntryEvent.MakeFilterUnique -> _state.update { it.copy(filterId = null, linkedFilter = null) }
            is BudgetEntryEvent.SetDeleteFilterWithBudget -> _state.update { it.copy(deleteFilterWithBudget = event.delete) }
            is BudgetEntryEvent.ShowDeleteDialog -> _state.update { it.copy(showDeleteDialog = event.show) }
            is BudgetEntryEvent.SetAccountQuery -> _state.update { it.copy(accountQuery = event.query) }
            is BudgetEntryEvent.SetCategoryQuery -> _state.update { it.copy(categoryQuery = event.query) }
            is BudgetEntryEvent.TriggerNavigation -> _state.update { it.copy(navTriggered = true) }
        }
    }

    fun saveBudget(onSuccess: () -> Unit) {
        val s = _state.value
        if (!s.canSave) return

        viewModelScope.launch {
            val amount = s.limitAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO
            
            // 1. Create or Update Filter
            val filter = Filter(
                id = s.linkedFilter?.id ?: 0L,
                name = "Filter for Budget: ${s.name}",
                filter = TransactionFilter(
                    categoryIds = s.selectedCategoryIds.toList(),
                    accountIds = s.selectedAccountIds.toList(),
                    tags = s.selectedTags,
                    paymentMethods = s.selectedPaymentMethods.toList(),
                    statuses = s.selectedStatuses.toList(),
                    currencies = s.selectedCurrencies.toList(),
                    linkedTransactionIds = s.linkedTransactionIds.toList(),
                    transactionTypes = s.selectedTrackTypes.toList(),
                    queryNote = s.queryNote.takeIf { it.isNotBlank() },
                    isNoteRegex = s.isNoteRegex,
                    queryPayee = s.queryPayee.takeIf { it.isNotBlank() },
                    isPayeeRegex = s.isPayeeRegex,
                    minAmount = s.minAmount.toBigDecimalOrNull(),
                    maxAmount = s.maxAmount.toBigDecimalOrNull(),
                    hasWarranty = s.hasWarranty,
                    warrantyExpirationStart = s.warrantyExpirationStart,
                    warrantyExpirationEnd = s.warrantyExpirationEnd,
                    timelineRange = s.timelineRange,
                    excludeFuture = s.excludeFuture
                )
            )

            val filterId = if (filter.id == 0L || s.filterUsageCount > 1) {
                filterRepository.insertFilter(filter.copy(id = 0L))
            } else {
                filterRepository.updateFilter(filter)
                filter.id
            }

            // 2. Create or Update Budget
            val budget = (s.existingBudget ?: Budget(
                id = 0,
                name = s.name,
                limitAmount = amount,
                currency = s.currency,
                timelineRange = s.timelineRange,
                profileId = s.currentProfileId
            )).copy(
                id = s.budgetId ?: 0L,
                name = s.name,
                limitAmount = amount,
                currency = s.currency,
                timelineRange = s.timelineRange,
                trackTypes = s.selectedTrackTypes.toList().ifEmpty { listOf(TransactionType.EXPENSE) },
                alertThreshold = s.alertThreshold,
                rollover = s.rollover,
                categoryIds = s.selectedCategoryIds.toList(),
                accountIds = s.selectedAccountIds.toList(),
                tags = s.selectedTags,
                paymentMethods = s.selectedPaymentMethods.toList(),
                statuses = s.selectedStatuses.toList(),
                linkedTransactionIds = s.linkedTransactionIds.toList(),
                profileId = s.currentProfileId,
                filterId = filterId
            )

            if (s.isEditMode) {
                budgetRepository.update(budget)
            } else {
                budgetRepository.insert(budget)
            }
            onSuccess()
        }
    }

    fun deleteBudget() {
        val id = _state.value.budgetId ?: return
        val filterId = _state.value.filterId
        val deleteFilter = _state.value.deleteFilterWithBudget
        
        viewModelScope.launch {
            if (deleteFilter && filterId != null) {
                filterRepository.getFilterById(filterId)?.let { filter ->
                    filterRepository.deleteFilter(filter)
                }
            } else {
                budgetRepository.delete(id)
            }
            _state.update { it.copy(navTriggered = true) }
        }
    }
}
