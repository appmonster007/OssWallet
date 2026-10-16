package com.osscabinet.ossWallet.domain.service.discovery

import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.PaymentSchedule
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.RecurrenceRule
import com.osscabinet.ossWallet.domain.model.RecurrenceType
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.isTransfer
import com.osscabinet.ossWallet.domain.model.toEntryMode
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.max

/**
 * Result of the pattern discovery process.
 */
data class DiscoveredPattern(
    /** Stable identity (payee/category/type/currency), unchanged when the amount drifts. Dismissals hang off it. */
    val key: String,
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
    /** When the next one is expected, from the last occurrence and the detected rhythm. */
    val nextExpectedDate: Long = lastDate,
    /** The amount changes from one occurrence to the next (a utility bill), so [averageAmount] is an estimate. */
    val amountVaries: Boolean = false,
    val existsAsTemplate: Boolean = false,
    val existsAsPlannedPayment: Boolean = false
)

/**
 * Heuristic engine for discovering repeating transaction patterns: fuzzy payee matching, calendar-aware
 * recurrence (weekly through yearly), and a cut-off for things that have stopped. Pure and deterministic:
 * the clock and time zone are parameters.
 */
object PatternDiscoveryService {

    private const val MIN_OCCURRENCES = 3
    private const val DAY_IN_MILLIS = 24 * 60 * 60 * 1000L

    /** A share of gaps that must sit near the typical gap, or the history is just irregular spending. */
    private const val MIN_REGULARITY = 0.6

    /**
     * Scans transactions and returns the patterns that look like they repeat. Transfers and transactions
     * that already came from a planned payment are ignored: neither is something to schedule.
     */
    fun discoverPatterns(
        transactions: List<Transaction>,
        now: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ): List<DiscoveredPattern> {
        val candidates = transactions.filter { !it.transactionType.isTransfer && it.sourcePlannedPaymentId == null }
        if (candidates.size < 2) return emptyList()

        return candidates.groupBy { patternKey(it) }
            .mapNotNull { (key, group) ->
                if (group.size < 2) null else analyzeGroup(key, group.sortedBy { it.timestamp }, now, zone)
            }
            .sortedByDescending { it.confidence }
    }

    /** The identity used to group repeats: who was paid (or the note when there's no payee), what for, which way. */
    fun patternKey(tx: Transaction): String {
        val cleanedPayee = normalize(tx.payee)
        val who = if (cleanedPayee.isEmpty()) "note:${normalize(tx.note)}" else "payee:$cleanedPayee"
        return "$who|${tx.categoryId}|${tx.transactionType}|${tx.currency}"
    }

    /** Lower-cases and strips separators and long digit runs (card numbers) so "NETFLIX*1234" matches "Netflix". */
    fun normalize(text: String): String = text.trim()
        .lowercase()
        .replace(Regex("[*#]"), " ")
        .replace(Regex("\\d{4,}"), "")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun analyzeGroup(key: String, txs: List<Transaction>, now: Long, zone: ZoneId): DiscoveredPattern? {
        val lastTx = txs.last()

        val intervals = (1 until txs.size).map { (txs[it].timestamp - txs[it - 1].timestamp).toDouble() / DAY_IN_MILLIS }
        val median = median(intervals)
        val rhythm = detectRecurrence(txs, intervals, median, zone) ?: return null
        // Two data points only make a pattern when they are a year apart (an annual subscription).
        if (txs.size < MIN_OCCURRENCES && rhythm.rule.type != RecurrenceType.YEARLY) return null

        // Where it should have happened next; far past that, it has stopped and isn't worth suggesting.
        val nextExpected = calculateNextDueDate(lastTx.timestamp, rhythm.rule, zone)
        val graceDays = max(14.0, median * 0.5)
        val lateDays = (now - nextExpected).toDouble() / DAY_IN_MILLIS
        if (lateDays > graceDays) return null
        val recencyScore = (1.0 - max(0.0, lateDays) / (2 * graceDays)).coerceIn(0.5, 1.0).toFloat()

        val amounts = txs.map { it.amount }
        val mean = amounts.reduce { a, b -> a.add(b) }.divide(BigDecimal(txs.size), 2, RoundingMode.HALF_UP)
        val meanAbsDeviation = amounts.map { it.subtract(mean).abs() }.reduce { a, b -> a.add(b) }
            .divide(BigDecimal(txs.size), 10, RoundingMode.HALF_UP)
        val spread = if (mean.signum() == 0) 0.0 else meanAbsDeviation.divide(mean.abs(), 10, RoundingMode.HALF_UP).toDouble()
        val amountScore = if (mean.signum() == 0) 1f else (1.0 - spread).coerceIn(0.0, 1.0).toFloat()
        val varies = amounts.distinct().size > 1 && spread > 0.10

        // The usual amount if there is one; for a bill that moves, the recent average rather than a stale or odd figure.
        val frequencies = amounts.groupingBy { it }.eachCount()
        val mode = frequencies.maxByOrNull { it.value }
        val representative = when {
            mode != null && mode.value >= txs.size / 2.0 -> mode.key
            varies -> amounts.takeLast(3).reduce { a, b -> a.add(b) }
                .divide(BigDecimal(minOf(3, amounts.size)), 2, RoundingMode.HALF_UP)
            else -> lastTx.amount
        }

        val volumeScore = (txs.size.toFloat() / 8f).coerceIn(0.4f, 1.0f)
        val confidence = (amountScore * 0.15f) + (rhythm.confidence * 0.45f) + (recencyScore * 0.25f) + (volumeScore * 0.15f)
        if (confidence < 0.45f) return null

        return DiscoveredPattern(
            key = key,
            name = lastTx.payee.ifBlank { lastTx.note.ifBlank { "Discovered Pattern" } },
            payee = lastTx.payee,
            categoryId = lastTx.categoryId,
            accountId = lastTx.accountId,
            averageAmount = representative,
            currency = lastTx.currency,
            entryMode = lastTx.transactionType.toEntryMode(),
            paymentMethod = lastTx.paymentMethod,
            note = lastTx.note,
            tags = lastTx.tags,
            gpsLocation = lastTx.gpsLocation,
            warrantyExpiration = lastTx.warrantyExpiration,
            recurrenceRule = rhythm.rule,
            confidence = confidence,
            occurrences = txs.size,
            lastDate = lastTx.timestamp,
            representativeTransactionId = lastTx.id,
            nextExpectedDate = nextExpected,
            amountVaries = varies
        )
    }

    private class Rhythm(val rule: RecurrenceRule, val confidence: Float)

    /**
     * The rhythm that fits the typical gap between occurrences, or null when the history is too uneven
     * or fits none (a gap of 45 days is not a rhythm anyone schedules).
     */
    private fun detectRecurrence(txs: List<Transaction>, intervals: List<Double>, median: Double, zone: ZoneId): Rhythm? {
        val regularity = intervals.count { abs(it - median) <= max(3.0, median * 0.12) }.toDouble() / intervals.size
        if (regularity < MIN_REGULARITY) return null
        val regularityFactor = (0.5 + 0.5 * regularity).toFloat()

        val zoned = txs.map { Instant.ofEpochMilli(it.timestamp).atZone(zone) }
        val dayOfMonthCounts = zoned.groupingBy { it.dayOfMonth }.eachCount()
        val dominantDay = dayOfMonthCounts.maxByOrNull { it.value }
        // One miss is allowed: a bill that lands on the 1st, the 1st, the 2nd is still "the 1st".
        val sameDayOfMonth = dominantDay != null && dominantDay.value >= txs.size - 1

        fun near(target: Double, tolerance: Double) = abs(median - target) <= tolerance
        fun scored(rule: RecurrenceRule, base: Float) = Rhythm(rule, (base * regularityFactor).coerceAtMost(1f))

        return when {
            near(1.0, 0.6) -> scored(RecurrenceRule(RecurrenceType.DAILY), 0.9f)
            near(7.0, 1.5) -> {
                val weekdays = zoned.groupingBy { it.dayOfWeek }.eachCount()
                val weekdayShare = (weekdays.values.max().toFloat() / txs.size)
                // Calendar numbering (Sunday = 1), which is what the recurrence rule uses.
                val calendarDay = (zoned.last().dayOfWeek.value % 7) + 1
                scored(RecurrenceRule(RecurrenceType.WEEKLY, dayOfWeek = calendarDay), 0.7f + weekdayShare * 0.3f)
            }
            near(14.0, 2.0) -> scored(RecurrenceRule(RecurrenceType.EVERY_N_WEEKS, interval = 2), 0.8f)
            near(30.4, 5.0) ->
                if (sameDayOfMonth) scored(RecurrenceRule(RecurrenceType.MONTHLY, dayOfMonth = dominantDay!!.key), 1.0f)
                else scored(RecurrenceRule(RecurrenceType.MONTHLY), 0.7f)
            near(60.8, 6.0) -> scored(RecurrenceRule(RecurrenceType.EVERY_N_MONTHS, interval = 2), if (sameDayOfMonth) 0.8f else 0.7f)
            near(91.3, 8.0) -> scored(RecurrenceRule(RecurrenceType.EVERY_N_MONTHS, interval = 3), if (sameDayOfMonth) 0.9f else 0.75f)
            near(182.6, 10.0) -> scored(RecurrenceRule(RecurrenceType.EVERY_N_MONTHS, interval = 6), if (sameDayOfMonth) 0.85f else 0.75f)
            near(365.25, 12.0) -> scored(RecurrenceRule(RecurrenceType.YEARLY), 0.8f)
            else -> null
        }
    }

    private fun median(values: List<Double>): Double {
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2
    }

    /**
     * Logic for calculating the next actual date, handling month-end edge cases.
     */
    fun calculateNextDueDate(lastDate: Long, rule: RecurrenceRule, zone: ZoneId = ZoneId.systemDefault()): Long {
        val calendar = Calendar.getInstance(java.util.TimeZone.getTimeZone(zone))
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
                nextDueDate = pattern.nextExpectedDate
            ),
            autoConfirm = autoConfirm,
            templateId = null
        )
    }
}
