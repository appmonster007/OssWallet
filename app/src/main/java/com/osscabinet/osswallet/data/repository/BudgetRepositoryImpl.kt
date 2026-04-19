package com.osscabinet.ossWallet.data.repository

import com.osscabinet.ossWallet.data.local.dao.BudgetDao
import com.osscabinet.ossWallet.data.local.entity.BudgetEntity
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.repository.BudgetRepository
import com.osscabinet.ossWallet.ui.widget.WidgetUpdateHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import android.content.Context
import android.util.Log

class BudgetRepositoryImpl(
    private val budgetDao: BudgetDao,
    private val context: Context
) : BudgetRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun updateWidgets() {
        // Log.d("BudgetRepo", "Triggering widget update")
        // WidgetUpdateHelper.triggerUpdate(context)
    }

    override fun getBudgets(): Flow<List<Budget>> = budgetDao.getAllBudgets().map { entities ->
        entities.map { it.toDomain() }
    }

    override suspend fun upsertBudget(budget: Budget) {
        budgetDao.upsertBudget(budget.toEntity())
        updateWidgets()
    }

    override suspend fun deleteBudget(category: String) {
        budgetDao.deleteBudget(category)
        updateWidgets()
    }

    private fun BudgetEntity.toDomain() = Budget(category, spent, limit, currency)
    private fun Budget.toEntity() = BudgetEntity(category, spent, limit, currency)
}
