package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.model.BudgetPeriod
import com.osscabinet.ossWallet.domain.model.BudgetTrackType
import com.osscabinet.ossWallet.domain.repository.BudgetRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal

class BudgetViewModel(
    private val budgetRepository: BudgetRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val budgets: StateFlow<List<Budget>> = settingsRepository.currentProfileId
        .flatMapLatest { profileId ->
            budgetRepository.getAllBudgets(profileId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun save(budget: Budget) {
        viewModelScope.launch {
            if (budget.id == 0L) {
                budgetRepository.insert(budget)
            } else {
                budgetRepository.update(budget)
            }
        }
    }

    fun delete(budgetId: Long) {
        viewModelScope.launch {
            budgetRepository.delete(budgetId)
        }
    }

    fun archive(budget: Budget) {
        viewModelScope.launch {
            budgetRepository.update(budget.copy(isArchived = true))
        }
    }

    fun buildDefault(profileId: Int): Budget = Budget(
        id = 0L,
        name = "",
        currency = "USD",
        limitAmount = BigDecimal.ZERO,
        period = BudgetPeriod.MONTHLY,
        categoryIds = emptyList(),
        accountIds = emptyList(),
        alertThreshold = 0.8f,
        rollover = false,
        trackType = BudgetTrackType.EXPENSE,
        isArchived = false,
        profileId = profileId
    )
}
