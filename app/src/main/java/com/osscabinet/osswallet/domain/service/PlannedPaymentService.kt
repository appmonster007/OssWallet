package com.osscabinet.ossWallet.domain.service

import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
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
     * Returns all ACTIVE payments whose [nextDueDate] is before [now].
     */
    fun getOverdue(payments: List<PlannedPayment>, now: Long): List<PlannedPayment> =
        payments.filter { it.status == PlannedPaymentStatus.ACTIVE && it.nextDueDate < now }

    /**
     * Returns true if this payment should be marked COMPLETED after one trigger.
     */
    fun shouldComplete(payment: PlannedPayment): Boolean =
        payment.recurrenceRule.type == RecurrenceType.ONE_TIME
}
