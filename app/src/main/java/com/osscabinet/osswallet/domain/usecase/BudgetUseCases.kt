package com.osscabinet.ossWallet.domain.usecase

import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.model.BudgetPeriod
import com.osscabinet.ossWallet.domain.repository.BudgetRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import com.osscabinet.ossWallet.domain.service.FilterService
import com.osscabinet.ossWallet.domain.service.TimelineService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.math.BigDecimal
import java.util.Calendar
import java.util.concurrent.TimeUnit

data class BudgetAnalysis(
    val budget: Budget,
    val dailySafeSpend: BigDecimal,
    val isHighRisk: Boolean,
    val daysRemaining: Int,
    val totalDays: Int,
    val fixedSpent: BigDecimal = BigDecimal.ZERO,
    val variableSpent: BigDecimal = BigDecimal.ZERO
)

class BudgetUseCases(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository
) {
    fun getBudgets(): Flow<List<Budget>> = budgetRepository.getBudgets()

    suspend fun getBudgetById(id: String) = budgetRepository.getBudgetById(id)

    suspend fun upsertBudget(budget: Budget) = budgetRepository.upsertBudget(budget)

    suspend fun deleteBudget(id: String) = budgetRepository.deleteBudget(id)

    fun getBudgetsWithAnalysis(profileId: Int = 1): Flow<List<BudgetAnalysis>> {
        return combine(
            budgetRepository.getBudgets(),
            transactionRepository.getAllTransactions(profileId)
        ) { budgets, transactions ->
            budgets.map { budget ->
                // Evaluate current spent
                val matchingTransactions = FilterService.filterTransactions(transactions, budget.filter)
                val expenseTransactions = matchingTransactions.filter { it.transactionType == "expense" }
                
                val fixedSpent = expenseTransactions
                    .filter { it.recurring != null || it.tags.any { tag -> tag.equals("fixed", ignoreCase = true) } }
                    .sumOf { it.amount.abs() }
                
                val variableSpent = expenseTransactions
                    .filter { it.recurring == null && it.tags.none { tag -> tag.equals("fixed", ignoreCase = true) } }
                    .sumOf { it.amount.abs() }

                val totalSpent = fixedSpent.add(variableSpent)
                val updatedBudget = budget.copy(spent = totalSpent)
                
                // Calculate metrics
                val (start, end) = getBudgetRange(updatedBudget)
                val now = System.currentTimeMillis()
                
                val totalDays = TimeUnit.MILLISECONDS.toDays(end - start).toInt().coerceAtLeast(1)
                val daysElapsed = TimeUnit.MILLISECONDS.toDays(now - start).toInt().coerceAtLeast(0)
                val daysRemaining = (totalDays - daysElapsed).coerceAtLeast(0)
                
                // For high risk, we prioritize variable spending velocity
                // If variable spent > (variable allowance * expected progress) + margin
                val variableAllowance = updatedBudget.limit.subtract(fixedSpent).coerceAtLeast(BigDecimal.ZERO)
                val expectedVariableProgress = if (totalDays > 0) daysElapsed.toFloat() / totalDays.toFloat() else 0f
                val currentVariableProgress = if (variableAllowance.signum() > 0) {
                    variableSpent.divide(variableAllowance, 4, java.math.RoundingMode.HALF_UP).toFloat()
                } else {
                    if (variableSpent.signum() > 0) 1f else 0f
                }

                val isHighRisk = currentVariableProgress > (expectedVariableProgress + 0.15f) || updatedBudget.isOverBudget

                BudgetAnalysis(
                    budget = updatedBudget,
                    dailySafeSpend = updatedBudget.calculateDailySafeSpend(daysRemaining),
                    isHighRisk = isHighRisk,
                    daysRemaining = daysRemaining,
                    totalDays = totalDays,
                    fixedSpent = fixedSpent,
                    variableSpent = variableSpent
                )
            }
        }
    }

    private fun getBudgetRange(budget: Budget): Pair<Long, Long> {
        val now = Calendar.getInstance()
        return when (budget.period) {
            BudgetPeriod.WEEKLY -> {
                now.set(Calendar.DAY_OF_WEEK, now.firstDayOfWeek)
                val start = now.timeInMillis
                now.add(Calendar.DAY_OF_YEAR, 7)
                Pair(start, now.timeInMillis)
            }
            BudgetPeriod.MONTHLY -> {
                now.set(Calendar.DAY_OF_MONTH, 1)
                val start = now.timeInMillis
                now.add(Calendar.MONTH, 1)
                Pair(start, now.timeInMillis)
            }
            BudgetPeriod.CUSTOM -> {
                Pair(budget.startDate ?: 0L, budget.endDate ?: System.currentTimeMillis())
            }
        }
    }
}
