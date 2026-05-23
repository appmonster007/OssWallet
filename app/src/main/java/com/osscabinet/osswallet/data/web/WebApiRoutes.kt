package com.osscabinet.ossWallet.data.web

import android.content.Context
import com.osscabinet.ossWallet.data.web.dto.AccountDto
import com.osscabinet.ossWallet.data.web.dto.ApiResponse
import com.osscabinet.ossWallet.data.web.dto.AuthRequest
import com.osscabinet.ossWallet.data.web.dto.AuthResponse
import com.osscabinet.ossWallet.data.web.dto.BudgetDto
import com.osscabinet.ossWallet.data.web.dto.CashFlowPointDto
import com.osscabinet.ossWallet.data.web.dto.CategoryBreakdownDto
import com.osscabinet.ossWallet.data.web.dto.CategoryDto
import com.osscabinet.ossWallet.data.web.dto.CreateBudgetRequest
import com.osscabinet.ossWallet.data.web.dto.CreateCategoryRequest
import com.osscabinet.ossWallet.data.web.dto.CreateLoanRequest
import com.osscabinet.ossWallet.data.web.dto.CreatePlannedPaymentRequest
import com.osscabinet.ossWallet.data.web.dto.CreateTransactionRequest
import com.osscabinet.ossWallet.data.web.dto.DashboardDto
import com.osscabinet.ossWallet.data.web.dto.LoanDto
import com.osscabinet.ossWallet.data.web.dto.MonthlySummaryDto
import com.osscabinet.ossWallet.data.web.dto.PagedResponse
import com.osscabinet.ossWallet.data.web.dto.PlannedPaymentDto
import com.osscabinet.ossWallet.data.web.dto.SettingsDto
import com.osscabinet.ossWallet.data.web.dto.TagDto
import com.osscabinet.ossWallet.data.web.dto.TransactionDto
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.CategoryNature
import com.osscabinet.ossWallet.domain.model.Loan
import com.osscabinet.ossWallet.domain.model.LoanType
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.PaymentSchedule
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
import com.osscabinet.ossWallet.domain.model.RecurrenceRule
import com.osscabinet.ossWallet.domain.model.RecurrenceType
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.BudgetRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.CurrencyRepository
import com.osscabinet.ossWallet.domain.repository.LoanRepository
import com.osscabinet.ossWallet.domain.repository.PlannedPaymentRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TagRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import com.osscabinet.ossWallet.domain.service.filter.TransactionFilterService
import com.osscabinet.ossWallet.domain.service.management.BudgetService
import com.osscabinet.ossWallet.domain.service.management.LoanService
import com.osscabinet.ossWallet.domain.service.management.PlannedPaymentService
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class WebApiRoutes(
    private val context: Context,
    private val authSessionManager: AuthSessionManager,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository,
    private val budgetRepository: BudgetRepository,
    private val loanRepository: LoanRepository,
    private val plannedPaymentRepository: PlannedPaymentRepository,
    private val tagRepository: TagRepository,
    private val currencyRepository: CurrencyRepository
) {
    fun Application.installRoutes() {
        routing {
            get("/") {
                call.respondText(loadIndexHtml(), ContentType.Text.Html)
            }

            get("/api/health") {
                call.respond(ApiResponse(success = true, data = mapOf("status" to "ok")))
            }

            post("/api/auth") {
                val remoteIp = call.request.local.remoteHost
                val request = runCatching { call.receive<AuthRequest>() }.getOrNull()
                    ?: run {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<Unit>(success = false, error = "Invalid request body")
                        )
                        return@post
                    }

                try {
                    val token = authSessionManager.authenticate(request.accessCode, remoteIp)
                    if (token != null) {
                        call.respond(ApiResponse(success = true, data = AuthResponse(token = token)))
                    } else {
                        call.respond(
                            HttpStatusCode.Unauthorized,
                            ApiResponse<Unit>(success = false, error = "Invalid access code")
                        )
                    }
                } catch (e: AuthSessionManager.RateLimitedException) {
                    call.response.headers.append("Retry-After", e.retryAfterSeconds.toString())
                    call.respond(
                        HttpStatusCode.TooManyRequests,
                        ApiResponse<Unit>(
                            success = false,
                            error = "Too many attempts. Try again in ${e.retryAfterSeconds}s."
                        )
                    )
                }
            }

            route("/api") {

                // ── Accounts ──────────────────────────────────────────────────────────
                get("/accounts") {
                    if (!authenticate(authSessionManager)) return@get
                    try {
                        val profileId = settingsRepository.currentProfileId.first()
                        val allTransactions = transactionRepository.getAllTransactionsSync(profileId)
                        val accounts = accountRepository.getAllAccountsSync(profileId)

                        val dtos = accounts.map { account ->
                            val balance = allTransactions
                                .filter { it.accountId == account.id }
                                .fold(BigDecimal.ZERO) { acc, tx ->
                                    when (tx.transactionType) {
                                        TransactionType.INCOME -> acc + tx.amount
                                        TransactionType.EXPENSE -> acc - tx.amount
                                        TransactionType.TRANSFER -> acc - tx.amount
                                    }
                                }
                            AccountDto(
                                id = account.id,
                                name = account.name,
                                currency = account.currencyCode,
                                balance = balance.toDouble(),
                                type = account.structureType.name
                            )
                        }
                        call.respond(ApiResponse(success = true, data = dtos))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(success = false, error = e.message ?: "Internal error")
                        )
                    }
                }

                // ── Transactions ──────────────────────────────────────────────────────
                get("/transactions") {
                    if (!authenticate(authSessionManager)) return@get
                    try {
                        val profileId = settingsRepository.currentProfileId.first()
                        val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(0) ?: 0
                        val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 100) ?: 25
                        val accountFilter = call.request.queryParameters["accountId"]
                        val typeParam = call.request.queryParameters["type"]
                        val dateFrom = call.request.queryParameters["dateFrom"]?.toLongOrNull()
                        val dateTo = call.request.queryParameters["dateTo"]?.toLongOrNull()
                        val categoryIdParam = call.request.queryParameters["categoryId"]?.toLongOrNull()
                        val tagNamesParam = call.request.queryParameters["tagNames"]
                        val minAmountParam = call.request.queryParameters["minAmount"]?.toDoubleOrNull()
                        val maxAmountParam = call.request.queryParameters["maxAmount"]?.toDoubleOrNull()
                        val currencyParam = call.request.queryParameters["currency"]
                        val statusParam = call.request.queryParameters["status"]
                        val payeeParam = call.request.queryParameters["payee"]

                        val allTransactions = transactionRepository.getAllTransactionsSync(profileId)
                        val categoryMap = categoryRepository.getAllCategoriesSync().associateBy { it.id }
                        val accountMap = accountRepository.getAllAccountsSync(profileId).associateBy { it.id }

                        // Build filter from query params
                        val timelineRange = if (dateFrom != null || dateTo != null) {
                            TimelineRange.CustomRange(start = dateFrom, end = dateTo)
                        } else {
                            TimelineRange.AllTime
                        }

                        val filter = TransactionFilter(
                            timelineRange = timelineRange,
                            accountIds = if (accountFilter != null) setOf(accountFilter) else emptySet(),
                            transactionTypes = if (typeParam != null) {
                                setOf(TransactionType.entries.find { it.name.equals(typeParam, ignoreCase = true) } ?: TransactionType.EXPENSE)
                            } else emptySet(),
                            categoryIds = if (categoryIdParam != null) setOf(categoryIdParam) else emptySet(),
                            tags = if (!tagNamesParam.isNullOrBlank()) tagNamesParam.split(",").map { it.trim() }.toSet() else emptySet(),
                            minAmount = if (minAmountParam != null) BigDecimal.valueOf(minAmountParam) else null,
                            maxAmount = if (maxAmountParam != null) BigDecimal.valueOf(maxAmountParam) else null,
                            currencies = if (!currencyParam.isNullOrBlank()) setOf(currencyParam) else emptySet(),
                            statuses = if (statusParam != null) {
                                setOf(TransactionStatus.entries.find { it.name.equals(statusParam, ignoreCase = true) } ?: TransactionStatus.CLEARED)
                            } else emptySet(),
                            queryPayee = payeeParam,
                            excludeFuture = false
                        )

                        val filtered = TransactionFilterService.filterTransactions(allTransactions, filter)
                        val sorted = filtered.sortedByDescending { it.timestamp }
                        val paged = sorted.drop(page * limit).take(limit)

                        val dtos = paged.map { tx ->
                            TransactionDto(
                                id = tx.id,
                                title = tx.name,
                                amount = tx.amount.toDouble(),
                                currency = tx.currency,
                                type = tx.transactionType.name,
                                categoryName = categoryMap[tx.categoryId]?.name,
                                accountName = accountMap[tx.accountId]?.name ?: "",
                                date = tx.timestamp,
                                note = tx.note.takeIf { it.isNotBlank() }
                            )
                        }
                        call.respond(
                            ApiResponse(
                                success = true,
                                data = PagedResponse(
                                    items = dtos,
                                    total = filtered.size,
                                    page = page,
                                    limit = limit,
                                    hasMore = page * limit + limit < filtered.size
                                )
                            )
                        )
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(success = false, error = e.message ?: "Internal error")
                        )
                    }
                }

                post("/transactions") {
                    if (!authenticate(authSessionManager)) return@post
                    try {
                        val profileId = settingsRepository.currentProfileId.first()
                        val req = call.receive<CreateTransactionRequest>()
                        val tx = Transaction(
                            name = req.name,
                            amount = BigDecimal.valueOf(req.amount),
                            timestamp = req.timestamp,
                            currency = req.currency,
                            transactionType = TransactionType.entries.find { it.name.equals(req.transactionType, ignoreCase = true) }
                                ?: TransactionType.EXPENSE,
                            accountId = req.accountId,
                            categoryId = req.categoryId,
                            tags = req.tags.toSet(),
                            paymentMethod = PaymentMethod.entries.find { it.name.equals(req.paymentMethod, ignoreCase = true) }
                                ?: PaymentMethod.CASH,
                            payee = req.payee,
                            note = req.note,
                            status = TransactionStatus.entries.find { it.name.equals(req.status, ignoreCase = true) }
                                ?: TransactionStatus.CLEARED
                        )
                        val newId = transactionRepository.insertTransaction(tx, profileId)
                        val categoryMap = categoryRepository.getAllCategoriesSync().associateBy { it.id }
                        val accountMap = accountRepository.getAllAccountsSync(profileId).associateBy { it.id }
                        val created = tx.copy(id = newId)
                        val dto = TransactionDto(
                            id = created.id,
                            title = created.name,
                            amount = created.amount.toDouble(),
                            currency = created.currency,
                            type = created.transactionType.name,
                            categoryName = categoryMap[created.categoryId]?.name,
                            accountName = accountMap[created.accountId]?.name ?: "",
                            date = created.timestamp,
                            note = created.note.takeIf { it.isNotBlank() }
                        )
                        call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = dto))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(success = false, error = e.message ?: "Internal error")
                        )
                    }
                }

                delete("/transactions/{id}") {
                    if (!authenticate(authSessionManager)) return@delete
                    try {
                        val id = call.parameters["id"]?.toLongOrNull()
                            ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Invalid id"))
                        transactionRepository.deleteTransaction(id)
                        call.respond(ApiResponse(success = true, data = mapOf("deleted" to id)))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(success = false, error = e.message ?: "Internal error")
                        )
                    }
                }

                // ── Summary ───────────────────────────────────────────────────────────
                get("/summary") {
                    if (!authenticate(authSessionManager)) return@get
                    try {
                        val profileId = settingsRepository.currentProfileId.first()
                        val allTransactions = transactionRepository.getAllTransactionsSync(profileId)
                        val accounts = accountRepository.getAllAccountsSync(profileId)

                        val cal = Calendar.getInstance()
                        val year = cal.get(Calendar.YEAR)
                        val month = cal.get(Calendar.MONTH) + 1

                        val monthlyTx = allTransactions.filter { tx ->
                            val txCal = Calendar.getInstance().also { it.timeInMillis = tx.timestamp }
                            txCal.get(Calendar.YEAR) == year && txCal.get(Calendar.MONTH) + 1 == month
                        }

                        val netWorth = accounts.sumOf { account ->
                            allTransactions
                                .filter { it.accountId == account.id }
                                .fold(BigDecimal.ZERO) { acc, tx ->
                                    when (tx.transactionType) {
                                        TransactionType.INCOME -> acc + tx.amount
                                        TransactionType.EXPENSE -> acc - tx.amount
                                        TransactionType.TRANSFER -> acc - tx.amount
                                    }
                                }.toDouble()
                        }

                        val income = monthlyTx
                            .filter { it.transactionType == TransactionType.INCOME }
                            .sumOf { it.amount.toDouble() }
                        val expenses = monthlyTx
                            .filter { it.transactionType == TransactionType.EXPENSE }
                            .sumOf { it.amount.toDouble() }

                        call.respond(
                            ApiResponse(
                                success = true,
                                data = MonthlySummaryDto(
                                    netWorth = netWorth,
                                    monthlyIncome = income,
                                    monthlyExpenses = expenses,
                                    month = "$year-${month.toString().padStart(2, '0')}"
                                )
                            )
                        )
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(success = false, error = e.message ?: "Internal error")
                        )
                    }
                }

                // ── Budgets ───────────────────────────────────────────────────────────
                get("/budgets") {
                    if (!authenticate(authSessionManager)) return@get
                    try {
                        val profileId = settingsRepository.currentProfileId.first()
                        val baseCurrency = settingsRepository.currency.first()
                        val precision = settingsRepository.decimalPrecision.first()
                        val now = System.currentTimeMillis()
                        val allTransactions = transactionRepository.getAllTransactionsSync(profileId)
                        val budgets = budgetRepository.getActiveBudgetsSync(profileId)
                        val categories = categoryRepository.getAllCategoriesSync()
                        val exchangeRates = runCatching {
                            currencyRepository.getExchangeRates(baseCurrency)
                                .mapValues { BigDecimal.valueOf(it.value) }
                        }.getOrDefault(emptyMap())

                        val cal = Calendar.getInstance()
                        val periodLabel = "${cal.get(Calendar.YEAR)}-${(cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')}"

                        val dtos = budgets.map { budget ->
                            val spent = BudgetService.calculateSpent(budget, allTransactions, exchangeRates, baseCurrency, now, categories, precision)
                            val progress = BudgetService.buildProgress(budget, spent)
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
                                isArchived = budget.isArchived
                            )
                        }
                        call.respond(ApiResponse(success = true, data = dtos))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(success = false, error = e.message ?: "Internal error")
                        )
                    }
                }

                post("/budgets") {
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
                            profileId = profileId
                        )
                        val newId = budgetRepository.insert(budget)
                        call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = mapOf("id" to newId)))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(success = false, error = e.message ?: "Internal error")
                        )
                    }
                }

                delete("/budgets/{id}") {
                    if (!authenticate(authSessionManager)) return@delete
                    try {
                        val id = call.parameters["id"]?.toLongOrNull()
                            ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Invalid id"))
                        budgetRepository.delete(id)
                        call.respond(ApiResponse(success = true, data = mapOf("deleted" to id)))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(success = false, error = e.message ?: "Internal error")
                        )
                    }
                }

                // ── Loans ─────────────────────────────────────────────────────────────
                get("/loans") {
                    if (!authenticate(authSessionManager)) return@get
                    try {
                        val profileId = settingsRepository.currentProfileId.first()
                        val now = System.currentTimeMillis()
                        val precision = settingsRepository.decimalPrecision.first()
                        val loans = loanRepository.getActiveLoansSync(profileId)

                        val dtos = loans.map { loan ->
                            val payments = loanRepository.getPaymentsForLoanSync(loan.id)
                            val summary = LoanService.buildSummary(loan, payments, now, precision)
                            LoanDto(
                                id = loan.id,
                                name = loan.name,
                                loanType = loan.loanType.name,
                                currency = loan.currency,
                                principalAmount = loan.principalAmount.toDouble(),
                                totalPaid = summary.totalPaid.toDouble(),
                                remainingBalance = summary.remainingBalance.toDouble(),
                                interestAccrued = summary.interestAccrued.toDouble(),
                                status = loan.status.name,
                                lenderOrBorrowerName = loan.lenderOrBorrowerName,
                                startDate = loan.startDate,
                                expectedSettlementDate = loan.expectedSettlementDate
                            )
                        }
                        call.respond(ApiResponse(success = true, data = dtos))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(success = false, error = e.message ?: "Internal error")
                        )
                    }
                }

                post("/loans") {
                    if (!authenticate(authSessionManager)) return@post
                    try {
                        val profileId = settingsRepository.currentProfileId.first()
                        val req = call.receive<CreateLoanRequest>()
                        val loan = Loan(
                            name = req.name,
                            principalAmount = BigDecimal.valueOf(req.principalAmount),
                            currency = req.currency,
                            loanType = LoanType.entries.find { it.name.equals(req.loanType, ignoreCase = true) }
                                ?: LoanType.BORROWED,
                            lenderOrBorrowerName = req.lenderOrBorrowerName,
                            startDate = req.startDate,
                            expectedSettlementDate = req.expectedSettlementDate,
                            interestRate = req.interestRate?.toFloat(),
                            note = req.note,
                            linkedAccountId = "",
                            profileId = profileId
                        )
                        val newId = loanRepository.insertLoan(loan)
                        call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = mapOf("id" to newId)))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(success = false, error = e.message ?: "Internal error")
                        )
                    }
                }

                delete("/loans/{id}") {
                    if (!authenticate(authSessionManager)) return@delete
                    try {
                        val id = call.parameters["id"]?.toLongOrNull()
                            ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Invalid id"))
                        loanRepository.deleteLoan(id)
                        call.respond(ApiResponse(success = true, data = mapOf("deleted" to id)))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(success = false, error = e.message ?: "Internal error")
                        )
                    }
                }

                // ── Planned Payments ──────────────────────────────────────────────────
                get("/planned-payments") {
                    if (!authenticate(authSessionManager)) return@get
                    try {
                        val profileId = settingsRepository.currentProfileId.first()
                        val now = System.currentTimeMillis()
                        val payments = plannedPaymentRepository.getAllSync(profileId)
                        val categoryMap = categoryRepository.getAllCategoriesSync().associateBy { it.id }
                        val accountMap = accountRepository.getAllAccountsSync(profileId).associateBy { it.id }

                        val dtos = payments.map { payment ->
                            payment.toDto(now, categoryMap, accountMap)
                        }
                        call.respond(ApiResponse(success = true, data = dtos))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(success = false, error = e.message ?: "Internal error")
                        )
                    }
                }

                get("/planned-payments/upcoming") {
                    if (!authenticate(authSessionManager)) return@get
                    try {
                        val profileId = settingsRepository.currentProfileId.first()
                        val now = System.currentTimeMillis()
                        val days = call.request.queryParameters["days"]?.toIntOrNull() ?: 30
                        val allPayments = plannedPaymentRepository.getAllSync(profileId)
                        val categoryMap = categoryRepository.getAllCategoriesSync().associateBy { it.id }
                        val accountMap = accountRepository.getAllAccountsSync(profileId).associateBy { it.id }

                        val upcoming = PlannedPaymentService.generateUpcoming(allPayments, now, days)
                        val dtos = upcoming.map { item ->
                            item.plannedPayment.toDto(now, categoryMap, accountMap)
                        }
                        call.respond(ApiResponse(success = true, data = dtos))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(success = false, error = e.message ?: "Internal error")
                        )
                    }
                }

                get("/planned-payments/overdue") {
                    if (!authenticate(authSessionManager)) return@get
                    try {
                        val profileId = settingsRepository.currentProfileId.first()
                        val now = System.currentTimeMillis()
                        val allPayments = plannedPaymentRepository.getAllSync(profileId)
                        val categoryMap = categoryRepository.getAllCategoriesSync().associateBy { it.id }
                        val accountMap = accountRepository.getAllAccountsSync(profileId).associateBy { it.id }

                        val overdue = PlannedPaymentService.getOverdue(allPayments, now)
                        val dtos = overdue.map { payment ->
                            payment.toDto(now, categoryMap, accountMap)
                        }
                        call.respond(ApiResponse(success = true, data = dtos))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(success = false, error = e.message ?: "Internal error")
                        )
                    }
                }

                post("/planned-payments") {
                    if (!authenticate(authSessionManager)) return@post
                    try {
                        val profileId = settingsRepository.currentProfileId.first()
                        val req = call.receive<CreatePlannedPaymentRequest>()
                        val recurrenceType = RecurrenceType.entries.find { it.name.equals(req.recurrenceType, ignoreCase = true) }
                            ?: RecurrenceType.MONTHLY
                        val rule = RecurrenceRule(type = recurrenceType, interval = req.recurrenceInterval)
                        val schedule = PaymentSchedule(
                            recurrenceRule = rule,
                            startDate = req.startDate,
                            nextDueDate = req.startDate
                        )
                        val payment = PlannedPayment(
                            name = req.name,
                            amount = BigDecimal.valueOf(req.amount),
                            currency = req.currency,
                            transactionType = TransactionType.entries.find { it.name.equals(req.transactionType, ignoreCase = true) }
                                ?: TransactionType.EXPENSE,
                            categoryId = req.categoryId,
                            accountId = req.accountId,
                            note = req.note,
                            schedule = schedule,
                            autoConfirm = req.autoConfirm,
                            profileId = profileId
                        )
                        val newId = plannedPaymentRepository.insert(payment)
                        call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = mapOf("id" to newId)))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(success = false, error = e.message ?: "Internal error")
                        )
                    }
                }

                delete("/planned-payments/{id}") {
                    if (!authenticate(authSessionManager)) return@delete
                    try {
                        val id = call.parameters["id"]?.toLongOrNull()
                            ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Invalid id"))
                        plannedPaymentRepository.delete(id)
                        call.respond(ApiResponse(success = true, data = mapOf("deleted" to id)))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(success = false, error = e.message ?: "Internal error")
                        )
                    }
                }

                post("/planned-payments/{id}/confirm") {
                    if (!authenticate(authSessionManager)) return@post
                    try {
                        val profileId = settingsRepository.currentProfileId.first()
                        val id = call.parameters["id"]?.toLongOrNull()
                            ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Invalid id"))
                        val payment = plannedPaymentRepository.getById(id)
                            ?: return@post call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, error = "Payment not found"))
                        val now = System.currentTimeMillis()

                        // Create transaction from the planned payment
                        val tx = Transaction(
                            name = payment.name,
                            amount = payment.amount,
                            timestamp = now,
                            currency = payment.currency,
                            transactionType = payment.transactionType,
                            accountId = payment.accountId,
                            categoryId = payment.categoryId,
                            tags = payment.tags,
                            paymentMethod = payment.paymentMethod ?: PaymentMethod.CASH,
                            payee = payment.payee ?: "",
                            note = payment.note ?: "",
                            status = payment.transactionStatus,
                            sourcePlannedPaymentId = payment.id
                        )
                        transactionRepository.insertTransaction(tx, profileId)

                        // Advance next due date
                        val nextDue = PlannedPaymentService.calculateNextDueDate(payment, now)
                        val updatedPayment = if (nextDue != null) {
                            payment.copy(schedule = payment.schedule.copy(nextDueDate = nextDue))
                        } else {
                            payment.copy(status = PlannedPaymentStatus.COMPLETED)
                        }
                        plannedPaymentRepository.update(updatedPayment)
                        call.respond(ApiResponse(success = true, data = mapOf("confirmed" to id)))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(success = false, error = e.message ?: "Internal error")
                        )
                    }
                }

                post("/planned-payments/{id}/skip") {
                    if (!authenticate(authSessionManager)) return@post
                    try {
                        val id = call.parameters["id"]?.toLongOrNull()
                            ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Invalid id"))
                        val payment = plannedPaymentRepository.getById(id)
                            ?: return@post call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, error = "Payment not found"))
                        val now = System.currentTimeMillis()
                        val nextDue = PlannedPaymentService.calculateNextDueDate(payment, now)
                        val updatedPayment = if (nextDue != null) {
                            payment.copy(schedule = payment.schedule.copy(nextDueDate = nextDue))
                        } else {
                            payment.copy(status = PlannedPaymentStatus.COMPLETED)
                        }
                        plannedPaymentRepository.update(updatedPayment)
                        call.respond(ApiResponse(success = true, data = mapOf("skipped" to id)))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(success = false, error = e.message ?: "Internal error")
                        )
                    }
                }

                // ── Categories ────────────────────────────────────────────────────────
                get("/categories") {
                    if (!authenticate(authSessionManager)) return@get
                    try {
                        val categories = categoryRepository.getAllCategoriesSync()
                        val dtos = categories.map { it.toDto() }
                        call.respond(ApiResponse(success = true, data = dtos))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(success = false, error = e.message ?: "Internal error")
                        )
                    }
                }

                post("/categories") {
                    if (!authenticate(authSessionManager)) return@post
                    try {
                        val req = call.receive<CreateCategoryRequest>()
                        val nature = CategoryNature.entries.find { it.name.equals(req.nature, ignoreCase = true) }
                            ?: CategoryNature.NEED
                        val category = Category(
                            id = 0,
                            name = req.name,
                            iconName = req.iconName,
                            colorHex = req.colorHex,
                            isDefault = false,
                            parentId = req.parentId,
                            nature = nature
                        )
                        categoryRepository.upsertCategory(category)
                        call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = category.toDto()))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(success = false, error = e.message ?: "Internal error")
                        )
                    }
                }

                delete("/categories/{id}") {
                    if (!authenticate(authSessionManager)) return@delete
                    try {
                        val id = call.parameters["id"]?.toLongOrNull()
                            ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Invalid id"))
                        val category = categoryRepository.getAllCategoriesSync().find { it.id == id }
                            ?: return@delete call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, error = "Category not found"))
                        categoryRepository.deleteCategory(category)
                        call.respond(ApiResponse(success = true, data = mapOf("deleted" to id)))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(success = false, error = e.message ?: "Internal error")
                        )
                    }
                }

                // ── Tags ──────────────────────────────────────────────────────────────
                get("/tags") {
                    if (!authenticate(authSessionManager)) return@get
                    try {
                        val profileId = settingsRepository.currentProfileId.first()
                        val allTags = tagRepository.getAllTags().first()
                        val allTransactions = transactionRepository.getAllTransactionsSync(profileId)

                        val dtos = allTags.map { tag ->
                            val count = allTransactions.count { tx -> tag.name in tx.tags }
                            TagDto(
                                name = tag.name,
                                colorHex = tag.colorHex,
                                transactionCount = count
                            )
                        }
                        call.respond(ApiResponse(success = true, data = dtos))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(success = false, error = e.message ?: "Internal error")
                        )
                    }
                }

                delete("/tags/{name}") {
                    if (!authenticate(authSessionManager)) return@delete
                    try {
                        val name = call.parameters["name"]
                            ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Invalid name"))
                        val allTags = tagRepository.getAllTags().first()
                        val tag = allTags.find { it.name == name }
                            ?: return@delete call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, error = "Tag not found"))
                        tagRepository.deleteTagAndRemoveFromTransactions(tag)
                        call.respond(ApiResponse(success = true, data = mapOf("deleted" to name)))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(success = false, error = e.message ?: "Internal error")
                        )
                    }
                }

                // ── Analytics ─────────────────────────────────────────────────────────
                get("/analytics/category-breakdown") {
                    if (!authenticate(authSessionManager)) return@get
                    try {
                        val profileId = settingsRepository.currentProfileId.first()
                        val dateFrom = call.request.queryParameters["dateFrom"]?.toLongOrNull()
                        val dateTo = call.request.queryParameters["dateTo"]?.toLongOrNull()
                        val typeParam = call.request.queryParameters["type"] ?: "EXPENSE"

                        val txType = TransactionType.entries.find { it.name.equals(typeParam, ignoreCase = true) }
                            ?: TransactionType.EXPENSE

                        val allTransactions = transactionRepository.getAllTransactionsSync(profileId)
                        val categoryMap = categoryRepository.getAllCategoriesSync().associateBy { it.id }

                        val filtered = allTransactions.filter { tx ->
                            tx.transactionType == txType &&
                                (dateFrom == null || tx.timestamp >= dateFrom) &&
                                (dateTo == null || tx.timestamp <= dateTo)
                        }

                        val grouped = filtered.groupBy { it.categoryId }
                        val totalAmount = filtered.sumOf { it.amount.toDouble() }

                        val dtos = grouped.map { (catId, txs) ->
                            val amount = txs.sumOf { it.amount.toDouble() }
                            val pct = if (totalAmount > 0.0) (amount / totalAmount) * 100.0 else 0.0
                            CategoryBreakdownDto(
                                categoryId = catId,
                                categoryName = categoryMap[catId]?.name ?: "Unknown",
                                amount = amount,
                                percentage = pct.toBigDecimal().setScale(2, RoundingMode.HALF_UP).toDouble(),
                                transactionCount = txs.size
                            )
                        }.sortedByDescending { it.amount }

                        call.respond(ApiResponse(success = true, data = dtos))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(success = false, error = e.message ?: "Internal error")
                        )
                    }
                }

                get("/analytics/cash-flow") {
                    if (!authenticate(authSessionManager)) return@get
                    try {
                        val profileId = settingsRepository.currentProfileId.first()
                        val dateFrom = call.request.queryParameters["dateFrom"]?.toLongOrNull()
                        val dateTo = call.request.queryParameters["dateTo"]?.toLongOrNull()
                        val granularity = call.request.queryParameters["granularity"]?.uppercase() ?: "MONTH"

                        val allTransactions = transactionRepository.getAllTransactionsSync(profileId)

                        val filtered = allTransactions.filter { tx ->
                            (dateFrom == null || tx.timestamp >= dateFrom) &&
                                (dateTo == null || tx.timestamp <= dateTo)
                        }

                        // Group by granularity bucket
                        data class Bucket(val label: String, val epochMillis: Long)

                        fun getBucket(timestamp: Long): Bucket {
                            val cal = Calendar.getInstance().also { it.timeInMillis = timestamp }
                            return when (granularity) {
                                "DAY" -> {
                                    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                    Bucket(fmt.format(cal.time), cal.timeInMillis)
                                }
                                "WEEK" -> {
                                    val week = cal.get(Calendar.WEEK_OF_YEAR)
                                    val year = cal.get(Calendar.YEAR)
                                    Bucket("$year-W${week.toString().padStart(2, '0')}", cal.timeInMillis)
                                }
                                else -> { // MONTH
                                    val month = cal.get(Calendar.MONTH) + 1
                                    val year = cal.get(Calendar.YEAR)
                                    Bucket("$year-${month.toString().padStart(2, '0')}", cal.timeInMillis)
                                }
                            }
                        }

                        // Accumulate per bucket
                        val bucketMap = linkedMapOf<String, Triple<Long, Double, Double>>() // label -> (epoch, income, expense)
                        filtered.sortedBy { it.timestamp }.forEach { tx ->
                            val bucket = getBucket(tx.timestamp)
                            val current = bucketMap.getOrDefault(bucket.label, Triple(bucket.epochMillis, 0.0, 0.0))
                            bucketMap[bucket.label] = when (tx.transactionType) {
                                TransactionType.INCOME -> current.copy(second = current.second + tx.amount.toDouble())
                                TransactionType.EXPENSE -> current.copy(third = current.third + tx.amount.toDouble())
                                else -> current
                            }
                        }

                        val dtos = bucketMap.map { (label, triple) ->
                            val (epoch, income, expense) = triple
                            CashFlowPointDto(
                                label = label,
                                epochMillis = epoch,
                                income = income,
                                expense = expense,
                                net = income - expense
                            )
                        }

                        call.respond(ApiResponse(success = true, data = dtos))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(success = false, error = e.message ?: "Internal error")
                        )
                    }
                }

                // ── Dashboard ─────────────────────────────────────────────────────────
                get("/dashboard") {
                    if (!authenticate(authSessionManager)) return@get
                    try {
                        val profileId = settingsRepository.currentProfileId.first()
                        val baseCurrency = settingsRepository.currency.first()
                        val precision = settingsRepository.decimalPrecision.first()
                        val now = System.currentTimeMillis()

                        val allTransactions = transactionRepository.getAllTransactionsSync(profileId)
                        val accounts = accountRepository.getAllAccountsSync(profileId)
                        val categories = categoryRepository.getAllCategoriesSync()
                        val categoryMap = categories.associateBy { it.id }
                        val accountMap = accounts.associateBy { it.id }

                        val exchangeRates = runCatching {
                            currencyRepository.getExchangeRates(baseCurrency)
                                .mapValues { BigDecimal.valueOf(it.value) }
                        }.getOrDefault(emptyMap())

                        // Net worth
                        val netWorth = accounts.sumOf { account ->
                            allTransactions
                                .filter { it.accountId == account.id }
                                .fold(BigDecimal.ZERO) { acc, tx ->
                                    when (tx.transactionType) {
                                        TransactionType.INCOME -> acc + tx.amount
                                        TransactionType.EXPENSE -> acc - tx.amount
                                        TransactionType.TRANSFER -> acc - tx.amount
                                    }
                                }.toDouble()
                        }

                        // Current month
                        val cal = Calendar.getInstance()
                        val year = cal.get(Calendar.YEAR)
                        val month = cal.get(Calendar.MONTH) + 1
                        val monthLabel = "$year-${month.toString().padStart(2, '0')}"

                        // Month boundaries
                        val monthStart = Calendar.getInstance().also {
                            it.set(year, month - 1, 1, 0, 0, 0)
                            it.set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        val monthEnd = Calendar.getInstance().also {
                            it.set(year, month - 1, cal.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
                            it.set(Calendar.MILLISECOND, 999)
                        }.timeInMillis

                        val monthlyTx = allTransactions.filter { it.timestamp in monthStart..monthEnd }
                        val monthlyIncome = monthlyTx.filter { it.transactionType == TransactionType.INCOME }.sumOf { it.amount.toDouble() }
                        val monthlyExpenses = monthlyTx.filter { it.transactionType == TransactionType.EXPENSE }.sumOf { it.amount.toDouble() }

                        // Account DTOs
                        val accountDtos = accounts.map { account ->
                            val balance = allTransactions
                                .filter { it.accountId == account.id }
                                .fold(BigDecimal.ZERO) { acc, tx ->
                                    when (tx.transactionType) {
                                        TransactionType.INCOME -> acc + tx.amount
                                        TransactionType.EXPENSE -> acc - tx.amount
                                        TransactionType.TRANSFER -> acc - tx.amount
                                    }
                                }
                            AccountDto(id = account.id, name = account.name, currency = account.currencyCode, balance = balance.toDouble(), type = account.structureType.name)
                        }

                        // Budget DTOs (first 5)
                        val budgets = budgetRepository.getActiveBudgetsSync(profileId)
                        val budgetDtos = budgets.take(5).map { budget ->
                            val spent = BudgetService.calculateSpent(budget, allTransactions, exchangeRates, baseCurrency, now, categories, precision)
                            val progress = BudgetService.buildProgress(budget, spent)
                            BudgetDto(
                                id = budget.id, name = budget.name, currency = budget.currency,
                                limitAmount = budget.limitAmount.toDouble(), spent = progress.spent.toDouble(),
                                remaining = progress.remaining.toDouble(), percentage = progress.percentage.toDouble(),
                                alertLevel = progress.alertLevel.name, period = monthLabel, isArchived = budget.isArchived
                            )
                        }

                        // Loan DTOs (first 5)
                        val loans = loanRepository.getActiveLoansSync(profileId)
                        val loanDtos = loans.take(5).map { loan ->
                            val payments = loanRepository.getPaymentsForLoanSync(loan.id)
                            val summary = LoanService.buildSummary(loan, payments, now, precision)
                            LoanDto(
                                id = loan.id, name = loan.name, loanType = loan.loanType.name,
                                currency = loan.currency, principalAmount = loan.principalAmount.toDouble(),
                                totalPaid = summary.totalPaid.toDouble(), remainingBalance = summary.remainingBalance.toDouble(),
                                interestAccrued = summary.interestAccrued.toDouble(), status = loan.status.name,
                                lenderOrBorrowerName = loan.lenderOrBorrowerName,
                                startDate = loan.startDate, expectedSettlementDate = loan.expectedSettlementDate
                            )
                        }

                        // Planned payments
                        val allPayments = plannedPaymentRepository.getAllSync(profileId)
                        val upcoming14 = PlannedPaymentService.generateUpcoming(allPayments, now, 14)
                            .map { it.plannedPayment.toDto(now, categoryMap, accountMap) }
                        val overduePayments = PlannedPaymentService.getOverdue(allPayments, now)
                            .map { it.toDto(now, categoryMap, accountMap) }

                        // Recent transactions (last 10)
                        val recentTx = allTransactions.sortedByDescending { it.timestamp }.take(10)
                            .map { tx ->
                                TransactionDto(
                                    id = tx.id, title = tx.name, amount = tx.amount.toDouble(),
                                    currency = tx.currency, type = tx.transactionType.name,
                                    categoryName = categoryMap[tx.categoryId]?.name,
                                    accountName = accountMap[tx.accountId]?.name ?: "",
                                    date = tx.timestamp, note = tx.note.takeIf { it.isNotBlank() }
                                )
                            }

                        // Category breakdown (current month expenses)
                        val monthExpenses = monthlyTx.filter { it.transactionType == TransactionType.EXPENSE }
                        val totalExpense = monthExpenses.sumOf { it.amount.toDouble() }
                        val catBreakdown = monthExpenses.groupBy { it.categoryId }.map { (catId, txs) ->
                            val amt = txs.sumOf { it.amount.toDouble() }
                            CategoryBreakdownDto(
                                categoryId = catId, categoryName = categoryMap[catId]?.name ?: "Unknown",
                                amount = amt,
                                percentage = if (totalExpense > 0.0) (amt / totalExpense * 100.0).toBigDecimal().setScale(2, RoundingMode.HALF_UP).toDouble() else 0.0,
                                transactionCount = txs.size
                            )
                        }.sortedByDescending { it.amount }

                        // Last 6 months cash flow
                        val sixMonthsAgo = Calendar.getInstance().also {
                            it.add(Calendar.MONTH, -6)
                            it.set(Calendar.DAY_OF_MONTH, 1)
                            it.set(Calendar.HOUR_OF_DAY, 0); it.set(Calendar.MINUTE, 0); it.set(Calendar.SECOND, 0); it.set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        val recentTxForFlow = allTransactions.filter { it.timestamp >= sixMonthsAgo }
                        val cashFlowPoints = buildMonthlyCashFlowPoints(recentTxForFlow)

                        call.respond(ApiResponse(success = true, data = DashboardDto(
                            netWorth = netWorth, baseCurrency = baseCurrency,
                            monthlyIncome = monthlyIncome, monthlyExpenses = monthlyExpenses,
                            cashFlow = monthlyIncome - monthlyExpenses, month = monthLabel,
                            accounts = accountDtos, budgets = budgetDtos, loans = loanDtos,
                            upcomingPayments = upcoming14, overduePayments = overduePayments,
                            recentTransactions = recentTx, categoryBreakdown = catBreakdown,
                            cashFlowPoints = cashFlowPoints
                        )))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(success = false, error = e.message ?: "Internal error")
                        )
                    }
                }

                // ── Settings ──────────────────────────────────────────────────────────
                get("/settings") {
                    if (!authenticate(authSessionManager)) return@get
                    try {
                        val profileId = settingsRepository.currentProfileId.first()
                        val baseCurrency = settingsRepository.currency.first()
                        val precision = settingsRepository.decimalPrecision.first()
                        call.respond(ApiResponse(success = true, data = SettingsDto(
                            currentProfileId = profileId,
                            baseCurrency = baseCurrency,
                            decimalPrecision = precision
                        )))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(success = false, error = e.message ?: "Internal error")
                        )
                    }
                }
            }
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────────

    private fun PlannedPayment.toDto(
        now: Long,
        categoryMap: Map<Long, Category>,
        accountMap: Map<String, com.osscabinet.ossWallet.domain.model.Account>
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

    private fun Category.toDto(): CategoryDto = CategoryDto(
        id = id,
        name = name,
        parentId = parentId,
        colorHex = colorHex,
        nature = nature.name,
        iconName = iconName,
        isDefault = isDefault
    )

    private fun buildMonthlyCashFlowPoints(transactions: List<Transaction>): List<CashFlowPointDto> {
        val bucketMap = linkedMapOf<String, Triple<Long, Double, Double>>()
        transactions.sortedBy { it.timestamp }.forEach { tx ->
            val cal = Calendar.getInstance().also { it.timeInMillis = tx.timestamp }
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH) + 1
            val label = "$year-${month.toString().padStart(2, '0')}"
            val current = bucketMap.getOrDefault(label, Triple(tx.timestamp, 0.0, 0.0))
            bucketMap[label] = when (tx.transactionType) {
                TransactionType.INCOME -> current.copy(second = current.second + tx.amount.toDouble())
                TransactionType.EXPENSE -> current.copy(third = current.third + tx.amount.toDouble())
                else -> current
            }
        }
        return bucketMap.map { (label, triple) ->
            val (epoch, income, expense) = triple
            CashFlowPointDto(label = label, epochMillis = epoch, income = income, expense = expense, net = income - expense)
        }
    }

    private suspend fun RoutingContext.authenticate(manager: AuthSessionManager): Boolean {
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

    private fun loadIndexHtml(): String {
        return try {
            context.assets.open("web/index.html").bufferedReader().readText()
        } catch (e: Exception) {
            "<html><body>Web UI not found. Rebuild the app.</body></html>"
        }
    }
}
