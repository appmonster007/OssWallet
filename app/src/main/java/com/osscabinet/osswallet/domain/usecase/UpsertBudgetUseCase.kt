package com.osscabinet.ossWallet.domain.usecase

import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.repository.BudgetRepository

class UpsertBudgetUseCase(private val repository: BudgetRepository) {
    suspend operator fun invoke(budget: Budget) = repository.upsertBudget(budget)
}
