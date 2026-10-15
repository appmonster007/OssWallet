package com.osscabinet.ossWallet.ui.screens.goals

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Goal
import com.osscabinet.ossWallet.domain.model.GoalProgress
import com.osscabinet.ossWallet.domain.repository.GoalRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.service.goal.GoalService
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    settingsRepository: SettingsRepository
) : BaseViewModel(settingsRepository) {

    val goals: StateFlow<List<GoalProgress>> = settingsRepository.currentProfileId
        .flatMapLatest { profileId -> goalRepository.getAllGoals(profileId) }
        .map { list ->
            val now = System.currentTimeMillis()
            list.map { GoalService.progress(it, now) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val baseCurrency: StateFlow<String> = settingsRepository.currency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "USD")

    /** Creates a goal, or updates [existing] keeping its saved amount and creation date. */
    fun save(name: String, target: BigDecimal, currency: String, deadline: Long?, existing: Goal?) {
        viewModelScope.launch {
            withProfile { profileId ->
                val now = System.currentTimeMillis()
                if (existing == null) {
                    goalRepository.insertGoal(
                        Goal(
                            name = name.trim(), targetAmount = target, currency = currency,
                            deadline = deadline, profileId = profileId, createdAt = now
                        )
                    )
                } else {
                    // Re-run completion so lowering/raising the target re-stamps correctly.
                    goalRepository.updateGoal(
                        GoalService.applyContribution(
                            existing.copy(name = name.trim(), targetAmount = target, deadline = deadline),
                            BigDecimal.ZERO, now
                        )
                    )
                }
            }
        }
    }

    fun contribute(goal: Goal, amount: BigDecimal) {
        viewModelScope.launch {
            goalRepository.updateGoal(GoalService.applyContribution(goal, amount, System.currentTimeMillis()))
        }
    }

    fun delete(goal: Goal) {
        viewModelScope.launch { goalRepository.deleteGoal(goal.id, goal.profileId) }
    }
}
