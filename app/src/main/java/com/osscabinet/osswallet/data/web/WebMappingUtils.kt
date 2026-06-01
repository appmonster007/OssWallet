package com.osscabinet.ossWallet.data.web

import com.osscabinet.ossWallet.data.web.dto.*
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.domain.model.settings.CardConfig
import com.osscabinet.ossWallet.domain.model.settings.ScreenContext
import com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.management.PlannedPaymentService
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

internal suspend fun RoutingContext.authenticate(manager: AuthSessionManager): Boolean {
    val authHeader = call.request.headers["Authorization"] ?: run {
        call.respond(
            HttpStatusCode.Unauthorized,
            ApiResponse<Unit>(success = false, error = "Missing Authorization header")
        )
        return false
    }
    val token = authHeader.removePrefix("Bearer ").trim()
    if (!manager.validateToken(token)) {
        call.respond(
            HttpStatusCode.Unauthorized,
            ApiResponse<Unit>(success = false, error = "Session expired or invalid")
        )
        return false
    }
    return true
}

internal fun Transaction.toDto(
    categoryMap: Map<Long, Category>,
    accountMap: Map<String, Account>
) = TransactionDto(
    id = id,
    title = name,
    amount = amount.toDouble(),
    currency = currency,
    type = transactionType.name,
    categoryId = categoryId,
    categoryName = categoryMap[categoryId]?.name,
    accountId = accountId,
    accountName = accountMap[accountId]?.name ?: "",
    targetAccountId = targetAccountId,
    targetAccountName = accountMap[targetAccountId]?.name,
    targetCurrency = targetCurrency,
    transferRate = transferRate.toDouble(),
    date = timestamp,
    note = note.takeIf { it.isNotBlank() },
    payee = payee.takeIf { it.isNotBlank() },
    tags = tags.toList(),
    status = status.name,
    paymentMethod = paymentMethod?.name,
    warrantyExpiration = warrantyExpiration,
    location = location,
    linkedTransactionIds = emptyList() // Links are typically fetched separately or hydrated if needed
)

internal fun PlannedPayment.toDto(
    now: Long,
    categoryMap: Map<Long, Category>,
    accountMap: Map<String, Account>
): PlannedPaymentDto {
    val isOverdue = status == PlannedPaymentStatus.ACTIVE && schedule.nextDueDate < now
    return PlannedPaymentDto(
        id = id,
        name = name,
        amount = amount.toDouble(),
        currency = currency,
        transactionType = transactionType.name,
        categoryId = categoryId,
        categoryName = categoryMap[categoryId]?.name,
        accountId = accountId,
        accountName = accountMap[accountId]?.name,
        nextDueDate = schedule.nextDueDate,
        recurrenceDescription = PlannedPaymentService.formatRecurrenceHuman(schedule.recurrenceRule),
        status = status.name,
        isOverdue = isOverdue,
        autoConfirm = autoConfirm
    )
}

internal fun Category.toDto(): CategoryDto = CategoryDto(
    id = id,
    name = name,
    parentId = parentId,
    colorHex = colorHex,
    nature = nature.name,
    iconName = iconName,
    isDefault = isDefault
)

internal fun TransactionFilter.toFilterSummaryDto(): FilterSummaryDto {
    val (timelineType, dateFrom, dateTo) = when (val tl = timelineRange) {
        is TimelineRange.AllTime -> Triple("ALL_TIME", null, null)
        is TimelineRange.CustomRange -> Triple("CUSTOM", tl.start, tl.end)
        is TimelineRange.ThisPeriod -> Triple(tl.period.name, null, null)
        is TimelineRange.LastPeriod -> Triple(tl.period.name, null, null)
        else -> Triple("ALL_TIME", null, null)
    }
    return FilterSummaryDto(
        timelineType = timelineType, dateFrom = dateFrom, dateTo = dateTo,
        accountIds = accountIds.toList(),
        categoryIds = categoryIds.toList(),
        transactionTypes = transactionTypes.map { it.name },
        tags = tags.toList(),
        currencies = currencies.toList(),
        statuses = statuses.map { it.name },
        paymentMethods = paymentMethods.map { it.name },
        minAmount = minAmount?.toDouble(),
        maxAmount = maxAmount?.toDouble(),
        queryNote = queryNote,
        queryPayee = queryPayee,
        excludeFuture = excludeFuture
    )
}

internal fun CardRenderData.toPayloadDto(
    now: Long,
    categoryMap: Map<Long, Category>,
    accountMap: Map<String, Account>
): CardPayloadDto = when (this) {
    is CardRenderData.NetWorth -> CardPayloadDto.NetWorth(
        netWorth = currentMetrics.netWorth.toDouble(),
        income = currentMetrics.cashFlow.totalIncomeInBase.toDouble(),
        expense = currentMetrics.cashFlow.totalExpensesInBase.toDouble(),
        transfer = currentMetrics.cashFlow.totalTransfersInBase.toDouble(),
        cashFlow = currentMetrics.cashFlow.netInBase.toDouble(),
        previousNetWorth = previousMetrics?.netWorth?.toDouble(),
        currency = this.baseCurrency
    )

    is CardRenderData.CashFlow -> CardPayloadDto.CashFlow(
        income = result.totalIncomeInBase.toDouble(),
        expense = result.totalExpensesInBase.toDouble(),
        transfer = result.totalTransfersInBase.toDouble(),
        net = result.netInBase.toDouble(),
        currency = this.baseCurrency
    )

    is CardRenderData.CurrencyBalances -> CardPayloadDto.CurrencyBalances(
        items = items.map { item ->
            CardPayloadDto.CurrencyBalanceItem(
                currency = item.currency,
                balance = item.amount.toDouble(),
                balanceInBase = item.amountInBase.toDouble()
            )
        },
        baseCurrency = this.baseCurrency
    )

    is CardRenderData.Accounts -> CardPayloadDto.Accounts(
        accounts = accountStats.map { (id, stats) ->
            CardPayloadDto.AccountSummaryItem(
                id = id,
                name = stats.account.name,
                currency = stats.account.currencyCode,
                balance = stats.balance.toDouble(),
                income = stats.incomeInAccountCurrency.toDouble(),
                expense = stats.expenseInAccountCurrency.toDouble(),
                transfer = stats.transferInAccountCurrency.toDouble(),
                balanceInBase = stats.balanceInBase.toDouble()
            )
        }
    )

    is CardRenderData.Budgets -> CardPayloadDto.Budgets(
        items = items.map { progress ->
            val cal = Calendar.getInstance()
            val periodLabel = "${cal.get(Calendar.YEAR)}-${
                (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
            }"
            BudgetDto(
                id = progress.budget.id, name = progress.budget.name,
                currency = progress.budget.currency,
                limitAmount = progress.budget.limitAmount.toDouble(),
                spent = progress.spent.toDouble(),
                remaining = progress.remaining.toDouble(),
                percentage = progress.percentage.toDouble(),
                alertLevel = progress.alertLevel.name,
                period = periodLabel,
                isArchived = progress.budget.isArchived
            )
        }
    )

    is CardRenderData.PlannedPayments -> CardPayloadDto.PlannedPayments(
        upcoming = upcoming.map { item ->
            item.plannedPayment.toDto(
                now,
                categoryMap,
                accountMap
            )
        },
        overdue = overdue.map { it.toDto(now, categoryMap, accountMap) }
    )

    is CardRenderData.Loans -> CardPayloadDto.Loans(
        items = items.map { summary ->
            LoanDto(
                id = summary.loan.id, name = summary.loan.name,
                loanType = summary.loan.loanType.name,
                currency = summary.loan.currency,
                principalAmount = summary.loan.principalAmount.toDouble(),
                totalPaid = summary.totalPaid.toDouble(),
                remainingBalance = summary.remainingBalance.toDouble(),
                interestAccrued = summary.interestAccrued.toDouble(),
                status = summary.loan.status.name,
                lenderOrBorrowerName = summary.loan.lenderOrBorrowerName,
                startDate = summary.loan.startDate,
                expectedSettlementDate = summary.loan.expectedSettlementDate
            )
        }
    )

    is CardRenderData.RecentActivities -> {
        val txList = listState.groups.flatMap { it.transactions }
        CardPayloadDto.RecentActivities(
            transactions = txList.take(20).mapNotNull { uiModel ->
                if (uiModel is ConfirmedTransactionUiModel) {
                    uiModel.item.transaction.toDto(categoryMap, accountMap)
                } else if (uiModel is PlannedPaymentUiModel) {
                    // Map planned to transaction DTO
                    TransactionDto(
                        id = 0,
                        title = uiModel.categoryName, // or some other name if available
                        amount = uiModel.amount.toDouble(),
                        currency = uiModel.currency,
                        type = "EXPENSE", // Default for planned
                        categoryId = null,
                        categoryName = uiModel.categoryName,
                        accountId = null,
                        accountName = uiModel.accountName,
                        date = uiModel.timestamp,
                        note = uiModel.note,
                        isPlanned = true,
                        isOverdue = uiModel.isOverdue,
                        paymentId = uiModel.paymentId
                    )
                } else null
            },
            total = listState.groups.sumOf { it.transactions.size }
        )
    }

    is CardRenderData.CategoryPieChart -> {
        val total = items.sumOf { it.expense.toDouble() }
        CardPayloadDto.CategoryBreakdown(
            items = items.map { metrics ->
                CategoryBreakdownDto(
                    categoryId = metrics.categoryId ?: 0L,
                    categoryName = metrics.category,
                    amount = metrics.expense.toDouble(),
                    percentage = if (total > 0.0) (metrics.expense.toDouble() / total * 100.0).toBigDecimal()
                        .setScale(2, RoundingMode.HALF_UP).toDouble() else 0.0,
                    transactionCount = 0,
                    colorHex = "#${(metrics.colorHex and 0xFFFFFF).toString(16).padStart(6, '0')}"
                )
            },
            displayCurrency = displayCurrency
        )
    }

    is CardRenderData.CategoryTreemap -> {
        val total = items.sumOf { it.expense.toDouble() }
        CardPayloadDto.CategoryBreakdown(
            items = items.map { metrics ->
                CategoryBreakdownDto(
                    categoryId = metrics.categoryId ?: 0L,
                    categoryName = metrics.category,
                    amount = metrics.expense.toDouble(),
                    percentage = if (total > 0.0) (metrics.expense.toDouble() / total * 100.0).toBigDecimal()
                        .setScale(2, RoundingMode.HALF_UP).toDouble() else 0.0,
                    transactionCount = 0,
                    colorHex = "#${(metrics.colorHex and 0xFFFFFF).toString(16).padStart(6, '0')}"
                )
            },
            displayCurrency = displayCurrency
        )
    }

    is CardRenderData.CashFlowComparison -> CardPayloadDto.CashFlowComparison(
        current = currentSeries.points.map { p ->
            CashFlowPointDto(
                label = p.label,
                epochMillis = p.epochMillis,
                income = p.income.toDouble(),
                expense = p.expense.toDouble(),
                net = p.net.toDouble()
            )
        },
        previous = previousSeries.points.map { p ->
            CashFlowPointDto(
                label = p.label,
                epochMillis = p.epochMillis,
                income = p.income.toDouble(),
                expense = p.expense.toDouble(),
                net = p.net.toDouble()
            )
        },
        lastYear = lastYearSeries.points.map { p ->
            CashFlowPointDto(
                label = p.label,
                epochMillis = p.epochMillis,
                income = p.income.toDouble(),
                expense = p.expense.toDouble(),
                net = p.net.toDouble()
            )
        },
        granularity = granularity.name,
        baseCurrency = this.baseCurrency
    )

    is CardRenderData.IncomeExpenseBar -> CardPayloadDto.IncomeExpenseBar(
        buckets = buckets.map { p ->
            CashFlowPointDto(
                label = p.label,
                epochMillis = p.epochMillis,
                income = p.income.toDouble(),
                expense = p.expense.toDouble(),
                net = p.net.toDouble()
            )
        },
        granularity = granularity.name,
        baseCurrency = this.baseCurrency,
        todayBucketIndex = todayBucketIndex
    )

    is CardRenderData.CategorySpendingTrend -> CardPayloadDto.CategorySpendingTrend(
        buckets = buckets.map { bucket ->
            CardPayloadDto.CategorySpendingBucketDto(
                label = bucket.label,
                epochMillis = bucket.epochMillis,
                categories = bucket.categoryAmounts.map { ca ->
                    CardPayloadDto.CategoryAmountDto(
                        categoryId = ca.categoryId,
                        categoryName = ca.categoryName,
                        amount = ca.amount.toDouble(),
                        nature = ca.nature.name
                    )
                }
            )
        },
        granularity = granularity.name,
        baseCurrency = this.baseCurrency
    )

    is CardRenderData.TagUpSetPlot -> CardPayloadDto.TagUpSetPlot(
        sets = sets.map { s ->
            CardPayloadDto.TagSetDto(
                tagName = s.tagName, totalAmount = s.totalAmount.toDouble(),
                income = s.income.toDouble(), expense = s.expense.toDouble(),
                transactionCount = s.transactionCount,
                colorHex = if (s.colorHex != -1) "#${(s.colorHex and 0xFFFFFF).toString(16).padStart(6, '0')}" else null
            )
        },
        intersections = intersections.map { i ->
            CardPayloadDto.TagIntersectionDto(
                tags = i.tags.toList(), amount = i.amount.toDouble(),
                income = i.income.toDouble(), expense = i.expense.toDouble(),
                transactionCount = i.transactionCount
            )
        },
        baseCurrency = this.baseCurrency
    )

    is CardRenderData.AccountHero -> CardPayloadDto.NetWorth(
        netWorth = balance.toDouble(),
        income = income.toDouble(),
        expense = expense.toDouble(),
        transfer = 0.0,
        cashFlow = (income - expense).toDouble(),
        previousNetWorth = null,
        currency = account.currencyCode
    )
}

internal fun Filter.toDto(): FilterDto {
    val tf = this.filter
    val (timelineType, dateFrom, dateTo) = when (val tl = tf.timelineRange) {
        is TimelineRange.AllTime -> Triple("ALL_TIME", null, null)
        is TimelineRange.CustomRange -> Triple("CUSTOM", tl.start, tl.end)
        is TimelineRange.ThisPeriod -> Triple(tl.period.name, null, null)
        is TimelineRange.LastPeriod -> Triple(tl.period.name, null, null)
        else -> Triple("ALL_TIME", null, null)
    }
    return FilterDto(
        id = id, name = name,
        timelineType = timelineType, dateFrom = dateFrom, dateTo = dateTo,
        queryNote = tf.queryNote, isNoteRegex = tf.isNoteRegex,
        queryPayee = tf.queryPayee, isPayeeRegex = tf.isPayeeRegex,
        categoryIds = tf.categoryIds.toList(),
        accountIds = tf.accountIds.toList(),
        paymentMethods = tf.paymentMethods.map { it.name },
        transactionTypes = tf.transactionTypes.map { it.name },
        currencies = tf.currencies.toList(),
        tags = tf.tags.toList(),
        statuses = tf.statuses.map { it.name },
        minAmount = tf.minAmount?.toDouble(),
        maxAmount = tf.maxAmount?.toDouble(),
        hasWarranty = tf.hasWarranty,
        excludeFuture = tf.excludeFuture
    )
}

internal fun CreateFilterRequest.toTransactionFilter(): TransactionFilter {
    val timelineRange = when (timelineType.uppercase()) {
        "ALL_TIME" -> TimelineRange.AllTime
        "CUSTOM" -> TimelineRange.CustomRange(start = dateFrom, end = dateTo)
        "MONTH" -> TimelineRange.ThisPeriod(ThisPeriodType.MONTH)
        "YEAR" -> TimelineRange.ThisPeriod(ThisPeriodType.YEAR)
        "WEEK" -> TimelineRange.ThisPeriod(ThisPeriodType.WEEK)
        "TODAY" -> TimelineRange.ThisPeriod(ThisPeriodType.TODAY)
        "QUARTER" -> TimelineRange.ThisPeriod(ThisPeriodType.QUARTER)
        else -> TimelineRange.AllTime
    }
    return TransactionFilter(
        timelineRange = timelineRange,
        queryNote = queryNote,
        isNoteRegex = isNoteRegex,
        queryPayee = queryPayee,
        isPayeeRegex = isPayeeRegex,
        categoryIds = categoryIds.toSet(),
        accountIds = accountIds.toSet(),
        paymentMethods = paymentMethods.mapNotNull { m ->
            PaymentMethod.entries.find { it.name.equals(m, ignoreCase = true) }
        }.toSet(),
        transactionTypes = transactionTypes.mapNotNull { t ->
            TransactionType.entries.find { it.name.equals(t, ignoreCase = true) }
        }.toSet(),
        currencies = currencies.toSet(),
        tags = tags.toSet(),
        statuses = statuses.mapNotNull { s ->
            TransactionStatus.entries.find { it.name.equals(s, ignoreCase = true) }
        }.toSet(),
        minAmount = minAmount?.let { BigDecimal.valueOf(it) },
        maxAmount = maxAmount?.let { BigDecimal.valueOf(it) },
        hasWarranty = hasWarranty,
        excludeFuture = excludeFuture
    )
}

internal fun Template.toTemplateDto() = TemplateDto(
    id = id,
    name = name,
    categoryId = categoryId,
    amount = amount.toDouble(),
    transactionType = transactionType.name,
    currency = currency,
    accountId = accountId,
    targetAccountId = targetAccountId,
    targetCurrency = targetCurrency,
    transferRate = transferRate.toDouble(),
    note = note,
    payee = payee,
    location = location,
    gpsLocation = gpsLocation,
    warrantyExpiration = warrantyExpiration,
    transactionStatus = transactionStatus.name,
    paymentMethod = paymentMethod?.name,
    tags = tags.toList(),
    icon = icon,
    colorHex = colorHex,
    isStarred = isStarred,
    displayOrder = displayOrder
)

internal fun CardType.toDefaultCardConfig(): CardConfig {
    val cardId = "${serialName}_${System.currentTimeMillis()}"
    return CardConfig.createDefault(
        type = this,
        context = ScreenContext.DASHBOARD,
        cardId = cardId
    )
}

internal fun CardConfig.withOrder(newOrder: Int): CardConfig = when (this) {
    is com.osscabinet.ossWallet.domain.model.settings.NetWorthCardConfig -> copy(order = newOrder)
    is com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig -> copy(order = newOrder)
    is com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig -> copy(order = newOrder)
    is com.osscabinet.ossWallet.domain.model.settings.AccountsCardConfig -> copy(order = newOrder)
    is com.osscabinet.ossWallet.domain.model.settings.CategoryPieChartCardConfig -> copy(order = newOrder)
    is com.osscabinet.ossWallet.domain.model.settings.CategoryTreemapCardConfig -> copy(order = newOrder)
    is com.osscabinet.ossWallet.domain.model.settings.PlannedPaymentsCardConfig -> copy(order = newOrder)
    is com.osscabinet.ossWallet.domain.model.settings.BudgetsCardConfig -> copy(order = newOrder)
    is com.osscabinet.ossWallet.domain.model.settings.LoansCardConfig -> copy(order = newOrder)
    is com.osscabinet.ossWallet.domain.model.settings.AccountHeroCardConfig -> copy(order = newOrder)
    is com.osscabinet.ossWallet.domain.model.settings.RecentActivitiesCardConfig -> copy(order = newOrder)
    is com.osscabinet.ossWallet.domain.model.settings.CashFlowComparisonCardConfig -> copy(order = newOrder)
    is com.osscabinet.ossWallet.domain.model.settings.IncomeExpenseBarCardConfig -> copy(order = newOrder)
    is com.osscabinet.ossWallet.domain.model.settings.CategorySpendingTrendCardConfig -> copy(
        order = newOrder
    )

    is com.osscabinet.ossWallet.domain.model.settings.TagUpSetPlotCardConfig -> copy(order = newOrder)
}

internal fun CardConfig.withFilter(newFilter: TransactionFilter): CardConfig = when (this) {
    is com.osscabinet.ossWallet.domain.model.settings.NetWorthCardConfig -> copy(filter = newFilter)
    is com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig -> copy(filter = newFilter)
    is com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig -> copy(filter = newFilter)
    is com.osscabinet.ossWallet.domain.model.settings.AccountsCardConfig -> copy(filter = newFilter)
    is com.osscabinet.ossWallet.domain.model.settings.CategoryPieChartCardConfig -> copy(filter = newFilter)
    is com.osscabinet.ossWallet.domain.model.settings.CategoryTreemapCardConfig -> copy(filter = newFilter)
    is com.osscabinet.ossWallet.domain.model.settings.PlannedPaymentsCardConfig -> copy(filter = newFilter)
    is com.osscabinet.ossWallet.domain.model.settings.BudgetsCardConfig -> copy(filter = newFilter)
    is com.osscabinet.ossWallet.domain.model.settings.LoansCardConfig -> copy(filter = newFilter)
    is com.osscabinet.ossWallet.domain.model.settings.AccountHeroCardConfig -> copy(filter = newFilter)
    is com.osscabinet.ossWallet.domain.model.settings.RecentActivitiesCardConfig -> copy(filter = newFilter)
    is com.osscabinet.ossWallet.domain.model.settings.CashFlowComparisonCardConfig -> copy(filter = newFilter)
    is com.osscabinet.ossWallet.domain.model.settings.IncomeExpenseBarCardConfig -> copy(filter = newFilter)
    is com.osscabinet.ossWallet.domain.model.settings.CategorySpendingTrendCardConfig -> copy(
        filter = newFilter
    )
    is com.osscabinet.ossWallet.domain.model.settings.TagUpSetPlotCardConfig -> copy(filter = newFilter)
}

internal fun CardFilterUpdateDto.toDomain(): TransactionFilter {
    val timelineRange = when (timelineType?.uppercase()) {
        "ALL_TIME" -> TimelineRange.AllTime
        "CUSTOM" -> TimelineRange.CustomRange(start = dateFrom, end = dateTo)
        "MONTH" -> TimelineRange.ThisPeriod(ThisPeriodType.MONTH)
        "YEAR" -> TimelineRange.ThisPeriod(ThisPeriodType.YEAR)
        "WEEK" -> TimelineRange.ThisPeriod(ThisPeriodType.WEEK)
        "TODAY" -> TimelineRange.ThisPeriod(ThisPeriodType.TODAY)
        "QUARTER" -> TimelineRange.ThisPeriod(ThisPeriodType.QUARTER)
        "HALF_YEAR" -> TimelineRange.ThisPeriod(ThisPeriodType.HALF_YEAR)
        "LAST_7_DAYS" -> TimelineRange.LastPeriod(LastPeriodType.LAST_7_DAYS)
        "LAST_30_DAYS" -> TimelineRange.LastPeriod(LastPeriodType.LAST_30_DAYS)
        "LAST_6_MONTHS" -> TimelineRange.LastPeriod(LastPeriodType.LAST_6_MONTHS)
        "LAST_1_YEAR" -> TimelineRange.LastPeriod(LastPeriodType.LAST_1_YEAR)
        else -> TimelineRange.AllTime
    }
    return TransactionFilter(
        timelineRange = timelineRange,
        queryNote = queryNote,
        queryPayee = queryPayee,
        categoryIds = categoryIds.toSet(),
        accountIds = accountIds.toSet(),
        transactionTypes = transactionTypes.mapNotNull { t ->
            TransactionType.entries.find { it.name.equals(t, ignoreCase = true) }
        }.toSet(),
        currencies = currencies.toSet(),
        tags = tags.toSet(),
        statuses = statuses.mapNotNull { s ->
            TransactionStatus.entries.find { it.name.equals(s, ignoreCase = true) }
        }.toSet(),
        paymentMethods = paymentMethods.mapNotNull { m ->
            PaymentMethod.entries.find { it.name.equals(m, ignoreCase = true) }
        }.toSet(),
        minAmount = minAmount?.let { BigDecimal.valueOf(it) },
        maxAmount = maxAmount?.let { BigDecimal.valueOf(it) },
        excludeFuture = excludeFuture
    )
}
