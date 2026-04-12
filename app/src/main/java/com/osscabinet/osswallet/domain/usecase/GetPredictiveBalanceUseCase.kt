package com.osscabinet.ossWallet.domain.usecase

import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
    operator fun invoke(days: Int = 30, profileId: Int = 1): Flow<List<Pair<Long, Double>>> {
        return repository.getAllTransactions(profileId).map { transactions ->
            if (transactions.isEmpty()) return@map emptyList()

            val currentBalance = transactions.sumOf { if (it.isIncome) it.amount else -it.amount }
            val avgDailyNetFlow = calculateAvgDailyNetFlow(transactions)

            val predictions = mutableListOf<Pair<Long, Double>>()
            val calendar = Calendar.getInstance()
            // Start from today
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            for (i in 1..days) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                val projectedBalance = currentBalance + (avgDailyNetFlow * i)
                predictions.add(calendar.timeInMillis to projectedBalance)
            }
            predictions
        }
    }

    private fun calculateAvgDailyNetFlow(transactions: List<Transaction>): Double {
        if (transactions.isEmpty()) return 0.0

        val sortedTransactions = transactions.sortedBy { it.timestamp }
        val firstTimestamp = sortedTransactions.first().timestamp
        val lastTimestamp = System.currentTimeMillis()

        val totalDays = TimeUnit.MILLISECONDS.toDays(lastTimestamp - firstTimestamp).coerceAtLeast(1)
        val totalNetFlow = transactions.sumOf { if (it.isIncome) it.amount else -it.amount }

        return totalNetFlow / totalDays.toDouble()
    }
}
