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
                    val usedCurrencies = transactions.map { it.currency }.distinct().toSet()
                    val allCurrencies = if (usedCurrencies.isEmpty()) setOf("USD") else usedCurrencies
                    _state.update {
                        it.copy(
                            accounts = accounts,
                            categories = categories,
                            allTransactions = transactions,
                            availableTags = tags,
                            uniqueNotes = uniqueNotes,
                            currentProfileId = profileId,
                            availableFilters = filters,
                            selectedAccountIds = if (it.isEditMode) it.selectedAccountIds else if (it.selectedAccountIds.isEmpty()) accounts.map { a -> a.id }.toSet() else it.selectedAccountIds,
                            selectedCategoryIds = if (it.isEditMode) it.selectedCategoryIds else if (it.selectedCategoryIds.isEmpty()) categories.map { c -> c.id }.toSet() else it.selectedCategoryIds,
                            selectedPaymentMethods = if (it.isEditMode) it.selectedPaymentMethods else if (it.selectedPaymentMethods.isEmpty()) PaymentMethod.entries.toSet() else it.selectedPaymentMethods,
                            selectedStatuses = if (it.isEditMode) it.selectedStatuses else if (it.selectedStatuses.isEmpty()) TransactionStatus.entries.toSet() else it.selectedStatuses,
                            selectedTrackTypes = if (it.isEditMode) it.selectedTrackTypes else if (it.selectedTrackTypes.isEmpty()) setOf(TransactionType.EXPENSE, TransactionType.INCOME) else it.selectedTrackTypes,
                            selectedCurrencies = if (it.isEditMode) it.selectedCurrencies else if (it.selectedCurrencies.isEmpty()) allCurrencies else it.selectedCurrencies,
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
                val usedCurrencies = it.allTransactions.map { t -> t.currency }.distinct().toSet()
                it.copy(
                    isEditMode = false, 
                    budgetId = null,
                    selectedAccountIds = it.accounts.map { a -> a.id }.toSet(),
                    selectedCategoryIds = it.categories.map { c -> c.id }.toSet(),
                    selectedPaymentMethods = PaymentMethod.entries.toSet(),
                    selectedStatuses = TransactionStatus.entries.toSet(),
                    selectedTrackTypes = setOf(TransactionType.EXPENSE, TransactionType.INCOME),
                    selectedCurrencies = if (usedCurrencies.isEmpty()) setOf("USD") else usedCurrencies,
                    selectedTags = emptySet()
                ) 
            }
            return
        }

        viewModelScope.launch {
            budgetRepository.getById(budgetId)?.let { budget ->
                val filter = budget.filterId?.let { id -> filterRepository.getFilterById(id) }
                val usageCount = budget.filterId?.let { filterRepository.getFilterUsageCount(it) } ?: 0
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
                        period = budget.period,
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
                        customStartDate = budget.customStartDate ?: System.currentTimeMillis(),
                        customEndDate = budget.customEndDate ?: System.currentTimeMillis(),
                        filterId = budget.filterId
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
            is BudgetEntryEvent.SetPeriod -> _state.update { it.copy(period = event.period) }
            is BudgetEntryEvent.SetTrackTypes -> _state.update { it.copy(selectedTrackTypes = event.types) }
            is BudgetEntryEvent.SetAlertThreshold -> _state.update { it.copy(alertThreshold = event.threshold) }
            is BudgetEntryEvent.SetRollover -> _state.update { it.copy(rollover = event.rollover) }
            is BudgetEntryEvent.SetNote -> _state.update { it.copy(note = event.note) }
            is BudgetEntryEvent.SetCategoryIds -> _state.update { it.copy(selectedCategoryIds = event.ids) }
            is BudgetEntryEvent.SetAccountIds -> _state.update { it.copy(selectedAccountIds = event.ids) }
            is BudgetEntryEvent.SetCurrencies -> _state.update { it.copy(selectedCurrencies = event.currencies) }
            is BudgetEntryEvent.SetTags -> _state.update { it.copy(selectedTags = event.tags) }
            is BudgetEntryEvent.SetCustomStartDate -> _state.update { it.copy(customStartDate = event.date) }
            is BudgetEntryEvent.SetCustomEndDate -> _state.update { it.copy(customEndDate = event.date) }
            is BudgetEntryEvent.ShowCustomStartPicker -> _state.update { it.copy(showCustomStartPicker = event.show) }
            is BudgetEntryEvent.ShowCustomEndPicker -> _state.update { it.copy(showCustomEndPicker = event.show) }
            is BudgetEntryEvent.ShowNumpad -> _state.update { it.copy(showNumpad = event.show) }
            is BudgetEntryEvent.ShowAccountSheet -> _state.update { it.copy(showAccountSheet = event.show) }
            is BudgetEntryEvent.ShowTrackTypeSheet -> _state.update { it.copy(showTrackTypeSheet = event.show) }
            is BudgetEntryEvent.ShowCategorySheet -> _state.update { it.copy(showCategorySheet = event.show) }
            is BudgetEntryEvent.ShowTagSheet -> _state.update { it.copy(showTagSheet = event.show) }
            is BudgetEntryEvent.ShowPeriodSheet -> _state.update { it.copy(showPeriodSheet = event.show) }
            is BudgetEntryEvent.ShowCurrencyFilterSheet -> _state.update { it.copy(showCurrencyFilterSheet = event.show) }
            is BudgetEntryEvent.ShowPaymentMethodSheet -> _state.update { it.copy(showPaymentMethodSheet = event.show) }
            is BudgetEntryEvent.ShowStatusSheet -> _state.update { it.copy(showStatusSheet = event.show) }
            is BudgetEntryEvent.ShowLinkSheet -> _state.update { it.copy(showLinkSheet = event.show) }
            is BudgetEntryEvent.ShowFilterSheet -> _state.update { it.copy(showFilterSheet = event.show) }
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
                    transactionTypes = s.selectedTrackTypes.toList()
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
                period = s.period,
                profileId = s.currentProfileId
            )).copy(
                id = s.budgetId ?: 0L,
                name = s.name,
                limitAmount = amount,
                currency = s.currency,
                period = s.period,
                trackTypes = s.selectedTrackTypes.toList().ifEmpty { listOf(TransactionType.EXPENSE) },
                alertThreshold = s.alertThreshold,
                rollover = s.rollover,
                categoryIds = s.selectedCategoryIds.toList(),
                accountIds = s.selectedAccountIds.toList(),
                tags = s.selectedTags,
                paymentMethods = s.selectedPaymentMethods.toList(),
                statuses = s.selectedStatuses.toList(),
                linkedTransactionIds = s.linkedTransactionIds.toList(),
                customStartDate = if (s.period == com.osscabinet.ossWallet.domain.model.BudgetPeriod.CUSTOM) s.customStartDate else null,
                customEndDate = if (s.period == com.osscabinet.ossWallet.domain.model.BudgetPeriod.CUSTOM) s.customEndDate else null,
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
