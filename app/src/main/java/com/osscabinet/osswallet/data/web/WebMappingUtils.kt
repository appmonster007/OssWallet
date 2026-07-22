package com.osscabinet.ossWallet.data.web

import com.osscabinet.ossWallet.data.web.dto.*
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.domain.model.settings.AccountingPerspective
import com.osscabinet.ossWallet.domain.model.settings.CardConfig
import com.osscabinet.ossWallet.domain.model.settings.ChartMetric
import com.osscabinet.ossWallet.domain.model.settings.ScreenContext
import com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.management.PlannedPaymentService
import com.osscabinet.ossWallet.domain.service.financial.CategoryTreeMetrics
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
    paymentMethod = paymentMethod.name,
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
        transactionType = entryMode.name,
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

internal fun PlannedPaymentSummary.toDto() = PlannedPaymentDto(
    id = id,
    name = name,
    amount = amount.toDouble(),
    currency = currency,
    transactionType = entryMode.name,
    categoryId = 0,
    categoryName = null,
    accountId = null,
    accountName = null,
    nextDueDate = scheduledDate,
    recurrenceDescription = "",
    status = if (isOverdue) "ACTIVE" else "ACTIVE",
    isOverdue = isOverdue,
    autoConfirm = false
)

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
        excludeFuture = timelineRange.excludeFuture
    )
}

internal fun CategoryTreeMetrics.toCategoryBreakdownDto(total: Double, metric: ChartMetric): CategoryBreakdownDto {
    val amountVal = when (metric) {
        ChartMetric.INCOME -> income.toDouble()
        ChartMetric.EXPENSE -> expense.abs().toDouble()
        ChartMetric.INFLOW -> (income + transferIn).toDouble()
        ChartMetric.OUTFLOW -> (expense + transferOut).abs().toDouble()
        else -> netBalance.abs().toDouble()
    }
    val percentageVal = if (total > 0.0 && amountVal.isFinite() && total.isFinite()) {
        runCatching {
            (amountVal / total * 100.0).toBigDecimal()
                .setScale(2, RoundingMode.HALF_UP).toDouble()
        }.getOrDefault(0.0)
    } else 0.0

    return CategoryBreakdownDto(
        categoryId = categoryId ?: 0L,
        categoryName = category,
        amount = if (amountVal.isFinite()) amountVal else 0.0,
        percentage = percentageVal,
        transactionCount = 0,
        colorHex = "#${(colorHex and 0xFFFFFF).toString(16).padStart(6, '0')}",
        subcategories = subcategoryData.map { it.toCategoryBreakdownDto(total, metric) }
    )
}

internal fun CardRenderData.toPayloadDto(
    now: Long,
    categoryMap: Map<Long, Category>,
    accountMap: Map<String, Account>
): CardPayloadDto? = when (this) {
    is CardRenderData.NetWorth -> CardPayloadDto.NetWorth(
        netWorth = currentPeriodMetrics.netWorth.toDouble(),
        income = currentPeriodMetrics.cashFlow.totalIncomeInBase.toDouble(),
        expense = currentPeriodMetrics.cashFlow.totalExpensesInBase.toDouble(),
        transfer = currentPeriodMetrics.cashFlow.totalNetTransferInBase.toDouble(),
        cashFlow = currentPeriodMetrics.cashFlow.netInBase.toDouble(),
        previousNetWorth = previousPeriodMetrics?.netWorth?.toDouble(),
        previousIncome = previousPeriodMetrics?.cashFlow?.totalIncomeInBase?.toDouble(),
        previousExpense = previousPeriodMetrics?.cashFlow?.totalExpensesInBase?.toDouble(),
        previousTransfer = previousPeriodMetrics?.cashFlow?.totalNetTransferInBase?.toDouble(),
        currency = this.currency,
        fontSize = config.fontSize,
        perspective = when {
            config.perspective is AccountingPerspective.Accounting -> "Accounting"
            config.perspective is AccountingPerspective.CashFlow -> "CashFlow"
            else -> "Accounting"
        },
        showComparison = config.showComparison
    )

    is CardRenderData.CashFlow -> CardPayloadDto.CashFlow(
        income = result.totalIncomeInBase.toDouble(),
        expense = result.totalExpensesInBase.toDouble(),
        transfer = result.totalTransfersInBase.toDouble(),
        net = result.netInBase.toDouble(),
        currency = this.currency,
        perspective = when {
            config.perspective is AccountingPerspective.Accounting -> "Accounting"
            config.perspective is AccountingPerspective.CashFlow -> "CashFlow"
            else -> "Accounting"
        },
        showNetColumn = config.showNetColumn,
        showBaseConversion = config.showBaseConversion,
        breakdown = result.breakdown.map { item ->
            CardPayloadDto.CashFlowBreakdownItemDto(
                currency = item.currency,
                income = item.income.toDouble(),
                incomeInBase = item.incomeInBase.toDouble(),
                expense = item.expense.toDouble(),
                expenseInBase = item.expenseInBase.toDouble(),
                transfer = item.transfer.toDouble(),
                transferInBase = item.transferInBase.toDouble(),
                transferIn = item.transferIn.toDouble(),
                transferInInBase = item.transferInInBase.toDouble(),
                transferOut = item.transferOut.toDouble(),
                transferOutInBase = item.transferOutInBase.toDouble()
            )
        }
    )

    is CardRenderData.CurrencyBalances -> CardPayloadDto.CurrencyBalances(
        items = items.map { item ->
            CardPayloadDto.CurrencyBalanceItem(
                currency = item.currency,
                balance = item.amount.toDouble(),
                balanceInBase = item.amountInBase.toDouble()
            )
        },
        baseCurrency = this.currency,
        layout = config.layout,
        gridColumns = config.gridColumns,
        showBaseConversion = config.showBaseConversion
    )

    is CardRenderData.Accounts -> CardPayloadDto.Accounts(
        accounts = accountSummaries.map { (id, stats) ->
            CardPayloadDto.AccountSummaryItem(
                id = id,
                name = stats.account.name,
                currency = stats.account.currencyCode,
                balance = stats.balance.toDouble(),
                income = stats.currentPeriodFlow.income.toDouble(),
                expense = stats.currentPeriodFlow.expense.toDouble(),
                transfer = stats.currentPeriodFlow.netTransfer.toDouble(),
                balanceInBase = stats.balanceInBase.toDouble()
            )
        },
        layout = config.layout,
        gridColumns = config.gridColumns,
        showAddAccountCard = config.showAddAccountCard,
        perspective = when {
            config.perspective is AccountingPerspective.Accounting -> "Accounting"
            config.perspective is AccountingPerspective.CashFlow -> "CashFlow"
            else -> "Accounting"
        },
        excludeInternalTransfers = config.excludeInternalTransfers
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
        },
        showArchived = config.showArchived,
        maxItems = config.maxItems
    )

    is CardRenderData.PlannedPayments -> CardPayloadDto.PlannedPayments(
        upcoming = items.filter { !it.isOverdue }.map { it.toDto() },
        overdue = items.filter { it.isOverdue }.map { it.toDto() },
        viewMode = config.viewMode.name,
        maxItems = config.maxItems
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
        },
        showType = config.showType,
        maxItems = config.maxItems
    )

    is CardRenderData.RecentActivities -> {
        CardPayloadDto.RecentActivities(
            transactions = emptyList(), // We'll use groups
            total = listState.groups.sumOf { it.transactions.size },
            groups = listState.groups.map { group ->
                CardPayloadDto.TransactionGroupDto(
                    label = group.headerData.title,
                    transactions = group.transactions.mapNotNull { uiModel ->
                        if (uiModel is ConfirmedTransactionUiModel) {
                            uiModel.item.transaction.toDto(categoryMap, accountMap)
                        } else if (uiModel is PlannedPaymentUiModel) {
                            TransactionDto(
                                id = 0,
                                title = uiModel.categoryName ?: "Planned Payment",
                                amount = uiModel.amount.toDouble(),
                                currency = uiModel.currency,
                                type = "EXPENSE",
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
                    }
                )
            },
            groupBy = config.groupBy.name,
            showAccount = config.showAccount,
            showCategory = config.showCategory,
            showNote = config.showNote,
            showTags = config.showTags,
            showTimestamp = config.showTimestamp,
            showIcon = config.showIcon,
            showOpeningBalance = config.showOpeningBalance,
            showClosingBalance = config.showClosingBalance
        )
    }

    is CardRenderData.CategoryPieChart -> {
        val metric = config.selectedMetric
        val total = items.sumOf {
            when (metric) {
                ChartMetric.INCOME -> it.income.toDouble()
                ChartMetric.EXPENSE -> it.expense.abs().toDouble()
                ChartMetric.INFLOW -> (it.income + it.transferIn).toDouble()
                ChartMetric.OUTFLOW -> (it.expense + it.transferOut).abs().toDouble()
                else -> it.netBalance.abs().toDouble()
            }
        }
        CardPayloadDto.CategoryBreakdown(
            items = items.map { it.toCategoryBreakdownDto(total, metric) },
            displayCurrency = currency
        )
    }

    is CardRenderData.CategoryTreemap -> {
        val metric = config.selectedMetric
        val total = items.sumOf {
            when (metric) {
                ChartMetric.INCOME -> it.income.toDouble()
                ChartMetric.EXPENSE -> it.expense.abs().toDouble()
                ChartMetric.INFLOW -> (it.income + it.transferIn).toDouble()
                ChartMetric.OUTFLOW -> (it.expense + it.transferOut).abs().toDouble()
                else -> it.netBalance.abs().toDouble()
            }
        }
        CardPayloadDto.CategoryBreakdown(
            items = items.map { it.toCategoryBreakdownDto(total, metric) },
            displayCurrency = currency
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
        baseCurrency = this.currency,
        isCumulative = config.isCumulative,
        showGridLines = config.showGridLines,
        showCurrent = config.showCurrentPeriod,
        showPrevious = config.showPreviousPeriod,
        showLastYear = config.showSamePeriodLastYear,
        selectedMetric = config.selectedMetric.name
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
        baseCurrency = this.currency,
        todayBucketIndex = todayBucketIndex,
        isCumulative = config.isCumulative,
        showGridLines = config.showGridLines
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
        baseCurrency = this.currency,
        isCumulative = config.isCumulative,
        showGridLines = config.showGridLines,
        selectedMetric = config.selectedMetric.name,
        selectedNatures = config.selectedNatures.map { it.name },
        activeCategories = activeCategories.map { summary ->
            CategoryBreakdownDto(
                categoryId = summary.categoryId,
                categoryName = summary.name,
                amount = summary.totalAmount.toDouble(),
                percentage = 0.0,
                transactionCount = 0,
                colorHex = "#${(summary.colorHex and 0xFFFFFF).toString(16).padStart(6, '0')}"
            )
        }
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
        baseCurrency = this.currency,
        selectedMetric = config.selectedMetric.name,
        maxTags = config.maxTags,
        maxIntersections = config.maxIntersections
    )

    is CardRenderData.AccountHero -> CardPayloadDto.AccountHero(
        accountId = summary.account.id,
        accountName = summary.account.name,
        currency = summary.account.currencyCode,
        balance = summary.balance.toDouble(),
        balanceInBase = summary.balanceInBase.toDouble(),
        colorHex = summary.account.colorHex,
        iconName = summary.account.icon,
        structureType = summary.account.structureType.name,
        income = summary.currentPeriodFlow.income.toDouble(),
        expense = summary.currentPeriodFlow.expense.toDouble(),
        transferIn = summary.currentPeriodFlow.transferIn.toDouble(),
        transferOut = summary.currentPeriodFlow.transferOut.toDouble(),
        distribution = summary.currencyBalances.mapValues { it.value.toDouble() },
        showCurrencyBadges = config.showCurrencyBadges,
        showComparison = config.showComparison,
        perspective = when {
            config.perspective is AccountingPerspective.Accounting -> "Accounting"
            config.perspective is AccountingPerspective.CashFlow -> "CashFlow"
            else -> "Accounting"
        }
    )

    is CardRenderData.Error -> null
}

internal fun Filter.toDto(): FilterDto {
    val tf = this.filter
    val (timelineType, dateFrom, dateTo) = when (val tl = tf.timelineRange) {
        is TimelineRange.AllTime -> Triple("ALL_TIME", null, null)
        is TimelineRange.CustomRange -> Triple("CUSTOM", tl.start, tl.end)
        is TimelineRange.ThisPeriod -> Triple(tl.period.name, null, null)
        is TimelineRange.LastPeriod -> Triple(tl.period.name, null, null)
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
        excludeFuture = tf.timelineRange.excludeFuture
    )
}

private const val MAX_REGEX_LENGTH = 200

private fun validateRegexOrThrow(pattern: String, fieldName: String) {
    require(pattern.length <= MAX_REGEX_LENGTH) {
        "$fieldName pattern exceeds $MAX_REGEX_LENGTH character limit"
    }
    try {
        Regex(pattern)
    } catch (e: Exception) {
        throw IllegalArgumentException("Invalid $fieldName regex pattern: ${e.message}")
    }
}

internal fun CreateFilterRequest.toTransactionFilter(): TransactionFilter {
    val timelineRange = when (timelineType.uppercase()) {
        "ALL_TIME" -> TimelineRange.AllTime(excludeFuture = excludeFuture)
        "CUSTOM" -> TimelineRange.CustomRange(start = dateFrom, end = dateTo, excludeFuture = excludeFuture)
        "MONTH" -> TimelineRange.ThisPeriod(ThisPeriodType.MONTH, excludeFuture = excludeFuture)
        "YEAR" -> TimelineRange.ThisPeriod(ThisPeriodType.YEAR, excludeFuture = excludeFuture)
        "WEEK" -> TimelineRange.ThisPeriod(ThisPeriodType.WEEK, excludeFuture = excludeFuture)
        "TODAY" -> TimelineRange.ThisPeriod(ThisPeriodType.TODAY, excludeFuture = excludeFuture)
        "QUARTER" -> TimelineRange.ThisPeriod(ThisPeriodType.QUARTER, excludeFuture = excludeFuture)
        else -> TimelineRange.AllTime(excludeFuture = excludeFuture)
    }
    if (isNoteRegex && !queryNote.isNullOrEmpty()) validateRegexOrThrow(queryNote, "note")
    if (isPayeeRegex && !queryPayee.isNullOrEmpty()) validateRegexOrThrow(queryPayee, "payee")

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
        hasWarranty = hasWarranty
    )
}

internal fun Template.toTemplateDto() = TemplateDto(
    id = id,
    name = name,
    categoryId = categoryId,
    amount = amount.toDouble(),
    transactionType = entryMode.name,
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
        "ALL_TIME" -> TimelineRange.AllTime(excludeFuture = excludeFuture)
        "CUSTOM" -> TimelineRange.CustomRange(start = dateFrom, end = dateTo, excludeFuture = excludeFuture)
        "MONTH" -> TimelineRange.ThisPeriod(ThisPeriodType.MONTH, excludeFuture = excludeFuture)
        "YEAR" -> TimelineRange.ThisPeriod(ThisPeriodType.YEAR, excludeFuture = excludeFuture)
        "WEEK" -> TimelineRange.ThisPeriod(ThisPeriodType.WEEK, excludeFuture = excludeFuture)
        "TODAY" -> TimelineRange.ThisPeriod(ThisPeriodType.TODAY, excludeFuture = excludeFuture)
        "QUARTER" -> TimelineRange.ThisPeriod(ThisPeriodType.QUARTER, excludeFuture = excludeFuture)
        "HALF_YEAR" -> TimelineRange.ThisPeriod(ThisPeriodType.HALF_YEAR, excludeFuture = excludeFuture)
        "LAST_7_DAYS" -> TimelineRange.LastPeriod(LastPeriodType.LAST_7_DAYS, excludeFuture = excludeFuture)
        "LAST_30_DAYS" -> TimelineRange.LastPeriod(LastPeriodType.LAST_30_DAYS, excludeFuture = excludeFuture)
        "LAST_6_MONTHS" -> TimelineRange.LastPeriod(LastPeriodType.LAST_6_MONTHS, excludeFuture = excludeFuture)
        "LAST_1_YEAR" -> TimelineRange.LastPeriod(LastPeriodType.LAST_1_YEAR, excludeFuture = excludeFuture)
        else -> TimelineRange.AllTime(excludeFuture = excludeFuture)
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
        maxAmount = maxAmount?.let { BigDecimal.valueOf(it) }
    )
}
