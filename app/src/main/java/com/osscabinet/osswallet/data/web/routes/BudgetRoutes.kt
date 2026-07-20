package com.osscabinet.ossWallet.data.web.routes

import com.osscabinet.ossWallet.data.web.AuthSessionManager
import com.osscabinet.ossWallet.data.web.authenticate
import com.osscabinet.ossWallet.data.web.dto.ApiResponse
import com.osscabinet.ossWallet.data.web.dto.BudgetDto
import com.osscabinet.ossWallet.data.web.dto.CreateBudgetRequest
import com.osscabinet.ossWallet.data.web.dto.UpdateBudgetRequest
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.repository.*
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.domain.service.management.BudgetService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.util.*

fun Route.budgetRoutes(
    authSessionManager: AuthSessionManager,
    budgetRepository: BudgetRepository,
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    settingsRepository: SettingsRepository,
    currencyRepository: CurrencyRepository
) {
    route("/budgets") {
        get {
            if (!authenticate(authSessionManager)) return@get
            try {
                val profileId = settingsRepository.currentProfileId.first()
                val baseCurrency = settingsRepository.currency.first()
                val precision = settingsRepository.decimalPrecision.first()
                val now = System.currentTimeMillis()
                val allTransactions =
                    transactionRepository.getAllTransactionsSync(profileId)
                val includeArchived = call.request.queryParameters["includeArchived"]?.toBoolean() ?: false
                val budgets = if (includeArchived) {
                    budgetRepository.getAllBudgets(profileId).first()
                } else {
                    budgetRepository.getActiveBudgetsSync(profileId)
                }
                val categories = categoryRepository.getAllCategoriesSync()
                val exchangeRates = ExchangeRateSnapshot(
                    anchor = baseCurrency,
                    rates = runCatching {
                        currencyRepository.getExchangeRates(baseCurrency)
                            .mapValues { BigDecimal.valueOf(it.value) }
                    }.getOrDefault(emptyMap())
                )

                val cal = Calendar.getInstance()
                val periodLabel = "${cal.get(Calendar.YEAR)}-${
                    (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
                }"

                val dtos = budgets.map { budget ->
                    val spent = BudgetService.calculateSpent(
                        budget,
                        allTransactions,
                        exchangeRates,
                        baseCurrency,
                        now,
                        categories,
                        precision
                    )
                    val progress = BudgetService.buildProgress(budget, spent)
                    val tlType = when (val tl = budget.timelineRange) {
                        is TimelineRange.ThisPeriod -> when (tl.period) {
                            ThisPeriodType.MONTH -> "MONTHLY"
                            ThisPeriodType.YEAR -> "YEARLY"
                            ThisPeriodType.WEEK -> "WEEKLY"
                            ThisPeriodType.TODAY -> "TODAY"
                            ThisPeriodType.QUARTER -> "QUARTER"
                            ThisPeriodType.HALF_YEAR -> "HALF_YEAR"
                        }
                        else -> "MONTHLY"
                    }
                    BudgetDto(
                        id = budget.id,
                        name = budget.name,
                        currency = budget.currency,
                        limitAmount = budget.limitAmount.toDouble(),
                        spent = progress.spent.toDouble(),
                        remaining = progress.remaining.toDouble(),
                        percentage = progress.percentage.toDouble(),
                        alertLevel = progress.alertLevel.name,
                        period = periodLabel,
                        isArchived = budget.isArchived,
                        alertThreshold = budget.alertThreshold.toDouble(),
                        timelineType = tlType,
                        categoryIds = budget.categoryIds.toList(),
                        accountIds = budget.accountIds.toList(),
                        rollover = budget.rollover
                    )
                }
                call.respond(ApiResponse(success = true, data = dtos))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Unit>(
                        success = false,
                        error = "Internal error"
                    )
                )
            }
        }

        post {
            if (!authenticate(authSessionManager)) return@post
            try {
                val profileId = settingsRepository.currentProfileId.first()
                val req = call.receive<CreateBudgetRequest>()
                val timelineRange: TimelineRange = when (req.timelineType.uppercase()) {
                    "MONTHLY" -> TimelineRange.ThisPeriod(ThisPeriodType.MONTH)
                    "YEARLY" -> TimelineRange.ThisPeriod(ThisPeriodType.YEAR)
                    "WEEKLY" -> TimelineRange.ThisPeriod(ThisPeriodType.WEEK)
                    "TODAY" -> TimelineRange.ThisPeriod(ThisPeriodType.TODAY)
                    "QUARTER" -> TimelineRange.ThisPeriod(ThisPeriodType.QUARTER)
                    else -> TimelineRange.ThisPeriod(ThisPeriodType.MONTH)
                }
                val budget = Budget(
                    name = req.name,
                    currency = req.currency,
                    limitAmount = BigDecimal.valueOf(req.limitAmount),
                    timelineRange = timelineRange,
                    categoryIds = req.categoryIds.toSet(),
                    alertThreshold = req.alertThreshold.toFloat(),
                    rollover = req.rollover,
                    profileId = profileId,
                    accountIds = req.accountIds.toSet(),
                    trackTypes = req.trackTypes.mapNotNull { t ->
                        TransactionType.entries.find {
                            it.name.equals(
                                t,
                                ignoreCase = true
                            )
                        }
                    }.toSet(),
                    filterId = req.filterId
                )
                val newId = budgetRepository.insert(budget)
                call.respond(
                    HttpStatusCode.Created,
                    ApiResponse(success = true, data = mapOf("id" to newId))
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Unit>(
                        success = false,
                        error = "Internal error"
                    )
                )
            }
        }

        get("/{id}") {
            if (!authenticate(authSessionManager)) return@get
            try {
                val profileId = settingsRepository.currentProfileId.first()
                val baseCurrency = settingsRepository.currency.first()
                val precision = settingsRepository.decimalPrecision.first()
                val now = System.currentTimeMillis()
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, error = "Invalid id")
                    )
                val budget = budgetRepository.getById(id, profileId)
                    ?: return@get call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse<Unit>(success = false, error = "Budget not found")
                    )
                val allTransactions =
                    transactionRepository.getAllTransactionsSync(profileId)
                val categories = categoryRepository.getAllCategoriesSync()
                val exchangeRates = ExchangeRateSnapshot(
                    anchor = baseCurrency,
                    rates = runCatching {
                        currencyRepository.getExchangeRates(baseCurrency)
                            .mapValues { BigDecimal.valueOf(it.value) }
                    }.getOrDefault(emptyMap())
                )
                val cal = Calendar.getInstance()
                val periodLabel = "${cal.get(Calendar.YEAR)}-${
                    (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
                }"
                val spent = BudgetService.calculateSpent(
                    budget,
                    allTransactions,
                    exchangeRates,
                    baseCurrency,
                    now,
                    categories,
                    precision
                )
                val progress = BudgetService.buildProgress(budget, spent)
                val tlType = when (val tl = budget.timelineRange) {
                    is TimelineRange.ThisPeriod -> when (tl.period) {
                        ThisPeriodType.MONTH -> "MONTHLY"
                        ThisPeriodType.YEAR -> "YEARLY"
                        ThisPeriodType.WEEK -> "WEEKLY"
                        ThisPeriodType.TODAY -> "TODAY"
                        ThisPeriodType.QUARTER -> "QUARTER"
                        ThisPeriodType.HALF_YEAR -> "HALF_YEAR"
                    }
                    else -> "MONTHLY"
                }
                call.respond(
                    ApiResponse(
                        success = true, data = BudgetDto(
                            id = budget.id,
                            name = budget.name,
                            currency = budget.currency,
                            limitAmount = budget.limitAmount.toDouble(),
                            spent = progress.spent.toDouble(),
                            remaining = progress.remaining.toDouble(),
                            percentage = progress.percentage.toDouble(),
                            alertLevel = progress.alertLevel.name,
                            period = periodLabel,
                            isArchived = budget.isArchived,
                            alertThreshold = budget.alertThreshold.toDouble(),
                            timelineType = tlType,
                            categoryIds = budget.categoryIds.toList(),
                            accountIds = budget.accountIds.toList(),
                            rollover = budget.rollover
                        )
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Unit>(
                        success = false,
                        error = "Internal error"
                    )
                )
            }
        }

        put("/{id}") {
            if (!authenticate(authSessionManager)) return@put
            try {
                val profileId = settingsRepository.currentProfileId.first()
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@put call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, error = "Invalid id")
                    )
                val existing = budgetRepository.getById(id, profileId)
                    ?: return@put call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse<Unit>(success = false, error = "Budget not found")
                    )
                val req = call.receive<UpdateBudgetRequest>()
                val timelineRange = req.timelineType?.let { t ->
                    when (t.uppercase()) {
                        "MONTHLY" -> TimelineRange.ThisPeriod(ThisPeriodType.MONTH)
                        "YEARLY" -> TimelineRange.ThisPeriod(ThisPeriodType.YEAR)
                        "WEEKLY" -> TimelineRange.ThisPeriod(ThisPeriodType.WEEK)
                        "TODAY" -> TimelineRange.ThisPeriod(ThisPeriodType.TODAY)
                        "QUARTER" -> TimelineRange.ThisPeriod(ThisPeriodType.QUARTER)
                        else -> existing.timelineRange
                    }
                } ?: existing.timelineRange
                val updated = existing.copy(
                    name = req.name ?: existing.name,
                    currency = req.currency ?: existing.currency,
                    limitAmount = req.limitAmount?.let { BigDecimal.valueOf(it) }
                        ?: existing.limitAmount,
                    timelineRange = timelineRange,
                    categoryIds = req.categoryIds?.toSet() ?: existing.categoryIds,
                    alertThreshold = req.alertThreshold?.toFloat()
                        ?: existing.alertThreshold,
                    rollover = req.rollover ?: existing.rollover,
                    isArchived = req.isArchived ?: existing.isArchived,
                    accountIds = req.accountIds?.toSet() ?: existing.accountIds,
                    trackTypes = req.trackTypes?.mapNotNull { t ->
                        TransactionType.entries.find {
                            it.name.equals(
                                t,
                                ignoreCase = true
                            )
                        }
                    }?.toSet() ?: existing.trackTypes,
                    filterId = req.filterId ?: existing.filterId
                )
                budgetRepository.update(updated)
                call.respond(ApiResponse(success = true, data = mapOf("updated" to id)))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Unit>(
                        success = false,
                        error = "Internal error"
                    )
                )
            }
        }

        delete("/{id}") {
            if (!authenticate(authSessionManager)) return@delete
            try {
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@delete call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, error = "Invalid id")
                    )
                budgetRepository.delete(id)
                call.respond(ApiResponse(success = true, data = mapOf("deleted" to id)))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Unit>(
                        success = false,
                        error = "Internal error"
                    )
                )
            }
        }
    }
}
