package com.osscabinet.ossWallet.domain.service.discovery

import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.toEntryMode

import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.domain.model.toEntryMode

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar

/**
 * Result of the pattern discovery process.
 */
data class DiscoveredPattern(
    val name: String,
    val payee: String,
    val categoryId: Long,
    val accountId: String?,
    val averageAmount: BigDecimal,
    val currency: String,
    val entryMode: EntryMode,
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
 * Advanced heuristic engine for discovering repeating transaction patterns.
 * Features: Fuzzy matching, Calendar-aware recurrence, and Time-decay weighting.
 */
object PatternDiscoveryService {

    private const val MIN_OCCURRENCES = 3
    private const val DAY_IN_MILLIS = 24 * 60 * 60 * 1000L
    private const val INTERVAL_TOLERANCE_DAYS = 3
    private const val RECENCY_DECAY_MONTHS = 3L

    /**
     * Scans transactions and returns a list of discovered patterns.
     */
    fun discoverPatterns(transactions: List<Transaction>): List<DiscoveredPattern> {
        if (transactions.size < MIN_OCCURRENCES) return emptyList()

        // 1. Group by cleaned features
        val groups = transactions.groupBy { tx ->
            val cleanedPayee = normalizePayee(tx.payee)
            val payeeKey = if (cleanedPayee.isEmpty()) "note:${normalizePayee(tx.note)}" else "payee:$cleanedPayee"
            "$payeeKey|${tx.categoryId}|${tx.transactionType}|${tx.currency}"
        }

        return groups.mapNotNull { (_, groupTransactions) ->
            if (groupTransactions.size < MIN_OCCURRENCES) return@mapNotNull null
            
            val sortedTxs = groupTransactions.sortedBy { it.timestamp }
            analyzeGroup(sortedTxs)
        }.sortedByDescending { it.confidence }
    }

    private fun normalizePayee(payee: String): String {
        return payee.trim()
            .lowercase()
            .replace(Regex("[*#]"), " ")           // Remove * and # separators
            .replace(Regex("\\d{4,}"), "")        // Remove long number sequences (card numbers)
            .replace(Regex("\\s+"), " ")          // Collapse whitespace
            .trim()
    }

    private fun analyzeGroup(txs: List<Transaction>): DiscoveredPattern? {
        val lastTx = txs.last()

        // 1. Calculate Representative Amount
        // Strategy: Use the most frequent amount (Mode) if it appears in >= 50% of txs.
        // Otherwise, use the most recent amount (Trend-aware).
        val amountFrequencies = txs.groupingBy { it.amount }.eachCount()
        val mostFrequent = amountFrequencies.maxByOrNull { it.value }
        val representativeAmount = if (mostFrequent != null && mostFrequent.value >= txs.size / 2.0) {
            mostFrequent.key
        } else {
            lastTx.amount
        }

        // 2. Consistency Score (Calculated against mathematical average)
        val mathematicalAvg = txs.map { it.amount }.reduce { acc, amt -> acc.add(amt) }
            .divide(BigDecimal(txs.size), 2, RoundingMode.HALF_UP)

        val amountVariance = txs.map { it.amount.subtract(mathematicalAvg).abs() }
            .reduce { acc, v -> acc.add(v) }
            .divide(BigDecimal(txs.size), 10, RoundingMode.HALF_UP)

        val amountScore = if (mathematicalAvg.compareTo(BigDecimal.ZERO) == 0) 1f
        else (1f - (amountVariance.divide(mathematicalAvg.abs(), 10, RoundingMode.HALF_UP).toFloat())).coerceIn(0f, 1f)

        // 3. Periodicity & Calendar Awareness Score
        val recurrenceResult = detectRecurrence(txs)
        val recurrenceScore = recurrenceResult.confidence
        
        // 3. Recency Score (Time Decay)
        val currentTime = System.currentTimeMillis()
        val daysSinceLast = (currentTime - lastTx.timestamp) / DAY_IN_MILLIS
        val decayPeriodDays = RECENCY_DECAY_MONTHS * 30
        
        // Penalty if the pattern seems to have stopped
        val recencyScore = if (daysSinceLast < decayPeriodDays) 1f 
            else (1f - ((daysSinceLast - decayPeriodDays).toFloat() / decayPeriodDays)).coerceIn(0f, 1f)

        // 4. Volume Score
        val volumeScore = (txs.size.toFloat() / 8f).coerceIn(0.4f, 1.0f)

        // Weighted Confidence
        val totalConfidence = (amountScore * 0.15f) + 
                            (recurrenceScore * 0.45f) + 
                            (recencyScore * 0.25f) + 
                            (volumeScore * 0.15f)

        if (totalConfidence < 0.45f) return null

        return DiscoveredPattern(
            name = lastTx.payee.ifBlank { lastTx.note.ifBlank { "Discovered Pattern" } },
            payee = lastTx.payee,
            categoryId = lastTx.categoryId,
            accountId = lastTx.accountId,
            averageAmount = representativeAmount,
            currency = lastTx.currency,
            entryMode = lastTx.transactionType.toEntryMode(),
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
        val calendar = Calendar.getInstance()
        val daysOfMonth = txs.map { 
            calendar.timeInMillis = it.timestamp
            calendar.get(Calendar.DAY_OF_MONTH)
        }
        val daysOfWeek = txs.map {
            calendar.timeInMillis = it.timestamp
            calendar.get(Calendar.DAY_OF_WEEK)
        }

        // Check for "Same Day of Month" (Monthly Bill)
        val domFrequency = daysOfMonth.groupingBy { it }.eachCount()
        val maxDomFreq = domFrequency.values.maxOrNull() ?: 0
        if (maxDomFreq >= txs.size - 1) { // 1 error allowed
            val dominantDom = domFrequency.maxBy { it.value }.key
            return RecurrenceDiscoveryResult(
                rule = RecurrenceRule(RecurrenceType.MONTHLY, dayOfMonth = dominantDom),
                confidence = 1.0f
            )
        }

        // Check for "Same Day of Week" (Weekly Activity)
        val dowFrequency = daysOfWeek.groupingBy { it }.eachCount()
        val maxDowFreq = dowFrequency.values.maxOrNull() ?: 0
        
        // Calculate intervals for general check
        val intervals = mutableListOf<Long>()
        for (i in 0 until txs.size - 1) {
            intervals.add(txs[i + 1].timestamp - txs[i].timestamp)
        }
        val avgIntervalDays = if (intervals.isEmpty()) 0.0 else intervals.average() / DAY_IN_MILLIS

        return when {
            // Weekly check (with DOW consistency)
            isCloseTo(avgIntervalDays, 7.0) -> {
                val dowScore = maxDowFreq.toFloat() / txs.size
                RecurrenceDiscoveryResult(
                    rule = RecurrenceRule(RecurrenceType.WEEKLY, dayOfWeek = daysOfWeek.last()),
                    confidence = (0.7f + (dowScore * 0.3f))
                )
            }
            // Bi-weekly
            isCloseTo(avgIntervalDays, 14.0) -> {
                RecurrenceDiscoveryResult(
                    rule = RecurrenceRule(RecurrenceType.EVERY_N_WEEKS, interval = 2),
                    confidence = 0.8f
                )
            }
            // Average Monthly (if DOM consistency failed but interval is ~30 days)
            isCloseTo(avgIntervalDays, 30.0) -> {
                RecurrenceDiscoveryResult(
                    rule = RecurrenceRule(RecurrenceType.MONTHLY),
                    confidence = 0.7f
                )
            }
            // Daily
            isCloseTo(avgIntervalDays, 1.0) -> {
                RecurrenceDiscoveryResult(
                    rule = RecurrenceRule(RecurrenceType.DAILY),
                    confidence = 0.9f
                )
            }
            else -> RecurrenceDiscoveryResult(null, 0.2f)
        }
    }

    private fun isCloseTo(value: Double, target: Double): Boolean {
        return Math.abs(value - target) <= INTERVAL_TOLERANCE_DAYS
    }

    /**
     * Logic for calculating the next actual date, handling month-end edge cases.
     */
    fun calculateNextDueDate(lastDate: Long, rule: RecurrenceRule): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = lastDate
        
        when (rule.type) {
            RecurrenceType.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, rule.interval)
            RecurrenceType.WEEKLY -> {
                calendar.add(Calendar.WEEK_OF_YEAR, rule.interval)
                rule.dayOfWeek?.let { calendar.set(Calendar.DAY_OF_WEEK, it) }
            }
            RecurrenceType.MONTHLY -> {
                calendar.add(Calendar.MONTH, rule.interval)
                rule.dayOfMonth?.let { dom ->
                    val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                    calendar.set(Calendar.DAY_OF_MONTH, Math.min(dom, maxDay))
                }
            }
            RecurrenceType.YEARLY -> calendar.add(Calendar.YEAR, rule.interval)
            RecurrenceType.EVERY_N_DAYS -> calendar.add(Calendar.DAY_OF_YEAR, rule.interval)
            RecurrenceType.EVERY_N_WEEKS -> calendar.add(Calendar.WEEK_OF_YEAR, rule.interval)
            RecurrenceType.EVERY_N_MONTHS -> calendar.add(Calendar.MONTH, rule.interval)
            else -> calendar.add(Calendar.MONTH, 1)
        }
        
        return calendar.timeInMillis
    }

    fun toTemplate(pattern: DiscoveredPattern): Template {
        return Template(
            id = 0,
            name = pattern.name,
            categoryId = pattern.categoryId,
            amount = pattern.averageAmount,
            entryMode = pattern.entryMode,
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

    fun toPlannedPayment(pattern: DiscoveredPattern, autoConfirm: Boolean = true): PlannedPayment? {
        val rule = pattern.recurrenceRule ?: return null
        
        return PlannedPayment(
            id = 0,
            name = pattern.name,
            categoryId = pattern.categoryId,
            amount = pattern.averageAmount,
            entryMode = pattern.entryMode,
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
}
