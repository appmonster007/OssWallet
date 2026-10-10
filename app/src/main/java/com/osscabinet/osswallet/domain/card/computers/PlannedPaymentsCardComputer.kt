package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.CardDataComputer
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.card.PlannedPaymentsSummary
import com.osscabinet.ossWallet.domain.card.resolveEffectiveFilter
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
import com.osscabinet.ossWallet.domain.model.PlannedPaymentSummary
import com.osscabinet.ossWallet.domain.model.settings.PlannedPaymentsCardConfig
import com.osscabinet.ossWallet.domain.model.settings.PlannedPaymentsViewMode
import com.osscabinet.ossWallet.domain.service.management.PlannedPaymentService
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService

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

        val accountIdSet = mergedFilter.accountIds.toHashSet()
        val categoryIdSet = mergedFilter.categoryIds.toHashSet()
        val currencySet = mergedFilter.currencies.toHashSet()
        val tagSet = mergedFilter.tags.toHashSet()

        // Filter planned payments based on the merged filter (categories, accounts, etc.)
        val filteredPayments = input.plannedPayments.filter { pp ->
            (accountIdSet.isEmpty() || pp.accountId in accountIdSet) &&
                    (categoryIdSet.isEmpty() || pp.categoryId in categoryIdSet) &&
                    (currencySet.isEmpty() || pp.currency in currencySet) &&
                    (tagSet.isEmpty() || pp.tags.any { it in tagSet })
        }

        val (rangeStart, rangeEnd) = TimelineService.getDateRange(mergedFilter.timelineRange)

        // Overdue: past-due payments, optionally bounded by the timeline start so stale items
        // outside the selected range are not surfaced.
        val overdue = PlannedPaymentService.getOverdue(filteredPayments, now).let { all ->
            if (rangeStart != null) all.filter { it.schedule.nextDueDate >= rangeStart } else all
        }

        // Upcoming: from now up to the timeline end date; null (AllTime) caps at 365 days.
        val allUpcoming = PlannedPaymentService.generateUpcoming(
            payments = filteredPayments,
            fromDate = now,
            toDate = rangeEnd
        )

        // Strip items that are already in overdue from the upcoming list
        val upcomingNonOverdue = allUpcoming.filter { !it.isOverdue }
        val upcomingById = upcomingNonOverdue.associateBy { it.plannedPayment.id }

        val (shownOverdue, shownUpcoming) = when (config.viewMode) {
            PlannedPaymentsViewMode.OVERDUE_ONLY -> overdue to emptyList()
            PlannedPaymentsViewMode.BOTH -> overdue to upcomingNonOverdue.take(config.maxItems)
            PlannedPaymentsViewMode.FUTURE_ONLY -> {
                val futureOnly = input.plannedPayments
                    .filter { it.status == PlannedPaymentStatus.ACTIVE }
                    .mapNotNull { payment -> upcomingById[payment.id] }
                    .sortedBy { it.scheduledDate }
                    .take(config.maxItems)
                emptyList<com.osscabinet.ossWallet.domain.model.PlannedPayment>() to futureOnly
            }
        }

        val categoriesById = input.categories.associateBy { it.id }
        val accountsById = input.accounts.associateBy { it.id }

        fun summarize(
            p: com.osscabinet.ossWallet.domain.model.PlannedPayment,
            scheduledDate: Long,
            isOverdue: Boolean
        ): PlannedPaymentSummary {
            val category = categoriesById[p.categoryId]
            return PlannedPaymentSummary(
                id = p.id,
                name = p.name,
                amount = p.amount,
                currency = p.currency,
                entryMode = p.entryMode,
                scheduledDate = scheduledDate,
                isOverdue = isOverdue,
                categoryName = category?.name.orEmpty(),
                categoryIcon = category?.iconName,
                categoryColor = category?.colorHex ?: -1,
                accountName = p.accountId?.let { accountsById[it]?.name }.orEmpty(),
                note = p.note
            )
        }

        val overdueSummaries = shownOverdue.map { summarize(it, it.schedule.nextDueDate, true) }
        val upcomingSummaries = shownUpcoming.map {
            summarize(it.plannedPayment, it.scheduledDate, false)
        }

        // Totals cover every payment due in the window, not just the rows shown above.
        val summary = if (config.summaryDays > 0) {
            PlannedPaymentsSummary.build(
                payments = filteredPayments,
                overdue = overdue,
                now = now,
                days = config.summaryDays,
                countOverdue = config.countOverdueInSummary,
                targetCurrency = input.targetCurrency,
                exchangeRates = input.exchangeRates
            )
        } else null

        return CardRenderData.PlannedPayments(
            config = config,
            items = overdueSummaries + upcomingSummaries,
            currency = input.targetCurrency,
            exchangeRates = input.exchangeRates,
            summary = summary
        )
    }
}
