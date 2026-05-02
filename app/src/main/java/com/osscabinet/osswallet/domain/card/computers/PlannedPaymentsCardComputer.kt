package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.CardDataComputer
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.model.settings.PlannedPaymentsCardConfig
import com.osscabinet.ossWallet.domain.service.PlannedPaymentService

class PlannedPaymentsCardComputer : CardDataComputer<PlannedPaymentsCardConfig, CardRenderData.PlannedPayments> {
    override val supportedType = CardType.PLANNED_PAYMENTS

    override fun compute(input: CardInput, config: PlannedPaymentsCardConfig): CardRenderData.PlannedPayments {
        val now = System.currentTimeMillis()

        val upcoming = PlannedPaymentService.generateUpcoming(
            payments = input.plannedPayments,
            fromDate = now,
            daysAhead = config.daysAhead
        ).take(config.maxItems)

        val overdue = if (config.showOverdue) {
            PlannedPaymentService.getOverdue(input.plannedPayments, now)
        } else {
            emptyList()
        }

        return CardRenderData.PlannedPayments(
            config = config,
            upcoming = upcoming,
            overdue = overdue,
            precision = input.precision
        )
    }
}
