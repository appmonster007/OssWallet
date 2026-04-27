package com.osscabinet.ossWallet.domain.repository

import com.osscabinet.ossWallet.domain.model.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun getBudgets(): Flow<List<Budget>>
    suspend fun getBudgetById(id: String): Budget?
    suspend fun upsertBudget(budget: Budget)
    suspend fun deleteBudget(id: String)
    suspend fun evaluateBudgets() // Trigger re-calculation
}
