package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.CardDataComputer
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.card.resolveEffectiveFilter
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
import com.osscabinet.ossWallet.domain.model.settings.PlannedPaymentsCardConfig
import com.osscabinet.ossWallet.domain.model.settings.PlannedPaymentsViewMode
import com.osscabinet.ossWallet.domain.service.management.PlannedPaymentService

class PlannedPaymentsCardComputer :
    CardDataComputer<PlannedPaymentsCardConfig, CardRenderData.PlannedPayments> {
    override val supportedType = CardType.PLANNED_PAYMENTS

    override suspend fun compute(
        input: CardInput,
        config: PlannedPaymentsCardConfig
    ): CardRenderData.PlannedPayments {
        val mergedFilter = resolveEffectiveFilter(
            input.filter,
            config.filter,
            config.filterRules,
            input.screenOwnsTimeline
        )
        val now = System.currentTimeMillis()

        // Filter planned payments based on the merged filter (categories, accounts, etc.)
        val filteredPayments = input.plannedPayments.filter { pp ->
            (mergedFilter.accountIds.isEmpty() || pp.accountId in mergedFilter.accountIds) &&
                    (mergedFilter.categoryIds.isEmpty() || pp.categoryId in mergedFilter.categoryIds) &&
                    (mergedFilter.currencies.isEmpty() || pp.currency in mergedFilter.currencies) &&
                    (mergedFilter.tags.isEmpty() || pp.tags.any { it in mergedFilter.tags })
        }

        // Honor excludeFuture toggle if it's part of the card/screen filter.
        // If excludeFuture is true, we only show overdue payments (which are technically in the past).
        val overdue = PlannedPaymentService.getOverdue(filteredPayments, now)
        val overdueIds = overdue.map { it.id }.toSet()

        val allUpcoming = if (mergedFilter.excludeFuture) emptyList() else PlannedPaymentService.generateUpcoming(
            payments = filteredPayments,
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
                val futureOnly = if (mergedFilter.excludeFuture) emptyList() else input.plannedPayments
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
            overdue = shownOverdue
        )
    }
}
