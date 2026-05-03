package com.osscabinet.ossWallet.domain.service

import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
import com.osscabinet.ossWallet.domain.model.RecurrenceRule
import com.osscabinet.ossWallet.domain.model.RecurrenceType
import com.osscabinet.ossWallet.domain.model.UpcomingPaymentItem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

object PlannedPaymentService {

    /**
     * Calculates the next due date after [fromDate] according to the payment's recurrence rule.
     * Returns null when the payment has no further occurrences (ONE_TIME, or past endDate).
     */
    fun calculateNextDueDate(payment: PlannedPayment, fromDate: Long): Long? {
        val rule = payment.recurrenceRule
        if (rule.type == RecurrenceType.ONE_TIME) return null

        val from = Instant.ofEpochMilli(fromDate).atZone(ZoneOffset.UTC).toLocalDate()
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
            RecurrenceType.ONE_TIME -> return null
        }

        val nextMillis = next.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        return if (payment.endDate != null && nextMillis > payment.endDate) null else nextMillis
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
        val result = mutableListOf<UpcomingPaymentItem>()

        for (payment in payments) {
            if (payment.status != PlannedPaymentStatus.ACTIVE) continue

            var due = payment.nextDueDate
            while (due <= windowEnd) {
                if (due >= payment.startDate) {
                    result += UpcomingPaymentItem(
                        plannedPayment = payment,
                        scheduledDate = due,
                        isOverdue = due < fromDate
                    )
                }
                val next = calculateNextDueDate(payment.copy(nextDueDate = due), due) ?: break
                if (next == due) break  // guard against infinite loop
                due = next
            }
        }

        return result.sortedBy { it.scheduledDate }
    }

    /**
     * Generates up to [n] future occurrences starting from [payment.nextDueDate].
     * Respects endDate; stops early for ONE_TIME payments.
     */
    fun generateNextN(payment: PlannedPayment, now: Long, n: Int): List<UpcomingPaymentItem> {
        if (payment.status != PlannedPaymentStatus.ACTIVE) return emptyList()
        val result = mutableListOf<UpcomingPaymentItem>()
        var due = payment.nextDueDate
        while (result.size < n) {
            result += UpcomingPaymentItem(
                plannedPayment = payment,
                scheduledDate = due,
                isOverdue = due < now
            )
            val next = calculateNextDueDate(payment.copy(nextDueDate = due), due) ?: break
            if (next == due) break
            due = next
        }
        return result
    }

    /**
     * Returns all ACTIVE payments whose [nextDueDate] is before [now].
     */
    fun getOverdue(payments: List<PlannedPayment>, now: Long): List<PlannedPayment> =
        payments.filter { it.status == PlannedPaymentStatus.ACTIVE && it.nextDueDate < now }

    /**
     * Returns true if this payment should be marked COMPLETED after one trigger.
     */
    fun shouldComplete(payment: PlannedPayment): Boolean =
        payment.recurrenceRule.type == RecurrenceType.ONE_TIME

    /**
     * Returns a human-readable description of a recurrence rule.
     * E.g. "Every month on day 15", "Every 2 weeks on Mon", "Daily", "One time".
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
        }
    }
}
