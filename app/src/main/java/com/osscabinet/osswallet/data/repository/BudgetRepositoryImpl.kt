package com.osscabinet.ossWallet.data.repository

import com.osscabinet.ossWallet.data.local.dao.BudgetDao
import com.osscabinet.ossWallet.data.local.entity.BudgetEntity
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.repository.BudgetRepository
import com.osscabinet.ossWallet.ui.widget.WidgetUpdateHelper
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import android.content.Context

class BudgetRepositoryImpl(
    private val budgetDao: BudgetDao,
    private val context: Context
) : BudgetRepository {

    @OptIn(DelicateCoroutinesApi::class)
    private fun updateWidgets() {
        GlobalScope.launch {
            WidgetUpdateHelper.updateAllWidgets(context)
        }
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
