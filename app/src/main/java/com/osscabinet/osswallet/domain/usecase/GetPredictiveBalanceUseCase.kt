package com.osscabinet.ossWallet.domain.usecase

import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar
import java.util.concurrent.TimeUnit

class GetPredictiveBalanceUseCase(
    private val repository: TransactionRepository
) {
    /**
     * Returns a Flow of predicted balances for the next [days] days.
     * The result is a list of pairs where the first element is the timestamp (start of day)
     * and the second element is the projected balance.
     */
    operator fun invoke(days: Int = 30, profileId: Int = 1): Flow<List<Pair<Long, BigDecimal>>> {
        return repository.getAllTransactions(profileId).map { transactions ->
            if (transactions.isEmpty()) return@map emptyList()

            val currentBalance = transactions.fold(BigDecimal.ZERO) { acc, t ->
                if (t.isIncome) acc.add(t.amount) else acc.subtract(t.amount)
            }
            val avgDailyNetFlow = calculateAvgDailyNetFlow(transactions)

            val predictions = mutableListOf<Pair<Long, BigDecimal>>()
            val calendar = Calendar.getInstance()
            // Start from today
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            for (i in 1..days) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                val projectedBalance = currentBalance.add(avgDailyNetFlow.multiply(BigDecimal(i)))
                predictions.add(calendar.timeInMillis to projectedBalance)
            }
            predictions
        }
    }

    private fun calculateAvgDailyNetFlow(transactions: List<Transaction>): BigDecimal {
        if (transactions.isEmpty()) return BigDecimal.ZERO

        val sortedTransactions = transactions.sortedBy { it.timestamp }
        val firstTimestamp = sortedTransactions.first().timestamp
        val lastTimestamp = System.currentTimeMillis()

        val totalDays = TimeUnit.MILLISECONDS.toDays(lastTimestamp - firstTimestamp).coerceAtLeast(1)
        val totalNetFlow = transactions.fold(BigDecimal.ZERO) { acc, t ->
            if (t.isIncome) acc.add(t.amount) else acc.subtract(t.amount)
        }

        return totalNetFlow.divide(BigDecimal(totalDays), 2, RoundingMode.HALF_UP)
    }
}
