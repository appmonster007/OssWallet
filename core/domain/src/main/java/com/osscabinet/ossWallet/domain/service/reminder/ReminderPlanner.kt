package com.osscabinet.ossWallet.domain.service.reminder

import com.osscabinet.ossWallet.domain.model.BudgetAlertLevel
import com.osscabinet.ossWallet.domain.model.Loan
import com.osscabinet.ossWallet.domain.model.LoanStatus
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
import com.osscabinet.ossWallet.domain.model.settings.ReminderConfig
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class ReminderKind { PLANNED_PAYMENT, LOAN, BUDGET_WARNING, BUDGET_EXCEEDED }

data class ReminderEvent(
    /** Stable identity: the same real-world event always yields the same key. */
    val key: String,
    val kind: ReminderKind,
    val title: String,
    val body: String,
    val targetId: Long,
    /** The due date this reminder is for (planned payments), needed to confirm that occurrence. */
    val dueDate: Long? = null
) {
    /** Flat form for intents and work data, so a reminder can be re-posted later (snooze). */
    fun toMap(): Map<String, String> = buildMap {
        put("key", key); put("kind", kind.name); put("title", title); put("body", body)
        put("targetId", targetId.toString())
        dueDate?.let { put("dueDate", it.toString()) }
    }

    companion object {
        fun fromMap(map: Map<String, String?>): ReminderEvent? {
            val key = map["key"] ?: return null
            val kind = runCatching { ReminderKind.valueOf(map["kind"] ?: return null) }.getOrNull() ?: return null
            return ReminderEvent(
                key = key, kind = kind, title = map["title"].orEmpty(), body = map["body"].orEmpty(),
                targetId = map["targetId"]?.toLongOrNull() ?: return null,
                dueDate = map["dueDate"]?.toLongOrNull()
            )
        }
    }
}

/** A budget's current alert state, resolved by the caller (spend math lives with budgets). */
data class BudgetAlertInput(
    val budgetId: Long,
    val name: String,
    val percentUsed: Int,
    val level: BudgetAlertLevel,
    /** Start of the budget's current period; makes the alert fire once per period. */
    val periodStart: Long
)

/** Decides which reminders are due. Pure: no clock, storage or Android access. */
object ReminderPlanner {

    fun plan(
        now: Long,
        config: ReminderConfig,
        plannedPayments: List<PlannedPayment>,
        loans: List<Loan>,
        budgetAlerts: List<BudgetAlertInput>,
        sentKeys: Set<String>,
        zone: ZoneId = ZoneId.systemDefault()
    ): List<ReminderEvent> {
        if (!config.enabled) return emptyList()
        val nowZoned = Instant.ofEpochMilli(now).atZone(zone)
        if (nowZoned.hour < config.hourOfDay) return emptyList()
        val today = nowZoned.toLocalDate()

        val events = buildList {
            if (config.plannedPayments) {
                plannedPayments.filter { it.status == PlannedPaymentStatus.ACTIVE }.forEach { p ->
                    val due = p.schedule.nextDueDate
                    val days = daysUntil(today, due, zone)
                    if (days in 0..config.leadDays) {
                        add(
                            ReminderEvent(
                                key = "pp:${p.id}:$due",
                                kind = ReminderKind.PLANNED_PAYMENT,
                                title = "${p.name} ${whenText(days)}",
                                body = "${p.amount.abs().stripTrailingZeros().toPlainString()} ${p.currency}",
                                targetId = p.id,
                                dueDate = due
                            )
                        )
                    }
                }
            }
            if (config.loans) {
                loans.filter { it.status == LoanStatus.ACTIVE }.forEach { l ->
                    val due = l.expectedSettlementDate ?: return@forEach
                    val days = daysUntil(today, due, zone)
                    if (days in 0..config.leadDays) {
                        add(
                            ReminderEvent(
                                key = "loan:${l.id}:$due",
                                kind = ReminderKind.LOAN,
                                title = "Loan \"${l.name}\" settles ${whenText(days)}",
                                body = l.lenderOrBorrowerName,
                                targetId = l.id
                            )
                        )
                    }
                }
            }
            if (config.budgetAlerts) {
                budgetAlerts.forEach { b ->
                    when (b.level) {
                        BudgetAlertLevel.EXCEEDED -> add(
                            ReminderEvent(
                                key = "bud:${b.budgetId}:${b.periodStart}:over",
                                kind = ReminderKind.BUDGET_EXCEEDED,
                                title = "${b.name} is over budget",
                                body = "${b.percentUsed}% of the limit used",
                                targetId = b.budgetId
                            )
                        )
                        BudgetAlertLevel.WARNING -> add(
                            ReminderEvent(
                                key = "bud:${b.budgetId}:${b.periodStart}:warn",
                                kind = ReminderKind.BUDGET_WARNING,
                                title = "${b.name} is nearly used up",
                                body = "${b.percentUsed}% of the limit used",
                                targetId = b.budgetId
                            )
                        )
                        BudgetAlertLevel.NONE -> Unit
                    }
                }
            }
        }
        return events.filter { it.key !in sentKeys }
    }

    private fun daysUntil(today: LocalDate, dueMillis: Long, zone: ZoneId): Long =
        ChronoUnit.DAYS.between(today, Instant.ofEpochMilli(dueMillis).atZone(zone).toLocalDate())

    private fun whenText(days: Long) = when (days) {
        0L -> "is due today"
        1L -> "is due tomorrow"
        else -> "is due in $days days"
    }
}
