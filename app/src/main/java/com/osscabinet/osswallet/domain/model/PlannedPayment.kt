package com.osscabinet.ossWallet.domain.model

import java.math.BigDecimal

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
 * Groups transaction-specific data to be used as a template for recurring instances.
 */
data class TransactionTemplate(
    val title: String,
    val amount: BigDecimal,
    val currency: String,
    val transactionType: String,       // "income", "expense", or "transfer"
    val accountId: String,
    val targetAccountId: String? = null,
    val targetCurrency: String? = null,
    val transferRate: BigDecimal = BigDecimal.ONE,
    val categoryId: Long,
    val paymentMethod: String = "Cash",
    val tags: List<String> = emptyList(),
    val note: String = "",
    val payee: String = ""
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
    val template: TransactionTemplate,
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
