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

enum class PlannedPaymentStatus(
    val displayName: String,
    val iconName: String,
    val colorHex: String
) {
    ACTIVE("Active", "CheckCircle", "#4CAF50"),
    PAUSED("Paused", "PauseCircle", "#FFC107"),
    COMPLETED("Completed", "CheckCircle", "#2196F3"),
    CANCELLED("Cancelled", "Cancel", "#F44336");

    companion object {
        fun fromString(value: String): PlannedPaymentStatus {
            return entries.find { 
                it.name.equals(value, ignoreCase = true) || 
                it.displayName.equals(value, ignoreCase = true) 
            } ?: ACTIVE
        }
    }
}

enum class OccurrenceStatus { 
    PENDING, CONFIRMED, SKIPPED;

    companion object {
        fun fromString(value: String): OccurrenceStatus {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: PENDING
        }
    }
}

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
    val name: String,
    val categoryId: Long,
    val amount: BigDecimal,
    val entryMode: EntryMode,
    val accountId: String? = null,
    val targetAccountId: String? = null,
    val targetCurrency: String? = null,
    val transferRate: BigDecimal = BigDecimal.ONE,
    val note: String? = null,
    val payee: String? = null,
    val location: String? = null,
    val gpsLocation: String? = null,
    val warrantyExpiration: Long? = null,
    val transactionStatus: TransactionStatus = TransactionStatus.CLEARED,
    val paymentMethod: PaymentMethod? = null,
    val currency: String,
    val tags: Set<String> = emptySet(),
    val schedule: PaymentSchedule,
    val autoConfirm: Boolean = false,
    val status: PlannedPaymentStatus = PlannedPaymentStatus.ACTIVE,
    val profileId: Int = 1,
    val templateId: Long? = null
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
