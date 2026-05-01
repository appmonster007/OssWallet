package com.osscabinet.ossWallet.domain.repository

import com.osscabinet.ossWallet.domain.model.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun getActiveBudgets(profileId: Int): Flow<List<Budget>>
    fun getAllBudgets(profileId: Int): Flow<List<Budget>>
    suspend fun getActiveBudgetsSync(profileId: Int): List<Budget>
    suspend fun getById(id: Long): Budget?
    suspend fun insert(budget: Budget): Long
    suspend fun update(budget: Budget)
    suspend fun delete(id: Long)
}
