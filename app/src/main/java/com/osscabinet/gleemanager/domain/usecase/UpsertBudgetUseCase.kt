package com.osscabinet.gleemanager.domain.usecase

import com.osscabinet.gleemanager.domain.model.Budget
import com.osscabinet.gleemanager.domain.repository.BudgetRepository

class UpsertBudgetUseCase(private val repository: BudgetRepository) {
    suspend operator fun invoke(budget: Budget) = repository.upsertBudget(budget)
}
