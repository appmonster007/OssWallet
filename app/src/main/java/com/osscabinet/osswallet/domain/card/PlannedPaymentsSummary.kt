package com.osscabinet.ossWallet.domain.card

import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
import com.osscabinet.ossWallet.domain.model.util.BigDecimalSerializer
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.domain.service.management.PlannedPaymentService
import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * Cash due in the next [days] days, in the card's currency.
 *
 * [outflow] is zero or negative and [inflow] is zero or positive. Transfers move money between the
 * user's own accounts, so they are left out. [overdueOutflow] is the (negative) total of overdue
 * expenses; it is already included in [outflow] when [overdueCounted] is true.
 */
@Serializable
data class PlannedPaymentsSummary(
    val days: Int,
    @Serializable(with = BigDecimalSerializer::class)
    val outflow: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val inflow: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val overdueOutflow: BigDecimal,
    val overdueCounted: Boolean
) {
    val net: BigDecimal get() = outflow + inflow

    companion object {
        /**
         * @param payments the payments to look at (already filtered by the card's account / category filter)
         * @param overdue payments whose next due date has passed, as the card lists them
         */
        fun build(
            payments: List<PlannedPayment>,
            overdue: List<PlannedPayment>,
            now: Long,
            days: Int,
            countOverdue: Boolean,
            targetCurrency: String,
            exchangeRates: ExchangeRateSnapshot
        ): PlannedPaymentsSummary {
            val windowEnd = now + days.toLong() * DAY_MS

            fun signed(p: PlannedPayment): BigDecimal? {
                val amount = CurrencyService.convertAmount(p.amount.abs(), p.currency, targetCurrency, exchangeRates)
                return when (p.entryMode) {
                    EntryMode.EXPENSE -> amount.negate()
                    EntryMode.INCOME -> amount
                    EntryMode.TRANSFER -> null
                }
            }

            var outflow = BigDecimal.ZERO
            var inflow = BigDecimal.ZERO
            PlannedPaymentService.generateUpcoming(
                payments.filter { it.status == PlannedPaymentStatus.ACTIVE }, now, days
            )
                .filter { it.scheduledDate in now..windowEnd }
                .forEach { item ->
                    val value = signed(item.plannedPayment) ?: return@forEach
                    if (value.signum() < 0) outflow += value else inflow += value
                }

            val overdueOutflow = overdue
                .filter { it.entryMode == EntryMode.EXPENSE }
                .fold(BigDecimal.ZERO) { acc, p -> acc + (signed(p) ?: BigDecimal.ZERO) }

            return PlannedPaymentsSummary(
                days = days,
                outflow = if (countOverdue) outflow + overdueOutflow else outflow,
                inflow = inflow,
                overdueOutflow = overdueOutflow,
                overdueCounted = countOverdue
            )
        }

        private const val DAY_MS = 24L * 60 * 60 * 1000
    }
}
