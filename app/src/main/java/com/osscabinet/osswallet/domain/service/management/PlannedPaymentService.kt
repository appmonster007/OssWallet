package com.osscabinet.ossWallet.domain.service.management

import com.cronutils.descriptor.CronDescriptor
import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.model.time.ExecutionTime
import com.cronutils.parser.CronParser
import com.osscabinet.ossWallet.domain.model.*
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
import java.util.Optional

object PlannedPaymentService {

    private val cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX)
    private val cronParser = CronParser(cronDefinition)
    private val descriptor = CronDescriptor.instance(Locale.getDefault())

    /**
     * Calculates the next due date after [fromDate] according to the payment's recurrence rule.
     * Returns null when the payment has no further occurrences (ONE_TIME, or past endDate).
     */
    fun calculateNextDueDate(payment: PlannedPayment, fromDate: Long): Long? =
        calculateNextDueDate(payment.schedule, fromDate)

    private fun calculateNextDueDate(schedule: PaymentSchedule, fromDate: Long): Long? {
        val rule = schedule.recurrenceRule
        if (rule.type == RecurrenceType.ONE_TIME) return null

        val fromInstant = Instant.ofEpochMilli(fromDate)
        val fromZoned = fromInstant.atZone(ZoneOffset.UTC)

        val cronExpression = rule.cronExpression
        val nextMillis = if (rule.type == RecurrenceType.CRON && cronExpression != null) {
            calculateNextFromCron(cronExpression, fromDate)
        } else {
            val from = fromZoned.toLocalDate()
            val next = when (rule.type) {
                RecurrenceType.DAILY -> from.plusDays(rule.interval.toLong())
                RecurrenceType.EVERY_N_DAYS -> from.plusDays(rule.interval.toLong())
                RecurrenceType.WEEKLY, RecurrenceType.EVERY_N_WEEKS -> from.plusWeeks(rule.interval.toLong())
                RecurrenceType.MONTHLY, RecurrenceType.EVERY_N_MONTHS -> {
                    val candidate = from.plusMonths(rule.interval.toLong())
                    rule.dayOfMonth?.let { day ->
                        val clamped = day.coerceAtMost(candidate.lengthOfMonth())
                        candidate.withDayOfMonth(clamped)
                    } ?: candidate
                }
                RecurrenceType.YEARLY -> from.plusYears(rule.interval.toLong())
                else -> null
            }
            next?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        }

        val endDate = schedule.endDate
        return if (nextMillis != null && endDate != null && nextMillis > endDate) null else nextMillis
    }

    private fun calculateNextFromCron(cronExpression: String, fromDate: Long): Long? {
        return try {
            val fromZoned = Instant.ofEpochMilli(fromDate).atZone(ZoneOffset.UTC)
            val cron = cronParser.parse(cronExpression)
            val executionTime = ExecutionTime.forCron(cron)
            val nextExecution: Optional<java.time.ZonedDateTime> = executionTime.nextExecution(fromZoned)
            if (nextExecution.isPresent) {
                nextExecution.get().toInstant().toEpochMilli()
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Core projection engine — works directly from a [PaymentSchedule] with no status check.
     * Status gate lives in the [PlannedPayment] overload below.
     */
    private fun projectDates(
        schedule: PaymentSchedule,
        limitDate: Long? = null,
        limitCount: Int? = null,
        minDate: Long? = null
    ): List<Long> {
        val result = mutableListOf<Long>()
        val scheduleEndDate = schedule.endDate
        var due: Long? = schedule.nextDueDate
        while (due != null) {
            if (limitDate != null && due > limitDate) break
            if (scheduleEndDate != null && due > scheduleEndDate) break
            if (minDate == null || due >= minDate) result.add(due)
            if (limitCount != null && result.size >= limitCount) break
            if (schedule.recurrenceRule.type == RecurrenceType.ONE_TIME) break
            val next = calculateNextDueDate(schedule, due)
            if (next == null || next <= due) break
            due = next
        }
        return result
    }

    private fun projectDates(
        payment: PlannedPayment,
        limitDate: Long? = null,
        limitCount: Int? = null,
        minDate: Long? = null
    ): List<Long> {
        if (payment.status != PlannedPaymentStatus.ACTIVE) return emptyList()
        return projectDates(payment.schedule, limitDate, limitCount, minDate)
    }

    /**
     * Generates upcoming payment occurrences in the window [fromDate, fromDate + daysAhead].
     * Sorted by scheduledDate ascending.
     */
    fun generateUpcoming(
        payments: List<PlannedPayment>,
        fromDate: Long,
        daysAhead: Int
    ): List<UpcomingPaymentItem> {
        val windowEnd = fromDate + daysAhead.toLong() * 24 * 60 * 60 * 1000
        return payments.flatMap { payment ->
            projectDates(payment, limitDate = windowEnd, minDate = payment.schedule.startDate).map { date ->
                UpcomingPaymentItem(
                    plannedPayment = payment,
                    scheduledDate = date,
                    isOverdue = date < fromDate
                )
            }
        }.sortedBy { it.scheduledDate }
    }

    /**
     * Generates upcoming payment occurrences in the window [fromDate, toDate].
     * toDate = null means no upper bound; capped at 365 days from fromDate.
     * Sorted by scheduledDate ascending.
     */
    fun generateUpcoming(
        payments: List<PlannedPayment>,
        fromDate: Long,
        toDate: Long?
    ): List<UpcomingPaymentItem> {
        val windowEnd = toDate ?: (fromDate + 365L * 24 * 60 * 60 * 1000)
        return payments.flatMap { payment ->
            projectDates(payment, limitDate = windowEnd, minDate = payment.schedule.startDate).map { date ->
                UpcomingPaymentItem(
                    plannedPayment = payment,
                    scheduledDate = date,
                    isOverdue = date < fromDate
                )
            }
        }.sortedBy { it.scheduledDate }
    }

    /**
     * Generates upcoming payment occurrences in the window [now, windowEnd].
     * Respects endDate; stops early for ONE_TIME payments.
     */
    fun generateUntil(payment: PlannedPayment, now: Long, windowEnd: Long): List<UpcomingPaymentItem> {
        return projectDates(payment, limitDate = windowEnd).map { date ->
            UpcomingPaymentItem(
                plannedPayment = payment,
                scheduledDate = date,
                isOverdue = date < now
            )
        }
    }

    /**
     * Generates up to [n] future occurrences starting from [payment.schedule.nextDueDate].
     * Respects endDate; stops early for ONE_TIME payments.
     */
    fun generateNextN(payment: PlannedPayment, now: Long, n: Int): List<UpcomingPaymentItem> {
        return projectDates(payment, limitCount = n).map { date ->
            UpcomingPaymentItem(
                plannedPayment = payment,
                scheduledDate = date,
                isOverdue = date < now
            )
        }
    }

    /**
     * Generates up to [n] future occurrences starting from [startDate] according to [rule].
     * Stops at [endDate] if provided.
     */
    fun generateOccurrences(
        startDate: Long,
        rule: RecurrenceRule,
        n: Int,
        endDate: Long? = null
    ): List<Long> = projectDates(PaymentSchedule(rule, startDate, endDate, startDate), limitCount = n)

    /**
     * Generates all ACTIVE payments whose [nextDueDate] is before [now].
     */
    fun getOverdue(payments: List<PlannedPayment>, now: Long): List<PlannedPayment> =
        payments.filter { it.status == PlannedPaymentStatus.ACTIVE && it.schedule.nextDueDate < now }

    /**
     * Returns true if this payment should be marked COMPLETED after one trigger.
     */
    fun shouldComplete(payment: PlannedPayment): Boolean =
        payment.schedule.recurrenceRule.type == RecurrenceType.ONE_TIME

    /**
     * Returns a human-readable description of a recurrence rule.
     */
    fun formatRecurrenceHuman(rule: RecurrenceRule): String {
        val dayOfWeekName: (Int?) -> String? = { dow ->
            when (dow) {
                1 -> "Monday"; 2 -> "Tuesday"; 3 -> "Wednesday"; 4 -> "Thursday"
                5 -> "Friday"; 6 -> "Saturday"; 7 -> "Sunday"
                else -> null
            }
        }
        return when (rule.type) {
            RecurrenceType.ONE_TIME -> "One time"
            RecurrenceType.DAILY -> "Daily"
            RecurrenceType.WEEKLY -> {
                val day = dayOfWeekName(rule.dayOfWeek)
                if (day != null) "Every week on $day" else "Weekly"
            }
            RecurrenceType.MONTHLY -> {
                if (rule.dayOfMonth != null) "Every month on day ${rule.dayOfMonth}" else "Monthly"
            }
            RecurrenceType.YEARLY -> "Yearly"
            RecurrenceType.EVERY_N_DAYS -> {
                val n = rule.interval
                if (n == 1) "Daily" else "Every $n days"
            }
            RecurrenceType.EVERY_N_WEEKS -> {
                val n = rule.interval
                val day = dayOfWeekName(rule.dayOfWeek)
                val base = if (n == 1) "Weekly" else "Every $n weeks"
                if (day != null) "$base on $day" else base
            }
            RecurrenceType.EVERY_N_MONTHS -> {
                val n = rule.interval
                val base = if (n == 1) "Monthly" else "Every $n months"
                if (rule.dayOfMonth != null) "$base on day ${rule.dayOfMonth}" else base
            }
            RecurrenceType.CRON -> {
                if (rule.cronExpression.isNullOrBlank()) "Custom: Invalid expression"
                else {
                    try {
                        val cron = cronParser.parse(rule.cronExpression)
                        "Custom: ${descriptor.describe(cron)}"
                    } catch (_: Exception) {
                        "Custom: ${rule.cronExpression}"
                    }
                }
            }
        }
    }

    fun generateOccurrencesPreview(
        startDate: Long,
        rule: RecurrenceRule,
        n: Int = 5
    ): List<String> {
        val occurrences = generateOccurrences(startDate, rule, n)
        val sdf = java.text.SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
        return occurrences.map { sdf.format(java.util.Date(it)) }
    }

    fun buildScheduleSummary(
        startDateMillis: Long,
        rule: RecurrenceRule
    ): String {
        val recurrenceLabel = formatRecurrenceHuman(rule)
        val startLabel = java.text.SimpleDateFormat("MMM d", Locale.getDefault()).format(java.util.Date(startDateMillis))
        return "$recurrenceLabel · Starts $startLabel"
    }

    fun parseCronToRule(cron: String): RecurrenceRule? {
        if (cron.isBlank()) return null
        return try {
            val cronParsed = cronParser.parse(cron)
            cronParsed.validate()
            RecurrenceRule(RecurrenceType.CRON, cronExpression = cron)
        } catch (_: Exception) {
            null
        }
    }

    fun ruleToCron(rule: RecurrenceRule): String? {
        if (rule.type == RecurrenceType.ONE_TIME) return null
        if (rule.type == RecurrenceType.CRON) return rule.cronExpression
        
        // Simple heuristic for UNIX cron: "min hour dom mon dow"
        // Note: This is a simplified mapping. For production, consider a more robust converter.
        return when (rule.type) {
            RecurrenceType.DAILY -> "0 0 * * *"
            RecurrenceType.EVERY_N_DAYS -> "0 0 */${rule.interval} * *"
            RecurrenceType.WEEKLY -> "0 0 * * ${rule.dayOfWeek ?: "*"}"
            RecurrenceType.EVERY_N_WEEKS -> "0 0 * * ${rule.dayOfWeek ?: "*"}" // UNIX cron doesn't support every N weeks easily
            RecurrenceType.MONTHLY -> "0 0 ${rule.dayOfMonth ?: 1} * *"
            RecurrenceType.EVERY_N_MONTHS -> "0 0 ${rule.dayOfMonth ?: 1} */${rule.interval} *"
            RecurrenceType.YEARLY -> "0 0 1 1 *"
            else -> null
        }
    }
}
