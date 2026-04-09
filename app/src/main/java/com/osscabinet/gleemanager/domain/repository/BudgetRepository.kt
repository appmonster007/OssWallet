package com.osscabinet.gleemanager.domain.repository

import com.osscabinet.gleemanager.domain.model.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun getBudgets(): Flow<List<Budget>>
    suspend fun upsertBudget(budget: Budget)
    suspend fun deleteBudget(category: String)
}
