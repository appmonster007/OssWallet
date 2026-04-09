package com.osscabinet.gleemanager.domain.usecase

import com.osscabinet.gleemanager.domain.model.Budget
import com.osscabinet.gleemanager.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow

class GetBudgetsUseCase(private val repository: BudgetRepository) {
    operator fun invoke(): Flow<List<Budget>> = repository.getBudgets()
}
