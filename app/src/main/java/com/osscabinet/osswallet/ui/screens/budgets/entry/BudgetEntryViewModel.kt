package com.osscabinet.ossWallet.ui.screens.budgets.entry

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Budget
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
                    transactionRepository.getAllTransactions(profileId)
                ) { accounts, categories, tags, transactions ->
                    val uniqueNotes = transactions.map { it.note }.filter { it.isNotBlank() }.distinct().sorted()
                    _state.update {
                        it.copy(
                            accounts = accounts,
                            categories = categories,
                            availableTags = tags,
                            uniqueNotes = uniqueNotes,
                            currentProfileId = profileId
                        )
                    }
                }
            }.collect()
        }
    }

    fun initialize(budgetId: Long? = null) {
        if (budgetId == null) {
            _state.update { it.copy(isEditMode = false, budgetId = null) }
            return
        }

        viewModelScope.launch {
            budgetRepository.getById(budgetId)?.let { budget ->
                _state.update {
                    it.copy(
                        budgetId = budgetId,
                        isEditMode = true,
                        existingBudget = budget,
                        name = budget.name,
                        limitAmount = budget.limitAmount.toPlainString(),
                        currency = budget.currency,
                        period = budget.period,
                        selectedTrackTypes = budget.trackTypes.toSet(),
                        alertThreshold = budget.alertThreshold,
                        rollover = budget.rollover,
                        note = "", // note is not in Budget model?
                        selectedCategoryIds = budget.categoryIds.toSet(),
                        selectedAccountIds = budget.accountIds.toSet(),
                        selectedTags = budget.tags.toSet(),
                        customStartDate = budget.customStartDate ?: System.currentTimeMillis(),
                        customEndDate = budget.customEndDate ?: System.currentTimeMillis()
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
            is BudgetEntryEvent.SetAccountQuery -> _state.update { it.copy(accountQuery = event.query) }
            is BudgetEntryEvent.SetCategoryQuery -> _state.update { it.copy(categoryQuery = event.query) }
            is BudgetEntryEvent.TriggerNavigation -> _state.update { it.copy(navTriggered = true) }
        }
    }

    fun saveBudget(onSuccess: () -> Unit) {
        val s = _state.value
        if (!s.canSave) return

        viewModelScope.launch {
            val budget = (s.existingBudget ?: Budget(
                id = 0,
                name = s.name,
                limitAmount = s.limitAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                currency = s.currency,
                period = s.period,
                profileId = s.currentProfileId
            )).copy(
                id = s.budgetId ?: 0L,
                name = s.name,
                limitAmount = s.limitAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                currency = s.currency,
                period = s.period,
                trackTypes = s.selectedTrackTypes.toList().ifEmpty { listOf(com.osscabinet.ossWallet.domain.model.BudgetTrackType.EXPENSE) },
                alertThreshold = s.alertThreshold,
                rollover = s.rollover,
                categoryIds = s.selectedCategoryIds.toList(),
                accountIds = s.selectedAccountIds.toList(),
                tags = s.selectedTags.toList(),
                customStartDate = if (s.period == com.osscabinet.ossWallet.domain.model.BudgetPeriod.CUSTOM) s.customStartDate else null,
                customEndDate = if (s.period == com.osscabinet.ossWallet.domain.model.BudgetPeriod.CUSTOM) s.customEndDate else null,
                profileId = s.currentProfileId
            )

            if (s.isEditMode) {
                budgetRepository.update(budget)
            } else {
                budgetRepository.insert(budget)
            }
            onSuccess()
        }
    }

    fun deleteBudget(onSuccess: () -> Unit) {
        val id = _state.value.budgetId ?: return
        viewModelScope.launch {
            budgetRepository.delete(id)
            onSuccess()
        }
    }
}
