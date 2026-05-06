package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.CardDataComputer
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
import com.osscabinet.ossWallet.domain.model.settings.PlannedPaymentsCardConfig
import com.osscabinet.ossWallet.domain.model.settings.PlannedPaymentsViewMode
import com.osscabinet.ossWallet.domain.service.lifecycle.PlannedPaymentService

class PlannedPaymentsCardComputer :
    CardDataComputer<PlannedPaymentsCardConfig, CardRenderData.PlannedPayments> {
    override val supportedType = CardType.PLANNED_PAYMENTS

    override fun compute(
        input: CardInput,
        config: PlannedPaymentsCardConfig
    ): CardRenderData.PlannedPayments {
        val now = System.currentTimeMillis()

        val overdue = PlannedPaymentService.getOverdue(input.plannedPayments, now)
        val overdueIds = overdue.map { it.id }.toSet()

        val allUpcoming = PlannedPaymentService.generateUpcoming(
            payments = input.plannedPayments,
            fromDate = now,
            daysAhead = config.daysAhead
        )
        // Strip items that are already in overdue from the upcoming list
        val upcomingNonOverdue = allUpcoming.filter { it.plannedPayment.id !in overdueIds }

        val (shownOverdue, shownUpcoming) = when (config.viewMode) {
            PlannedPaymentsViewMode.OVERDUE_ONLY -> overdue to emptyList()
            PlannedPaymentsViewMode.BOTH -> overdue to upcomingNonOverdue.take(config.maxItems)
            PlannedPaymentsViewMode.FUTURE_ONLY -> {
                // One nearest upcoming occurrence per active payment schedule
                val futureOnly = input.plannedPayments
                    .filter { it.status == PlannedPaymentStatus.ACTIVE && it.id !in overdueIds }
                    .mapNotNull { payment ->
                        allUpcoming.firstOrNull { it.plannedPayment.id == payment.id && !it.isOverdue }
                    }
                    .sortedBy { it.scheduledDate }
                    .take(config.maxItems)
                emptyList<com.osscabinet.ossWallet.domain.model.PlannedPayment>() to futureOnly
            }
        }

        return CardRenderData.PlannedPayments(
            config = config,
            upcoming = shownUpcoming,
            overdue = shownOverdue,
            precision = input.precision
        )
    }
}
