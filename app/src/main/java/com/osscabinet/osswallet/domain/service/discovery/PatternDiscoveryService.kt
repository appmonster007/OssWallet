package com.osscabinet.ossWallet.domain.service.discovery

import com.osscabinet.ossWallet.domain.model.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar

/**
 * Result of the pattern discovery process.
 * Represents a set of transactions that appear to be repeating.
 */
data class DiscoveredPattern(
    val name: String,
    val payee: String,
    val categoryId: Long,
    val accountId: String?,
    val averageAmount: BigDecimal,
    val currency: String,
    val transactionType: TransactionType,
    val paymentMethod: PaymentMethod,
    val note: String,
    val tags: Set<String>,
    val gpsLocation: String? = null,
    val warrantyExpiration: Long?,
    val recurrenceRule: RecurrenceRule?,
    val confidence: Float,
    val occurrences: Int,
    val lastDate: Long,
    val representativeTransactionId: Long,
    val existsAsTemplate: Boolean = false,
    val existsAsPlannedPayment: Boolean = false
)

/**
 * Offline-first heuristic engine for discovering repeating transaction patterns.
 * Analyzes past transactions to suggest Templates and Planned Payments.
 */
object PatternDiscoveryService {

    private const val MIN_OCCURRENCES = 3
    private const val DAY_IN_MILLIS = 24 * 60 * 60 * 1000L
    private const val INTERVAL_TOLERANCE_DAYS = 3

    /**
     * Scans transactions and returns a list of discovered patterns.
     */
    fun discoverPatterns(transactions: List<Transaction>): List<DiscoveredPattern> {
        if (transactions.size < MIN_OCCURRENCES) return emptyList()

        // 1. Group by key features (Payee + Category + Type + Currency)
        val groups = transactions.groupBy { tx ->
            val normalizedPayee = tx.payee.trim().lowercase()
            val payeeKey = if (normalizedPayee.isEmpty()) "note:${tx.note.trim().lowercase()}" else "payee:$normalizedPayee"
            "$payeeKey|${tx.categoryId}|${tx.transactionType}|${tx.currency}"
        }

        return groups.mapNotNull { (_, groupTransactions) ->
            if (groupTransactions.size < MIN_OCCURRENCES) return@mapNotNull null
            
            val sortedTxs = groupTransactions.sortedBy { it.timestamp }
            analyzeGroup(sortedTxs)
        }.sortedByDescending { it.confidence }
    }

    private fun analyzeGroup(txs: List<Transaction>): DiscoveredPattern? {
        val lastTx = txs.last()
        val avgAmount = txs.map { it.amount }.reduce { acc, bigDecimal -> acc.add(bigDecimal) }
            .divide(BigDecimal(txs.size), 2, RoundingMode.HALF_UP)
        
        // Check amount consistency (variance)
        val amountVariance = txs.map { 
            it.amount.subtract(avgAmount).abs() 
        }.reduce { acc, bigDecimal -> acc.add(bigDecimal) }
            .divide(BigDecimal(txs.size), 10, RoundingMode.HALF_UP)
            
        val amountScore = if (avgAmount.compareTo(BigDecimal.ZERO) == 0) 1f 
            else (1f - (amountVariance.divide(avgAmount.abs(), 10, RoundingMode.HALF_UP).toFloat())).coerceIn(0f, 1f)

        // Identify Recurrence
        val recurrenceResult = detectRecurrence(txs)
        val recurrenceScore = recurrenceResult.confidence
        
        // Volume Score: More transactions = higher confidence
        val volumeScore = (txs.size.toFloat() / 10f).coerceIn(0.3f, 1.0f)

        // Total Confidence
        val totalConfidence = (amountScore * 0.2f) + (recurrenceScore * 0.5f) + (volumeScore * 0.3f)

        if (totalConfidence < 0.4f) return null

        return DiscoveredPattern(
            name = lastTx.payee.ifBlank { lastTx.note.ifBlank { "Discovered Pattern" } },
            payee = lastTx.payee,
            categoryId = lastTx.categoryId,
            accountId = lastTx.accountId,
            averageAmount = avgAmount,
            currency = lastTx.currency,
            transactionType = lastTx.transactionType,
            paymentMethod = lastTx.paymentMethod,
            note = lastTx.note,
            tags = lastTx.tags,
            gpsLocation = lastTx.gpsLocation,
            warrantyExpiration = lastTx.warrantyExpiration,
            recurrenceRule = recurrenceResult.rule,
            confidence = totalConfidence,
            occurrences = txs.size,
            lastDate = lastTx.timestamp,
            representativeTransactionId = lastTx.id
        )
    }

    private data class RecurrenceDiscoveryResult(
        val rule: RecurrenceRule?,
        val confidence: Float
    )

    private fun detectRecurrence(txs: List<Transaction>): RecurrenceDiscoveryResult {
        val intervals = mutableListOf<Long>()
        for (i in 0 until txs.size - 1) {
            intervals.add(txs[i + 1].timestamp - txs[i].timestamp)
        }

        val avgIntervalDays = intervals.average() / DAY_IN_MILLIS
        
        return when {
            // Weekly check (7 days +/- tolerance)
            isCloseTo(avgIntervalDays, 7.0) -> {
                RecurrenceDiscoveryResult(
                    rule = RecurrenceRule(RecurrenceType.WEEKLY),
                    confidence = calculateIntervalConsistency(intervals, 7.0)
                )
            }
            // Monthly check (30 days +/- tolerance)
            isCloseTo(avgIntervalDays, 30.0) -> {
                RecurrenceDiscoveryResult(
                    rule = RecurrenceRule(RecurrenceType.MONTHLY),
                    confidence = calculateIntervalConsistency(intervals, 30.44) // Average month
                )
            }
            // Bi-weekly check (14 days +/- tolerance)
            isCloseTo(avgIntervalDays, 14.0) -> {
                RecurrenceDiscoveryResult(
                    rule = RecurrenceRule(RecurrenceType.EVERY_N_WEEKS, interval = 2),
                    confidence = calculateIntervalConsistency(intervals, 14.0)
                )
            }
            // Daily check
            isCloseTo(avgIntervalDays, 1.0) -> {
                RecurrenceDiscoveryResult(
                    rule = RecurrenceRule(RecurrenceType.DAILY),
                    confidence = calculateIntervalConsistency(intervals, 1.0)
                )
            }
            else -> RecurrenceDiscoveryResult(null, 0.2f) // Low confidence if no periodicity
        }
    }

    private fun isCloseTo(value: Double, target: Double): Boolean {
        return Math.abs(value - target) <= INTERVAL_TOLERANCE_DAYS
    }

    private fun calculateIntervalConsistency(intervals: List<Long>, targetDays: Double): Float {
        val targetMillis = targetDays * DAY_IN_MILLIS
        val variances = intervals.map { Math.abs(it - targetMillis).toDouble() }
        val avgVarianceDays = variances.average() / DAY_IN_MILLIS
        
        return (1.0 - (avgVarianceDays / INTERVAL_TOLERANCE_DAYS)).coerceIn(0.0, 1.0).toFloat()
    }

    /**
     * Converts a discovered pattern into a Template.
     */
    fun toTemplate(pattern: DiscoveredPattern): Template {
        return Template(
            id = 0,
            name = pattern.name,
            categoryId = pattern.categoryId,
            amount = pattern.averageAmount,
            transactionType = pattern.transactionType,
            icon = null,
            colorHex = null,
            accountId = pattern.accountId,
            note = pattern.note,
            payee = pattern.payee,
            gpsLocation = pattern.gpsLocation,
            paymentMethod = pattern.paymentMethod,
            currency = pattern.currency,
            tags = pattern.tags,
            isStarred = false,
            displayOrder = 0
        )
    }

    /**
     * Converts a discovered pattern into a Planned Payment.
     */
    fun toPlannedPayment(pattern: DiscoveredPattern, autoConfirm: Boolean = true): PlannedPayment? {
        val rule = pattern.recurrenceRule ?: return null
        
        return PlannedPayment(
            id = 0,
            name = pattern.name,
            categoryId = pattern.categoryId,
            amount = pattern.averageAmount,
            transactionType = pattern.transactionType,
            accountId = pattern.accountId,
            note = pattern.note,
            payee = pattern.payee,
            gpsLocation = pattern.gpsLocation,
            paymentMethod = pattern.paymentMethod,
            currency = pattern.currency,
            tags = pattern.tags,
            schedule = PaymentSchedule(
                recurrenceRule = rule,
                startDate = pattern.lastDate,
                nextDueDate = calculateNextDueDate(pattern.lastDate, rule)
            ),
            autoConfirm = autoConfirm,
            templateId = null
        )
    }

    private fun calculateNextDueDate(lastDate: Long, rule: RecurrenceRule): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = lastDate
        
        when (rule.type) {
            RecurrenceType.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, rule.interval)
            RecurrenceType.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, rule.interval)
            RecurrenceType.MONTHLY -> calendar.add(Calendar.MONTH, rule.interval)
            RecurrenceType.YEARLY -> calendar.add(Calendar.YEAR, rule.interval)
            RecurrenceType.EVERY_N_DAYS -> calendar.add(Calendar.DAY_OF_YEAR, rule.interval)
            RecurrenceType.EVERY_N_WEEKS -> calendar.add(Calendar.WEEK_OF_YEAR, rule.interval)
            RecurrenceType.EVERY_N_MONTHS -> calendar.add(Calendar.MONTH, rule.interval)
            else -> calendar.add(Calendar.MONTH, 1)
        }
        
        return calendar.timeInMillis
    }
}
