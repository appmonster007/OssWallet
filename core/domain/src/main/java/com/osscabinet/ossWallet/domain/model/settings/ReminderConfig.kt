package com.osscabinet.ossWallet.domain.model.settings

data class ReminderConfig(
    val enabled: Boolean = true,
    /** How many days ahead of a due date the reminder fires. */
    val leadDays: Int = DEFAULT_LEAD_DAYS,
    /** Local hour (0-23) before which no reminder is posted. */
    val hourOfDay: Int = DEFAULT_HOUR,
    val plannedPayments: Boolean = true,
    val loans: Boolean = true,
    val budgetAlerts: Boolean = true,
    /** A note when repeated payments are found that could become planned payments. */
    val suggestions: Boolean = true
) {
    companion object {
        const val DEFAULT_LEAD_DAYS = 2
        const val DEFAULT_HOUR = 9
        val LEAD_DAY_OPTIONS = listOf(0, 1, 2, 3, 7)
        val HOUR_OPTIONS = listOf(7, 8, 9, 12, 18, 20)
    }
}
