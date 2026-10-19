package com.osscabinet.ossWallet.domain.model

import com.osscabinet.ossWallet.domain.model.util.BigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
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

@Serializable
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

@Serializable
enum class OccurrenceStatus {
    PENDING, CONFIRMED, SKIPPED;

    companion object {
        fun fromString(value: String): OccurrenceStatus {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: PENDING
        }
    }
}

@Serializable
data class RecurrenceRule(
    val type: RecurrenceType,
    val interval: Int = 1,
    val dayOfWeek: Int? = null,   // 1–7 (Mon–Sun) for WEEKLY
    val dayOfMonth: Int? = null,  // 1–31 for MONTHLY; clamped to month length
    val cronExpression: String? = null
) {
    fun formatHuman(): String {
        val dayOfWeekName: (Int?) -> String? = { dow ->
            when (dow) {
                1 -> "Monday"; 2 -> "Tuesday"; 3 -> "Wednesday"; 4 -> "Thursday"
                5 -> "Friday"; 6 -> "Saturday"; 7 -> "Sunday"
                else -> null
            }
        }
        return when (type) {
            RecurrenceType.ONE_TIME -> "One time"
            RecurrenceType.DAILY -> "Daily"
            RecurrenceType.WEEKLY -> {
                val day = dayOfWeekName(dayOfWeek)
                if (day != null) "Every week on $day" else "Weekly"
            }
            RecurrenceType.MONTHLY -> {
                if (dayOfMonth != null) "Every month on day $dayOfMonth" else "Monthly"
            }
            RecurrenceType.YEARLY -> "Yearly"
            RecurrenceType.EVERY_N_DAYS -> {
                if (interval == 1) "Daily" else "Every $interval days"
            }
            RecurrenceType.EVERY_N_WEEKS -> {
                val day = dayOfWeekName(dayOfWeek)
                val base = if (interval == 1) "Weekly" else if (interval == 2) "Bi-weekly" else "Every $interval weeks"
                if (day != null) "$base on $day" else base
            }
            RecurrenceType.EVERY_N_MONTHS -> {
                val base = if (interval == 1) "Monthly" else if (interval == 3) "Quarterly" else if (interval == 6) "Semi-annually" else "Every $interval months"
                if (dayOfMonth != null) "$base on day $dayOfMonth" else base
            }
            RecurrenceType.CRON -> {
                if (cronExpression.isNullOrBlank()) "Custom Cron"
                else "Cron ($cronExpression)"
            }
        }
    }
}

/**
 * Groups scheduling logic and state.
 */
@Serializable
data class PaymentSchedule(
    val recurrenceRule: RecurrenceRule,
    val startDate: Long,
    val endDate: Long? = null,
    val nextDueDate: Long,
    val lastTriggeredDate: Long? = null
)

@Serializable
data class PlannedPayment(
    val id: Long = 0,
    val name: String,
    val categoryId: Long,
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
    val entryMode: EntryMode,
    val accountId: String? = null,
    val targetAccountId: String? = null,
    val targetCurrency: String? = null,
    @Serializable(with = BigDecimalSerializer::class)
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

@Serializable
data class PlannedPaymentSummary(
    val id: Long,
    val name: String,
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
    val currency: String,
    val entryMode: EntryMode,
    val scheduledDate: Long,
    val isOverdue: Boolean,
    val categoryName: String = "",
    val categoryIcon: String? = null,
    val categoryColor: Int = -1,
    val accountName: String = "",
    val note: String? = null
)
