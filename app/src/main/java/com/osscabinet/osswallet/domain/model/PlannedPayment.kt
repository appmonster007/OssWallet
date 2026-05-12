package com.osscabinet.ossWallet.domain.model

enum class RecurrenceType {
    ONE_TIME,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
    EVERY_N_DAYS,
    EVERY_N_WEEKS,
    EVERY_N_MONTHS,
    CRON
}

enum class PlannedPaymentStatus { ACTIVE, PAUSED, COMPLETED, CANCELLED }

enum class OccurrenceStatus { PENDING, CONFIRMED, SKIPPED }

data class RecurrenceRule(
    val type: RecurrenceType,
    val interval: Int = 1,
    val dayOfWeek: Int? = null,   // 1–7 (Mon–Sun) for WEEKLY
    val dayOfMonth: Int? = null,  // 1–31 for MONTHLY; clamped to month length
    val cronExpression: String? = null
)

/**
 * Groups scheduling logic and state.
 */
data class PaymentSchedule(
    val recurrenceRule: RecurrenceRule,
    val startDate: Long,
    val endDate: Long? = null,
    val nextDueDate: Long,
    val lastTriggeredDate: Long? = null
)

data class PlannedPayment(
    val id: Long = 0,
    val templateId: Long,
    val template: Template, // Fully populated when loaded from repo
    val schedule: PaymentSchedule,
    val autoConfirm: Boolean = false,
    val status: PlannedPaymentStatus = PlannedPaymentStatus.ACTIVE,
    val profileId: Int = 1
)

data class PlannedPaymentOccurrence(
    val id: Long = 0,
    val plannedPaymentId: Long,
    val scheduledDate: Long,
    val generatedTransactionId: Long? = null,
    val status: OccurrenceStatus = OccurrenceStatus.PENDING
)

data class UpcomingPaymentItem(
    val plannedPayment: PlannedPayment,
    val scheduledDate: Long,
    val occurrenceId: Long? = null,
    val isOverdue: Boolean
)
