package com.osscabinet.ossWallet.data.web

import android.content.Context
import com.osscabinet.ossWallet.data.web.dto.AccountDto
import com.osscabinet.ossWallet.data.web.dto.AddCardRequest
import com.osscabinet.ossWallet.data.web.dto.CardFilterUpdateDto
import com.osscabinet.ossWallet.data.web.dto.ApiResponse
import com.osscabinet.ossWallet.data.web.dto.AuthRequest
import com.osscabinet.ossWallet.data.web.dto.AuthResponse
import com.osscabinet.ossWallet.data.web.dto.BudgetDto
import com.osscabinet.ossWallet.data.web.dto.CardDataDto
import com.osscabinet.ossWallet.data.web.dto.CardPayloadDto
import com.osscabinet.ossWallet.data.web.dto.CashFlowPointDto
import com.osscabinet.ossWallet.data.web.dto.CategoryBreakdownDto
import com.osscabinet.ossWallet.data.web.dto.CategoryDto
import com.osscabinet.ossWallet.data.web.dto.CreateAccountRequest
import com.osscabinet.ossWallet.data.web.dto.CreateBudgetRequest
import com.osscabinet.ossWallet.data.web.dto.CreateCategoryRequest
import com.osscabinet.ossWallet.data.web.dto.CreateFilterRequest
import com.osscabinet.ossWallet.data.web.dto.CreateLoanRequest
import com.osscabinet.ossWallet.data.web.dto.CreatePlannedPaymentRequest
import com.osscabinet.ossWallet.data.web.dto.CreateTagRequest
import com.osscabinet.ossWallet.data.web.dto.CreateTemplateRequest
import com.osscabinet.ossWallet.data.web.dto.CreateTransactionRequest
import com.osscabinet.ossWallet.data.web.dto.FilterDto
import com.osscabinet.ossWallet.data.web.dto.FilterSummaryDto
import com.osscabinet.ossWallet.data.web.dto.LoanDto
import com.osscabinet.ossWallet.data.web.dto.LoanPaymentDto
import com.osscabinet.ossWallet.data.web.dto.LoanPaymentRequest
import com.osscabinet.ossWallet.data.web.dto.MonthlySummaryDto
import com.osscabinet.ossWallet.data.web.dto.PagedResponse
import com.osscabinet.ossWallet.data.web.dto.PlannedPaymentDto
import com.osscabinet.ossWallet.data.web.dto.RenameTagRequest
import com.osscabinet.ossWallet.data.web.dto.ReorderAccountsRequest
import com.osscabinet.ossWallet.data.web.dto.ReorderCardsRequest
import com.osscabinet.ossWallet.data.web.dto.ReorderCategoriesRequest
import com.osscabinet.ossWallet.data.web.dto.SettingsDto
import com.osscabinet.ossWallet.data.web.dto.TagDto
import com.osscabinet.ossWallet.data.web.dto.TemplateDto
import com.osscabinet.ossWallet.data.web.dto.TransactionDto
import com.osscabinet.ossWallet.data.web.dto.UpdateAccountRequest
import com.osscabinet.ossWallet.data.web.dto.UpdateBudgetRequest
import com.osscabinet.ossWallet.data.web.dto.UpdateCategoryRequest
import com.osscabinet.ossWallet.data.web.dto.UpdateLoanRequest
import com.osscabinet.ossWallet.data.web.dto.UpdatePlannedPaymentRequest
import com.osscabinet.ossWallet.data.web.dto.UpdateSettingsRequest
import com.osscabinet.ossWallet.data.web.dto.UpdateTemplateRequest
import com.osscabinet.ossWallet.data.web.dto.UpdateTransactionRequest
import com.osscabinet.ossWallet.domain.card.CardComputerRegistry
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.CategoryNature
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.Loan
import com.osscabinet.ossWallet.domain.model.LoanPayment
import com.osscabinet.ossWallet.domain.model.LoanType
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.PaymentSchedule
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
import com.osscabinet.ossWallet.domain.model.RecurrenceRule
import com.osscabinet.ossWallet.domain.model.RecurrenceType
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.settings.CardConfig
import com.osscabinet.ossWallet.domain.model.settings.ScreenContext
import com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.BudgetRepository
import com.osscabinet.ossWallet.domain.repository.CardRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.CurrencyRepository
import com.osscabinet.ossWallet.domain.repository.FilterRepository
import com.osscabinet.ossWallet.domain.repository.LoanRepository
import com.osscabinet.ossWallet.domain.repository.PlannedPaymentRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TagRepository
import com.osscabinet.ossWallet.domain.repository.TemplateRepository
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
import io.ktor.server.routing.put
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
    private val currencyRepository: CurrencyRepository,
    private val filterRepository: FilterRepository,
    private val cardRepository: CardRepository,
    private val cardComputerRegistry: CardComputerRegistry,
    private val templateRepository: TemplateRepository
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
                        call.respond(
                            ApiResponse(
                                success = true,
                                data = AuthResponse(token = token)
                            )
                        )
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
                        val allTransactions =
                            transactionRepository.getAllTransactionsSync(profileId)
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
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                // ── Transactions ──────────────────────────────────────────────────────
                get("/transactions") {
                    if (!authenticate(authSessionManager)) return@get
                    try {
                        val profileId = settingsRepository.currentProfileId.first()
                        val page =
                            call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(0)
                                ?: 0
                        val limit =
                            call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 100)
                                ?: 25
                        val accountFilter = call.request.queryParameters["accountId"]
                        val typeParam = call.request.queryParameters["type"]
                        val dateFrom = call.request.queryParameters["dateFrom"]?.toLongOrNull()
                        val dateTo = call.request.queryParameters["dateTo"]?.toLongOrNull()
                        val categoryIdParam =
                            call.request.queryParameters["categoryId"]?.toLongOrNull()
                        val tagNamesParam = call.request.queryParameters["tagNames"]
                        val minAmountParam =
                            call.request.queryParameters["minAmount"]?.toDoubleOrNull()
                        val maxAmountParam =
                            call.request.queryParameters["maxAmount"]?.toDoubleOrNull()
                        val currencyParam = call.request.queryParameters["currency"]
                        val statusParam = call.request.queryParameters["status"]
                        val payeeParam = call.request.queryParameters["payee"]

                        val allTransactions =
                            transactionRepository.getAllTransactionsSync(profileId)
                        val categoryMap =
                            categoryRepository.getAllCategoriesSync().associateBy { it.id }
                        val accountMap =
                            accountRepository.getAllAccountsSync(profileId).associateBy { it.id }

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
                                setOf(TransactionType.entries.find {
                                    it.name.equals(
                                        typeParam,
                                        ignoreCase = true
                                    )
                                } ?: TransactionType.EXPENSE)
                            } else emptySet(),
                            categoryIds = if (categoryIdParam != null) setOf(categoryIdParam) else emptySet(),
                            tags = if (!tagNamesParam.isNullOrBlank()) tagNamesParam.split(",")
                                .map { it.trim() }.toSet() else emptySet(),
                            minAmount = if (minAmountParam != null) BigDecimal.valueOf(
                                minAmountParam
                            ) else null,
                            maxAmount = if (maxAmountParam != null) BigDecimal.valueOf(
                                maxAmountParam
                            ) else null,
                            currencies = if (!currencyParam.isNullOrBlank()) setOf(currencyParam) else emptySet(),
                            statuses = if (statusParam != null) {
                                setOf(TransactionStatus.entries.find {
                                    it.name.equals(
                                        statusParam,
                                        ignoreCase = true
                                    )
                                } ?: TransactionStatus.CLEARED)
                            } else emptySet(),
                            queryPayee = payeeParam,
                            excludeFuture = false
                        )

                        val filtered =
                            TransactionFilterService.filterTransactions(allTransactions, filter)
                        val sorted = filtered.sortedByDescending { it.timestamp }
                        val paged = sorted.drop(page * limit).take(limit)

                        val dtos = paged.map { tx ->
                            tx.toDto(categoryMap, accountMap)
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
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
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
                            transactionType = TransactionType.entries.find {
                                it.name.equals(
                                    req.transactionType,
                                    ignoreCase = true
                                )
                            }
                                ?: TransactionType.EXPENSE,
                            accountId = req.accountId,
                            categoryId = req.categoryId,
                            tags = req.tags.toSet(),
                            paymentMethod = PaymentMethod.entries.find {
                                it.name.equals(
                                    req.paymentMethod,
                                    ignoreCase = true
                                )
                            }
                                ?: PaymentMethod.CASH,
                            payee = req.payee,
                            note = req.note,
                            status = TransactionStatus.entries.find {
                                it.name.equals(
                                    req.status,
                                    ignoreCase = true
                                )
                            }
                                ?: TransactionStatus.CLEARED,
                            gpsLocation = req.gpsLocation,
                            location = req.location,
                            warrantyExpiration = req.warrantyExpiration
                        )
                        val newId = transactionRepository.insertTransaction(tx, profileId)
                        val categoryMap =
                            categoryRepository.getAllCategoriesSync().associateBy { it.id }
                        val accountMap =
                            accountRepository.getAllAccountsSync(profileId).associateBy { it.id }
                        val created = tx.copy(id = newId)
                        val dto = created.toDto(categoryMap, accountMap)
                        call.respond(
                            HttpStatusCode.Created,
                            ApiResponse(success = true, data = dto)
                        )
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                delete("/transactions/{id}") {
                    if (!authenticate(authSessionManager)) return@delete
                    try {
                        val id = call.parameters["id"]?.toLongOrNull()
                            ?: return@delete call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<Unit>(success = false, error = "Invalid id")
                            )
                        transactionRepository.deleteTransaction(id)
                        call.respond(ApiResponse(success = true, data = mapOf("deleted" to id)))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                // ── Summary ───────────────────────────────────────────────────────────
                get("/summary") {
                    if (!authenticate(authSessionManager)) return@get
                    try {
                        val profileId = settingsRepository.currentProfileId.first()
                        val allTransactions =
                            transactionRepository.getAllTransactionsSync(profileId)
                        val accounts = accountRepository.getAllAccountsSync(profileId)

                        val cal = Calendar.getInstance()
                        val year = cal.get(Calendar.YEAR)
                        val month = cal.get(Calendar.MONTH) + 1

                        val monthlyTx = allTransactions.filter { tx ->
                            val txCal =
                                Calendar.getInstance().also { it.timeInMillis = tx.timestamp }
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
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
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
                        val allTransactions =
                            transactionRepository.getAllTransactionsSync(profileId)
                        val budgets = budgetRepository.getActiveBudgetsSync(profileId)
                        val categories = categoryRepository.getAllCategoriesSync()
                        val exchangeRates = runCatching {
                            currencyRepository.getExchangeRates(baseCurrency)
                                .mapValues { BigDecimal.valueOf(it.value) }
                        }.getOrDefault(emptyMap())

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
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
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
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                delete("/budgets/{id}") {
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
                                error = e.message ?: "Internal error"
                            )
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
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
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
                            loanType = LoanType.entries.find {
                                it.name.equals(
                                    req.loanType,
                                    ignoreCase = true
                                )
                            }
                                ?: LoanType.BORROWED,
                            lenderOrBorrowerName = req.lenderOrBorrowerName,
                            startDate = req.startDate,
                            expectedSettlementDate = req.expectedSettlementDate,
                            interestRate = req.interestRate?.toFloat(),
                            note = req.note,
                            linkedAccountId = req.linkedAccountId,
                            profileId = profileId
                        )
                        val newId = loanRepository.insertLoan(loan)
                        call.respond(
                            HttpStatusCode.Created,
                            ApiResponse(success = true, data = mapOf("id" to newId))
                        )
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                delete("/loans/{id}") {
                    if (!authenticate(authSessionManager)) return@delete
                    try {
                        val id = call.parameters["id"]?.toLongOrNull()
                            ?: return@delete call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<Unit>(success = false, error = "Invalid id")
                            )
                        loanRepository.deleteLoan(id)
                        call.respond(ApiResponse(success = true, data = mapOf("deleted" to id)))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
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
                        val categoryMap =
                            categoryRepository.getAllCategoriesSync().associateBy { it.id }
                        val accountMap =
                            accountRepository.getAllAccountsSync(profileId).associateBy { it.id }

                        val dtos = payments.map { payment ->
                            payment.toDto(now, categoryMap, accountMap)
                        }
                        call.respond(ApiResponse(success = true, data = dtos))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
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
                        val categoryMap =
                            categoryRepository.getAllCategoriesSync().associateBy { it.id }
                        val accountMap =
                            accountRepository.getAllAccountsSync(profileId).associateBy { it.id }

                        val upcoming =
                            PlannedPaymentService.generateUpcoming(allPayments, now, days)
                        val dtos = upcoming.map { item ->
                            item.plannedPayment.toDto(now, categoryMap, accountMap)
                        }
                        call.respond(ApiResponse(success = true, data = dtos))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                get("/planned-payments/overdue") {
                    if (!authenticate(authSessionManager)) return@get
                    try {
                        val profileId = settingsRepository.currentProfileId.first()
                        val now = System.currentTimeMillis()
                        val allPayments = plannedPaymentRepository.getAllSync(profileId)
                        val categoryMap =
                            categoryRepository.getAllCategoriesSync().associateBy { it.id }
                        val accountMap =
                            accountRepository.getAllAccountsSync(profileId).associateBy { it.id }

                        val overdue = PlannedPaymentService.getOverdue(allPayments, now)
                        val dtos = overdue.map { payment ->
                            payment.toDto(now, categoryMap, accountMap)
                        }
                        call.respond(ApiResponse(success = true, data = dtos))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                post("/planned-payments") {
                    if (!authenticate(authSessionManager)) return@post
                    try {
                        val profileId = settingsRepository.currentProfileId.first()
                        val req = call.receive<CreatePlannedPaymentRequest>()
                        val recurrenceType = RecurrenceType.entries.find {
                            it.name.equals(
                                req.recurrenceType,
                                ignoreCase = true
                            )
                        }
                            ?: RecurrenceType.MONTHLY
                        val rule = RecurrenceRule(
                            type = recurrenceType,
                            interval = req.recurrenceInterval,
                            dayOfWeek = req.dayOfWeek,
                            dayOfMonth = req.dayOfMonth
                        )
                        val schedule = PaymentSchedule(
                            recurrenceRule = rule,
                            startDate = req.startDate,
                            endDate = req.endDate,
                            nextDueDate = req.startDate
                        )
                        val payment = PlannedPayment(
                            name = req.name,
                            amount = BigDecimal.valueOf(req.amount),
                            currency = req.currency,
                            transactionType = TransactionType.entries.find {
                                it.name.equals(
                                    req.transactionType,
                                    ignoreCase = true
                                )
                            }
                                ?: TransactionType.EXPENSE,
                            categoryId = req.categoryId,
                            accountId = req.accountId,
                            targetCurrency = req.targetCurrency,
                            transferRate = BigDecimal.valueOf(req.transferRate),
                            note = req.note,
                            payee = req.payee,
                            location = req.location,
                            gpsLocation = req.gpsLocation,
                            warrantyExpiration = req.warrantyExpiration,
                            transactionStatus = TransactionStatus.entries.find {
                                it.name.equals(
                                    req.transactionStatus,
                                    ignoreCase = true
                                )
                            }
                                ?: TransactionStatus.CLEARED,
                            paymentMethod = req.paymentMethod?.let { m ->
                                PaymentMethod.entries.find { it.name.equals(m, ignoreCase = true) }
                            },
                            tags = req.tags.toSet(),
                            schedule = schedule,
                            autoConfirm = req.autoConfirm,
                            profileId = profileId
                        )
                        val newId = plannedPaymentRepository.insert(payment)
                        call.respond(
                            HttpStatusCode.Created,
                            ApiResponse(success = true, data = mapOf("id" to newId))
                        )
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                delete("/planned-payments/{id}") {
                    if (!authenticate(authSessionManager)) return@delete
                    try {
                        val id = call.parameters["id"]?.toLongOrNull()
                            ?: return@delete call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<Unit>(success = false, error = "Invalid id")
                            )
                        plannedPaymentRepository.delete(id)
                        call.respond(ApiResponse(success = true, data = mapOf("deleted" to id)))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                post("/planned-payments/{id}/confirm") {
                    if (!authenticate(authSessionManager)) return@post
                    try {
                        val profileId = settingsRepository.currentProfileId.first()
                        val id = call.parameters["id"]?.toLongOrNull()
                            ?: return@post call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<Unit>(success = false, error = "Invalid id")
                            )
                        val payment = plannedPaymentRepository.getById(id)
                            ?: return@post call.respond(
                                HttpStatusCode.NotFound,
                                ApiResponse<Unit>(success = false, error = "Payment not found")
                            )
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
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                post("/planned-payments/{id}/skip") {
                    if (!authenticate(authSessionManager)) return@post
                    try {
                        val id = call.parameters["id"]?.toLongOrNull()
                            ?: return@post call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<Unit>(success = false, error = "Invalid id")
                            )
                        val payment = plannedPaymentRepository.getById(id)
                            ?: return@post call.respond(
                                HttpStatusCode.NotFound,
                                ApiResponse<Unit>(success = false, error = "Payment not found")
                            )
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
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
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
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                post("/categories") {
                    if (!authenticate(authSessionManager)) return@post
                    try {
                        val req = call.receive<CreateCategoryRequest>()
                        val nature = CategoryNature.entries.find {
                            it.name.equals(
                                req.nature,
                                ignoreCase = true
                            )
                        }
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
                        call.respond(
                            HttpStatusCode.Created,
                            ApiResponse(success = true, data = category.toDto())
                        )
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                delete("/categories/{id}") {
                    if (!authenticate(authSessionManager)) return@delete
                    try {
                        val id = call.parameters["id"]?.toLongOrNull()
                            ?: return@delete call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<Unit>(success = false, error = "Invalid id")
                            )
                        val category =
                            categoryRepository.getAllCategoriesSync().find { it.id == id }
                                ?: return@delete call.respond(
                                    HttpStatusCode.NotFound,
                                    ApiResponse<Unit>(success = false, error = "Category not found")
                                )
                        categoryRepository.deleteCategory(category)
                        call.respond(ApiResponse(success = true, data = mapOf("deleted" to id)))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                // ── Tags ──────────────────────────────────────────────────────────────
                get("/tags") {
                    if (!authenticate(authSessionManager)) return@get
                    try {
                        val profileId = settingsRepository.currentProfileId.first()
                        val allTags = tagRepository.getAllTags().first()
                        val allTransactions =
                            transactionRepository.getAllTransactionsSync(profileId)

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
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                delete("/tags/{name}") {
                    if (!authenticate(authSessionManager)) return@delete
                    try {
                        val name = call.parameters["name"]
                            ?: return@delete call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<Unit>(success = false, error = "Invalid name")
                            )
                        val allTags = tagRepository.getAllTags().first()
                        val tag = allTags.find { it.name == name }
                            ?: return@delete call.respond(
                                HttpStatusCode.NotFound,
                                ApiResponse<Unit>(success = false, error = "Tag not found")
                            )
                        tagRepository.deleteTagAndRemoveFromTransactions(tag)
                        call.respond(ApiResponse(success = true, data = mapOf("deleted" to name)))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
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

                        val txType = TransactionType.entries.find {
                            it.name.equals(
                                typeParam,
                                ignoreCase = true
                            )
                        }
                            ?: TransactionType.EXPENSE

                        val allTransactions =
                            transactionRepository.getAllTransactionsSync(profileId)
                        val categoryMap =
                            categoryRepository.getAllCategoriesSync().associateBy { it.id }

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
                                percentage = pct.toBigDecimal().setScale(2, RoundingMode.HALF_UP)
                                    .toDouble(),
                                transactionCount = txs.size
                            )
                        }.sortedByDescending { it.amount }

                        call.respond(ApiResponse(success = true, data = dtos))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                get("/analytics/cash-flow") {
                    if (!authenticate(authSessionManager)) return@get
                    try {
                        val profileId = settingsRepository.currentProfileId.first()
                        val dateFrom = call.request.queryParameters["dateFrom"]?.toLongOrNull()
                        val dateTo = call.request.queryParameters["dateTo"]?.toLongOrNull()
                        val granularity =
                            call.request.queryParameters["granularity"]?.uppercase() ?: "MONTH"

                        val allTransactions =
                            transactionRepository.getAllTransactionsSync(profileId)

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
                                    Bucket(
                                        "$year-W${week.toString().padStart(2, '0')}",
                                        cal.timeInMillis
                                    )
                                }

                                else -> { // MONTH
                                    val month = cal.get(Calendar.MONTH) + 1
                                    val year = cal.get(Calendar.YEAR)
                                    Bucket(
                                        "$year-${month.toString().padStart(2, '0')}",
                                        cal.timeInMillis
                                    )
                                }
                            }
                        }

                        // Accumulate per bucket
                        val bucketMap =
                            linkedMapOf<String, Triple<Long, Double, Double>>() // label -> (epoch, income, expense)
                        filtered.sortedBy { it.timestamp }.forEach { tx ->
                            val bucket = getBucket(tx.timestamp)
                            val current = bucketMap.getOrDefault(
                                bucket.label,
                                Triple(bucket.epochMillis, 0.0, 0.0)
                            )
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
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                // ── Dashboard — card-aware, per-card filters ──────────────────────────
                get("/dashboard") {
                    if (!authenticate(authSessionManager)) return@get
                    try {
                        val profileId = settingsRepository.currentProfileId.first()
                        val baseCurrency = settingsRepository.currency.first()
                        val precision = settingsRepository.decimalPrecision.first()
                        val now = System.currentTimeMillis()

                        // Load all raw data once — shared across all card computers
                        val allTransactions =
                            transactionRepository.getAllTransactionsSync(profileId)
                        val accounts = accountRepository.getAllAccountsSync(profileId)
                        val categories = categoryRepository.getAllCategoriesSync()
                        val categoryMap = categories.associateBy { it.id }
                        val accountMap = accounts.associateBy { it.id }
                        val allTags = tagRepository.getAllTags().first()
                        val budgets = budgetRepository.getActiveBudgetsSync(profileId)
                        val loans = loanRepository.getActiveLoansSync(profileId)
                        val loanPayments = loans.associate { loan ->
                            loan.id to loanRepository.getPaymentsForLoanSync(loan.id)
                        }
                        val allPayments = plannedPaymentRepository.getAllSync(profileId)
                        val exchangeRates = runCatching {
                            currencyRepository.getExchangeRates(baseCurrency)
                                .mapValues { BigDecimal.valueOf(it.value) }
                        }.getOrDefault(emptyMap())

                        val baseInput = CardInput(
                            profileId = profileId,
                            transactions = allTransactions,
                            accounts = accounts,
                            categories = categories,
                            tags = allTags,
                            exchangeRates = exchangeRates,
                            baseCurrency = baseCurrency,
                            displayCurrency = baseCurrency,
                            filter = TransactionFilter(excludeFuture = false),
                            plannedPayments = allPayments,
                            budgets = budgets,
                            loans = loans,
                            loanPayments = loanPayments
                        )

                        // Read the user's actual dashboard card layout
                        val dashboardConfig = cardRepository.dashboardCardsConfig.first()
                        val enabledCards = dashboardConfig.cards
                            .filter { it.isEnabled }
                            .sortedBy { it.order }

                        val cardResults = enabledCards.map { cardConfig ->
                            val filterSummary = cardConfig.filter.toFilterSummaryDto()
                            val payload = runCatching {
                                val renderData = cardComputerRegistry.compute(
                                    type = cardConfig.cardType,
                                    input = baseInput,
                                    config = cardConfig
                                )
                                renderData.toPayloadDto(
                                    now,
                                    categoryMap,
                                    accountMap,
                                    precision,
                                    baseCurrency
                                )
                            }.getOrElse { e ->
                                CardPayloadDto.Error(e.message ?: "Computation failed")
                            }
                            CardDataDto(
                                cardId = cardConfig.cardId,
                                cardType = cardConfig.cardType.name,
                                title = cardConfig.title,
                                order = cardConfig.order,
                                isEnabled = cardConfig.isEnabled,
                                filterSummary = filterSummary,
                                data = payload
                            )
                        }

                        call.respond(ApiResponse(success = true, data = cardResults))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                // ── Accounts (CRUD) ───────────────────────────────────────────────────
                post("/accounts") {
                    if (!authenticate(authSessionManager)) return@post
                    try {
                        val profileId = settingsRepository.currentProfileId.first()
                        val req = call.receive<CreateAccountRequest>()
                        val account = Account(
                            id = java.util.UUID.randomUUID().toString(),
                            name = req.name,
                            currencyCode = req.currencyCode,
                            structureType = AccountStructureType.entries.find {
                                it.name.equals(
                                    req.structureType,
                                    ignoreCase = true
                                )
                            }
                                ?: AccountStructureType.SAVINGS,
                            colorHex = req.colorHex,
                            icon = req.icon,
                            allowedCurrencies = req.allowedCurrencies
                        )
                        accountRepository.insertAccount(account, profileId)
                        if (req.initialBalance != null && req.initialBalance != 0.0) {
                            val initTx = Transaction(
                                name = "Initial Balance",
                                amount = BigDecimal.valueOf(Math.abs(req.initialBalance)),
                                currency = account.currencyCode,
                                transactionType = if (req.initialBalance >= 0) TransactionType.INCOME else TransactionType.EXPENSE,
                                accountId = account.id,
                                categoryId = 0L,
                                timestamp = System.currentTimeMillis()
                            )
                            transactionRepository.insertTransaction(initTx, profileId)
                        }
                        val allTransactions =
                            transactionRepository.getAllTransactionsSync(profileId)
                        val balance = allTransactions.filter { it.accountId == account.id }
                            .fold(BigDecimal.ZERO) { acc, tx ->
                                when (tx.transactionType) {
                                    TransactionType.INCOME -> acc + tx.amount
                                    TransactionType.EXPENSE -> acc - tx.amount
                                    TransactionType.TRANSFER -> acc - tx.amount
                                }
                            }
                        call.respond(
                            HttpStatusCode.Created, ApiResponse(
                                success = true, data = AccountDto(
                                    id = account.id,
                                    name = account.name,
                                    currency = account.currencyCode,
                                    balance = balance.toDouble(),
                                    type = account.structureType.name
                                )
                            )
                        )
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                get("/accounts/{id}") {
                    if (!authenticate(authSessionManager)) return@get
                    try {
                        val profileId = settingsRepository.currentProfileId.first()
                        val id = call.parameters["id"]
                            ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Invalid id"))
                        val account = accountRepository.getAllAccountsSync(profileId).find { it.id == id }
                            ?: return@get call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, error = "Account not found"))
                        val allTransactions = transactionRepository.getAllTransactionsSync(profileId)
                        val txForAccount = allTransactions.filter { it.accountId == account.id }
                        val balance = txForAccount.fold(BigDecimal.ZERO) { acc, tx ->
                            when (tx.transactionType) {
                                TransactionType.INCOME -> acc + tx.amount
                                TransactionType.EXPENSE -> acc - tx.amount
                                TransactionType.TRANSFER -> acc - tx.amount
                            }
                        }
                        val categoryMap = categoryRepository.getAllCategoriesSync().associateBy { it.id }
                        val accountMap = accountRepository.getAllAccountsSync(profileId).associateBy { it.id }
                        val recentTx = txForAccount.sortedByDescending { it.timestamp }.take(20).map { it.toDto(categoryMap, accountMap) }
                        call.respond(ApiResponse(success = true, data = mapOf(
                            "account" to AccountDto(id = account.id, name = account.name, currency = account.currencyCode, balance = balance.toDouble(), type = account.structureType.name),
                            "recentTransactions" to recentTx
                        )))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = e.message ?: "Internal error"))
                    }
                }

                put("/accounts/{id}") {
                    if (!authenticate(authSessionManager)) return@put
                    try {
                        val profileId = settingsRepository.currentProfileId.first()
                        val id = call.parameters["id"]
                            ?: return@put call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<Unit>(success = false, error = "Invalid id")
                            )
                        val existing =
                            accountRepository.getAllAccountsSync(profileId).find { it.id == id }
                                ?: return@put call.respond(
                                    HttpStatusCode.NotFound,
                                    ApiResponse<Unit>(success = false, error = "Account not found")
                                )
                        val req = call.receive<UpdateAccountRequest>()
                        val updated = existing.copy(
                            name = req.name ?: existing.name,
                            currencyCode = req.currencyCode ?: existing.currencyCode,
                            structureType = req.structureType?.let { t ->
                                AccountStructureType.entries.find {
                                    it.name.equals(
                                        t,
                                        ignoreCase = true
                                    )
                                } ?: existing.structureType
                            } ?: existing.structureType,
                            colorHex = req.colorHex ?: existing.colorHex,
                            icon = req.icon ?: existing.icon,
                            isArchived = req.isArchived ?: existing.isArchived,
                            allowedCurrencies = req.allowedCurrencies ?: existing.allowedCurrencies
                        )
                        accountRepository.updateAccount(updated, profileId)
                        val allTransactions =
                            transactionRepository.getAllTransactionsSync(profileId)
                        val balance = allTransactions.filter { it.accountId == updated.id }
                            .fold(BigDecimal.ZERO) { acc, tx ->
                                when (tx.transactionType) {
                                    TransactionType.INCOME -> acc + tx.amount
                                    TransactionType.EXPENSE -> acc - tx.amount
                                    TransactionType.TRANSFER -> acc - tx.amount
                                }
                            }
                        call.respond(
                            ApiResponse(
                                success = true, data = AccountDto(
                                    id = updated.id,
                                    name = updated.name,
                                    currency = updated.currencyCode,
                                    balance = balance.toDouble(),
                                    type = updated.structureType.name
                                )
                            )
                        )
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                delete("/accounts/{id}") {
                    if (!authenticate(authSessionManager)) return@delete
                    try {
                        val id = call.parameters["id"]
                            ?: return@delete call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<Unit>(success = false, error = "Invalid id")
                            )
                        accountRepository.deleteAccount(id)
                        call.respond(ApiResponse(success = true, data = mapOf("deleted" to id)))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                // ── Transactions (detail + edit) ───────────────────────────────────────
                get("/transactions/{id}") {
                    if (!authenticate(authSessionManager)) return@get
                    try {
                        val profileId = settingsRepository.currentProfileId.first()
                        val id = call.parameters["id"]?.toLongOrNull()
                            ?: return@get call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<Unit>(success = false, error = "Invalid id")
                            )
                        val tx = transactionRepository.getTransactionById(id)
                            ?: return@get call.respond(
                                HttpStatusCode.NotFound,
                                ApiResponse<Unit>(success = false, error = "Transaction not found")
                            )
                        val categoryMap =
                            categoryRepository.getAllCategoriesSync().associateBy { it.id }
                        val accountMap =
                            accountRepository.getAllAccountsSync(profileId).associateBy { it.id }
                        call.respond(ApiResponse(success = true, data = tx.toDto(categoryMap, accountMap)))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                put("/transactions/{id}") {
                    if (!authenticate(authSessionManager)) return@put
                    try {
                        val profileId = settingsRepository.currentProfileId.first()
                        val id = call.parameters["id"]?.toLongOrNull()
                            ?: return@put call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<Unit>(success = false, error = "Invalid id")
                            )
                        val old = transactionRepository.getTransactionById(id)
                            ?: return@put call.respond(
                                HttpStatusCode.NotFound,
                                ApiResponse<Unit>(success = false, error = "Transaction not found")
                            )
                        val req = call.receive<UpdateTransactionRequest>()
                        val updated = old.copy(
                            name = req.name ?: old.name,
                            amount = req.amount?.let { BigDecimal.valueOf(it) } ?: old.amount,
                            timestamp = req.timestamp ?: old.timestamp,
                            currency = req.currency ?: old.currency,
                            transactionType = req.transactionType?.let { t ->
                                TransactionType.entries.find {
                                    it.name.equals(
                                        t,
                                        ignoreCase = true
                                    )
                                } ?: old.transactionType
                            } ?: old.transactionType,
                            accountId = req.accountId ?: old.accountId,
                            categoryId = req.categoryId ?: old.categoryId,
                            tags = req.tags?.toSet() ?: old.tags,
                            paymentMethod = req.paymentMethod?.let { m ->
                                PaymentMethod.entries.find { it.name.equals(m, ignoreCase = true) }
                                    ?: old.paymentMethod
                            } ?: old.paymentMethod,
                            payee = req.payee ?: old.payee,
                            note = req.note ?: old.note,
                            status = req.status?.let { s ->
                                TransactionStatus.entries.find {
                                    it.name.equals(
                                        s,
                                        ignoreCase = true
                                    )
                                } ?: old.status
                            } ?: old.status,
                            gpsLocation = req.gpsLocation ?: old.gpsLocation,
                            location = req.location ?: old.location,
                            warrantyExpiration = req.warrantyExpiration ?: old.warrantyExpiration
                        )
                        transactionRepository.updateTransaction(old, updated, profileId)
                        val categoryMap =
                            categoryRepository.getAllCategoriesSync().associateBy { it.id }
                        val accountMap =
                            accountRepository.getAllAccountsSync(profileId).associateBy { it.id }
                        call.respond(ApiResponse(success = true, data = updated.toDto(categoryMap, accountMap)))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                // ── Budgets (detail + edit) ────────────────────────────────────────────
                get("/budgets/{id}") {
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
                        val budget = budgetRepository.getById(id)
                            ?: return@get call.respond(
                                HttpStatusCode.NotFound,
                                ApiResponse<Unit>(success = false, error = "Budget not found")
                            )
                        val allTransactions =
                            transactionRepository.getAllTransactionsSync(profileId)
                        val categories = categoryRepository.getAllCategoriesSync()
                        val exchangeRates = runCatching {
                            currencyRepository.getExchangeRates(baseCurrency)
                                .mapValues { BigDecimal.valueOf(it.value) }
                        }.getOrDefault(emptyMap())
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
                                    isArchived = budget.isArchived
                                )
                            )
                        )
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                put("/budgets/{id}") {
                    if (!authenticate(authSessionManager)) return@put
                    try {
                        val id = call.parameters["id"]?.toLongOrNull()
                            ?: return@put call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<Unit>(success = false, error = "Invalid id")
                            )
                        val existing = budgetRepository.getById(id)
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
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                // ── Loans (detail + edit + payments) ──────────────────────────────────
                get("/loans/{id}") {
                    if (!authenticate(authSessionManager)) return@get
                    try {
                        val now = System.currentTimeMillis()
                        val precision = settingsRepository.decimalPrecision.first()
                        val id = call.parameters["id"]?.toLongOrNull()
                            ?: return@get call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<Unit>(success = false, error = "Invalid id")
                            )
                        val loan = loanRepository.getLoanById(id)
                            ?: return@get call.respond(
                                HttpStatusCode.NotFound,
                                ApiResponse<Unit>(success = false, error = "Loan not found")
                            )
                        val payments = loanRepository.getPaymentsForLoanSync(loan.id)
                        val summary = LoanService.buildSummary(loan, payments, now, precision)
                        call.respond(
                            ApiResponse(
                                success = true, data = LoanDto(
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
                            )
                        )
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                put("/loans/{id}") {
                    if (!authenticate(authSessionManager)) return@put
                    try {
                        val id = call.parameters["id"]?.toLongOrNull()
                            ?: return@put call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<Unit>(success = false, error = "Invalid id")
                            )
                        val existing = loanRepository.getLoanById(id)
                            ?: return@put call.respond(
                                HttpStatusCode.NotFound,
                                ApiResponse<Unit>(success = false, error = "Loan not found")
                            )
                        val req = call.receive<UpdateLoanRequest>()
                        val updated = existing.copy(
                            name = req.name ?: existing.name,
                            principalAmount = req.principalAmount?.let { BigDecimal.valueOf(it) }
                                ?: existing.principalAmount,
                            currency = req.currency ?: existing.currency,
                            loanType = req.loanType?.let { t ->
                                LoanType.entries.find { it.name.equals(t, ignoreCase = true) }
                                    ?: existing.loanType
                            } ?: existing.loanType,
                            lenderOrBorrowerName = req.lenderOrBorrowerName
                                ?: existing.lenderOrBorrowerName,
                            startDate = req.startDate ?: existing.startDate,
                            expectedSettlementDate = req.expectedSettlementDate
                                ?: existing.expectedSettlementDate,
                            interestRate = req.interestRate?.toFloat() ?: existing.interestRate,
                            note = req.note ?: existing.note,
                            linkedAccountId = req.linkedAccountId ?: existing.linkedAccountId
                        )
                        loanRepository.updateLoan(updated)
                        call.respond(ApiResponse(success = true, data = mapOf("updated" to id)))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                get("/loans/{id}/payments") {
                    if (!authenticate(authSessionManager)) return@get
                    try {
                        val id = call.parameters["id"]?.toLongOrNull()
                            ?: return@get call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<Unit>(success = false, error = "Invalid id")
                            )
                        val payments = loanRepository.getPaymentsForLoanSync(id)
                        val dtos = payments.map { p ->
                            LoanPaymentDto(
                                id = p.id, loanId = p.loanId, amount = p.amount.toDouble(),
                                date = p.date, note = p.note
                            )
                        }
                        call.respond(ApiResponse(success = true, data = dtos))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                post("/loans/{id}/payments") {
                    if (!authenticate(authSessionManager)) return@post
                    try {
                        val id = call.parameters["id"]?.toLongOrNull()
                            ?: return@post call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<Unit>(success = false, error = "Invalid id")
                            )
                        loanRepository.getLoanById(id)
                            ?: return@post call.respond(
                                HttpStatusCode.NotFound,
                                ApiResponse<Unit>(success = false, error = "Loan not found")
                            )
                        val req = call.receive<LoanPaymentRequest>()
                        val payment = LoanPayment(
                            loanId = id,
                            amount = BigDecimal.valueOf(req.amount),
                            date = req.date,
                            note = req.note
                        )
                        val newId = loanRepository.insertPayment(payment)
                        call.respond(
                            HttpStatusCode.Created,
                            ApiResponse(success = true, data = mapOf("id" to newId))
                        )
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                // ── Planned Payments (detail + edit) ──────────────────────────────────
                get("/planned-payments/{id}") {
                    if (!authenticate(authSessionManager)) return@get
                    try {
                        val profileId = settingsRepository.currentProfileId.first()
                        val now = System.currentTimeMillis()
                        val id = call.parameters["id"]?.toLongOrNull()
                            ?: return@get call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<Unit>(success = false, error = "Invalid id")
                            )
                        val payment = plannedPaymentRepository.getById(id)
                            ?: return@get call.respond(
                                HttpStatusCode.NotFound,
                                ApiResponse<Unit>(success = false, error = "Payment not found")
                            )
                        val categoryMap =
                            categoryRepository.getAllCategoriesSync().associateBy { it.id }
                        val accountMap =
                            accountRepository.getAllAccountsSync(profileId).associateBy { it.id }
                        call.respond(
                            ApiResponse(
                                success = true,
                                data = payment.toDto(now, categoryMap, accountMap)
                            )
                        )
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                put("/planned-payments/{id}") {
                    if (!authenticate(authSessionManager)) return@put
                    try {
                        val id = call.parameters["id"]?.toLongOrNull()
                            ?: return@put call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<Unit>(success = false, error = "Invalid id")
                            )
                        val existing = plannedPaymentRepository.getById(id)
                            ?: return@put call.respond(
                                HttpStatusCode.NotFound,
                                ApiResponse<Unit>(success = false, error = "Payment not found")
                            )
                        val req = call.receive<UpdatePlannedPaymentRequest>()
                        val recurrenceRule =
                            if (req.recurrenceType != null || req.recurrenceInterval != null || req.dayOfWeek != null || req.dayOfMonth != null) {
                                val type = req.recurrenceType?.let { t ->
                                    RecurrenceType.entries.find {
                                        it.name.equals(
                                            t,
                                            ignoreCase = true
                                        )
                                    }
                                } ?: existing.schedule.recurrenceRule.type
                                RecurrenceRule(
                                    type = type,
                                    interval = req.recurrenceInterval
                                        ?: existing.schedule.recurrenceRule.interval,
                                    dayOfWeek = req.dayOfWeek
                                        ?: existing.schedule.recurrenceRule.dayOfWeek,
                                    dayOfMonth = req.dayOfMonth
                                        ?: existing.schedule.recurrenceRule.dayOfMonth
                                )
                            } else existing.schedule.recurrenceRule
                        val schedule = existing.schedule.copy(
                            recurrenceRule = recurrenceRule,
                            startDate = req.startDate ?: existing.schedule.startDate,
                            endDate = req.endDate ?: existing.schedule.endDate
                        )
                        val updated = existing.copy(
                            name = req.name ?: existing.name,
                            amount = req.amount?.let { BigDecimal.valueOf(it) } ?: existing.amount,
                            currency = req.currency ?: existing.currency,
                            transactionType = req.transactionType?.let { t ->
                                TransactionType.entries.find {
                                    it.name.equals(
                                        t,
                                        ignoreCase = true
                                    )
                                } ?: existing.transactionType
                            } ?: existing.transactionType,
                            categoryId = req.categoryId ?: existing.categoryId,
                            accountId = req.accountId ?: existing.accountId,
                            targetCurrency = req.targetCurrency ?: existing.targetCurrency,
                            transferRate = req.transferRate?.let { BigDecimal.valueOf(it) }
                                ?: existing.transferRate,
                            note = req.note ?: existing.note,
                            payee = req.payee ?: existing.payee,
                            location = req.location ?: existing.location,
                            gpsLocation = req.gpsLocation ?: existing.gpsLocation,
                            warrantyExpiration = req.warrantyExpiration
                                ?: existing.warrantyExpiration,
                            transactionStatus = req.transactionStatus?.let { s ->
                                TransactionStatus.entries.find {
                                    it.name.equals(
                                        s,
                                        ignoreCase = true
                                    )
                                } ?: existing.transactionStatus
                            } ?: existing.transactionStatus,
                            paymentMethod = req.paymentMethod?.let { m ->
                                PaymentMethod.entries.find { it.name.equals(m, ignoreCase = true) }
                            } ?: existing.paymentMethod,
                            tags = req.tags?.toSet() ?: existing.tags,
                            schedule = schedule,
                            autoConfirm = req.autoConfirm ?: existing.autoConfirm
                        )
                        plannedPaymentRepository.update(updated)
                        call.respond(ApiResponse(success = true, data = mapOf("updated" to id)))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                // ── Categories (edit) ──────────────────────────────────────────────────
                put("/categories/{id}") {
                    if (!authenticate(authSessionManager)) return@put
                    try {
                        val id = call.parameters["id"]?.toLongOrNull()
                            ?: return@put call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<Unit>(success = false, error = "Invalid id")
                            )
                        val existing =
                            categoryRepository.getAllCategoriesSync().find { it.id == id }
                                ?: return@put call.respond(
                                    HttpStatusCode.NotFound,
                                    ApiResponse<Unit>(success = false, error = "Category not found")
                                )
                        val req = call.receive<UpdateCategoryRequest>()
                        val updated = existing.copy(
                            name = req.name ?: existing.name,
                            iconName = req.iconName ?: existing.iconName,
                            colorHex = req.colorHex ?: existing.colorHex,
                            nature = req.nature?.let { n ->
                                CategoryNature.entries.find { it.name.equals(n, ignoreCase = true) }
                                    ?: existing.nature
                            } ?: existing.nature,
                            parentId = req.parentId ?: existing.parentId
                        )
                        categoryRepository.upsertCategory(updated)
                        call.respond(ApiResponse(success = true, data = updated.toDto()))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                // ── Tags (create + rename) ─────────────────────────────────────────────
                post("/tags") {
                    if (!authenticate(authSessionManager)) return@post
                    try {
                        val req = call.receive<CreateTagRequest>()
                        val allTags = tagRepository.getAllTags().first()
                        if (allTags.any { it.name.equals(req.name, ignoreCase = true) }) {
                            call.respond(
                                HttpStatusCode.Conflict,
                                ApiResponse<Unit>(success = false, error = "Tag already exists")
                            )
                            return@post
                        }
                        val tag = Tag(name = req.name, colorHex = req.colorHex)
                        tagRepository.insertTag(tag)
                        call.respond(
                            HttpStatusCode.Created,
                            ApiResponse(
                                success = true,
                                data = TagDto(
                                    name = tag.name,
                                    colorHex = tag.colorHex,
                                    transactionCount = 0
                                )
                            )
                        )
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                put("/tags/{name}") {
                    if (!authenticate(authSessionManager)) return@put
                    try {
                        val name = call.parameters["name"]
                            ?: return@put call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<Unit>(success = false, error = "Invalid name")
                            )
                        val allTags = tagRepository.getAllTags().first()
                        val existing = allTags.find { it.name == name }
                            ?: return@put call.respond(
                                HttpStatusCode.NotFound,
                                ApiResponse<Unit>(success = false, error = "Tag not found")
                            )
                        val req = call.receive<RenameTagRequest>()
                        val updated = existing.copy(
                            name = req.newName,
                            colorHex = req.colorHex ?: existing.colorHex
                        )
                        tagRepository.updateTag(existing.name, updated)
                        call.respond(
                            ApiResponse(
                                success = true,
                                data = TagDto(
                                    name = updated.name,
                                    colorHex = updated.colorHex,
                                    transactionCount = 0
                                )
                            )
                        )
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                // ── Saved Filters ─────────────────────────────────────────────────────
                get("/filters") {
                    if (!authenticate(authSessionManager)) return@get
                    try {
                        val filters = filterRepository.getAllFilters().first()
                        val dtos = filters.map { it.toDto() }
                        call.respond(ApiResponse(success = true, data = dtos))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                get("/filters/{id}") {
                    if (!authenticate(authSessionManager)) return@get
                    try {
                        val id = call.parameters["id"]?.toLongOrNull()
                            ?: return@get call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<Unit>(success = false, error = "Invalid id")
                            )
                        val filter = filterRepository.getFilterById(id)
                            ?: return@get call.respond(
                                HttpStatusCode.NotFound,
                                ApiResponse<Unit>(success = false, error = "Filter not found")
                            )
                        call.respond(ApiResponse(success = true, data = filter.toDto()))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                post("/filters") {
                    if (!authenticate(authSessionManager)) return@post
                    try {
                        val req = call.receive<CreateFilterRequest>()
                        val transactionFilter = req.toTransactionFilter()
                        val filter = Filter(name = req.name, filter = transactionFilter)
                        val newId = filterRepository.insertFilter(filter)
                        call.respond(
                            HttpStatusCode.Created,
                            ApiResponse(success = true, data = mapOf("id" to newId))
                        )
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                put("/filters/{id}") {
                    if (!authenticate(authSessionManager)) return@put
                    try {
                        val id = call.parameters["id"]?.toLongOrNull()
                            ?: return@put call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<Unit>(success = false, error = "Invalid id")
                            )
                        filterRepository.getFilterById(id)
                            ?: return@put call.respond(
                                HttpStatusCode.NotFound,
                                ApiResponse<Unit>(success = false, error = "Filter not found")
                            )
                        val req = call.receive<CreateFilterRequest>()
                        val transactionFilter = req.toTransactionFilter()
                        val updated = Filter(id = id, name = req.name, filter = transactionFilter)
                        filterRepository.updateFilter(updated)
                        call.respond(ApiResponse(success = true, data = mapOf("updated" to id)))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                delete("/filters/{id}") {
                    if (!authenticate(authSessionManager)) return@delete
                    try {
                        val id = call.parameters["id"]?.toLongOrNull()
                            ?: return@delete call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<Unit>(success = false, error = "Invalid id")
                            )
                        val filter = filterRepository.getFilterById(id)
                            ?: return@delete call.respond(
                                HttpStatusCode.NotFound,
                                ApiResponse<Unit>(success = false, error = "Filter not found")
                            )
                        filterRepository.deleteFilter(filter)
                        call.respond(ApiResponse(success = true, data = mapOf("deleted" to id)))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
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
                        call.respond(
                            ApiResponse(
                                success = true, data = SettingsDto(
                                    currentProfileId = profileId,
                                    baseCurrency = baseCurrency,
                                    decimalPrecision = precision
                                )
                            )
                        )
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }
                put("/settings") {
                    if (!authenticate(authSessionManager)) return@put
                    try {
                        val req = call.receive<UpdateSettingsRequest>()
                        req.baseCurrency?.let { settingsRepository.saveCurrency(it) }
                        req.decimalPrecision?.let { settingsRepository.saveDecimalPrecision(it) }
                        req.theme?.let { settingsRepository.saveTheme(it) }
                        req.selectedFont?.let { settingsRepository.saveSelectedFont(it) }
                        req.use24HourFormat?.let { settingsRepository.saveUse24HourFormat(it) }
                        req.userName?.let { settingsRepository.saveUserName(it) }
                        req.mapsAccessEnabled?.let { settingsRepository.saveMapsAccessEnabled(it) }
                        req.autoSaveExchangeRates?.let {
                            settingsRepository.saveAutoSaveExchangeRates(
                                it
                            )
                        }
                        req.cardAnimationEnabled?.let {
                            settingsRepository.saveCardAnimationEnabled(
                                it
                            )
                        }
                        req.currencyProvider?.let { settingsRepository.saveCurrencyProvider(it) }
                        val profileId = settingsRepository.currentProfileId.first()
                        val baseCurrency = settingsRepository.currency.first()
                        val precision = settingsRepository.decimalPrecision.first()
                        call.respond(
                            ApiResponse(
                                success = true, data = SettingsDto(
                                    currentProfileId = profileId,
                                    baseCurrency = baseCurrency,
                                    decimalPrecision = precision
                                )
                            )
                        )
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                // ── Templates ─────────────────────────────────────────────────────────
                get("/templates") {
                    if (!authenticate(authSessionManager)) return@get
                    try {
                        val templates = templateRepository.getAllTemplatesSync()
                        val dtos = templates.map { it.toTemplateDto() }
                        call.respond(ApiResponse(success = true, data = dtos))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                post("/templates") {
                    if (!authenticate(authSessionManager)) return@post
                    try {
                        val profileId = settingsRepository.currentProfileId.first()
                        val req = call.receive<CreateTemplateRequest>()
                        val template = Template(
                            name = req.name,
                            categoryId = req.categoryId,
                            amount = BigDecimal.valueOf(req.amount),
                            transactionType = TransactionType.entries.find {
                                it.name.equals(
                                    req.transactionType,
                                    ignoreCase = true
                                )
                            }
                                ?: TransactionType.EXPENSE,
                            currency = req.currency,
                            accountId = req.accountId,
                            targetCurrency = req.targetCurrency,
                            transferRate = BigDecimal.valueOf(req.transferRate),
                            note = req.note,
                            payee = req.payee,
                            location = req.location,
                            gpsLocation = req.gpsLocation,
                            warrantyExpiration = req.warrantyExpiration,
                            transactionStatus = TransactionStatus.entries.find {
                                it.name.equals(
                                    req.transactionStatus,
                                    ignoreCase = true
                                )
                            }
                                ?: TransactionStatus.CLEARED,
                            paymentMethod = req.paymentMethod?.let { m ->
                                PaymentMethod.entries.find { it.name.equals(m, ignoreCase = true) }
                            },
                            tags = req.tags.toSet(),
                            icon = req.icon,
                            colorHex = req.colorHex,
                            isStarred = req.isStarred,
                            displayOrder = req.displayOrder,
                            profileId = profileId
                        )
                        val newId = templateRepository.insertTemplate(template)
                        call.respond(
                            HttpStatusCode.Created,
                            ApiResponse(success = true, data = mapOf("id" to newId))
                        )
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                get("/templates/{id}") {
                    if (!authenticate(authSessionManager)) return@get
                    try {
                        val id = call.parameters["id"]?.toLongOrNull()
                            ?: return@get call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<Unit>(success = false, error = "Invalid id")
                            )
                        val template = templateRepository.getTemplateById(id)
                            ?: return@get call.respond(
                                HttpStatusCode.NotFound,
                                ApiResponse<Unit>(success = false, error = "Template not found")
                            )
                        call.respond(ApiResponse(success = true, data = template.toTemplateDto()))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                put("/templates/{id}") {
                    if (!authenticate(authSessionManager)) return@put
                    try {
                        val id = call.parameters["id"]?.toLongOrNull()
                            ?: return@put call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<Unit>(success = false, error = "Invalid id")
                            )
                        val existing = templateRepository.getTemplateById(id)
                            ?: return@put call.respond(
                                HttpStatusCode.NotFound,
                                ApiResponse<Unit>(success = false, error = "Template not found")
                            )
                        val req = call.receive<UpdateTemplateRequest>()
                        val updated = existing.copy(
                            name = req.name ?: existing.name,
                            categoryId = req.categoryId ?: existing.categoryId,
                            amount = req.amount?.let { BigDecimal.valueOf(it) } ?: existing.amount,
                            transactionType = req.transactionType?.let { t ->
                                TransactionType.entries.find {
                                    it.name.equals(
                                        t,
                                        ignoreCase = true
                                    )
                                } ?: existing.transactionType
                            } ?: existing.transactionType,
                            currency = req.currency ?: existing.currency,
                            accountId = req.accountId ?: existing.accountId,
                            targetCurrency = req.targetCurrency ?: existing.targetCurrency,
                            transferRate = req.transferRate?.let { BigDecimal.valueOf(it) }
                                ?: existing.transferRate,
                            note = req.note ?: existing.note,
                            payee = req.payee ?: existing.payee,
                            location = req.location ?: existing.location,
                            gpsLocation = req.gpsLocation ?: existing.gpsLocation,
                            warrantyExpiration = req.warrantyExpiration
                                ?: existing.warrantyExpiration,
                            transactionStatus = req.transactionStatus?.let { s ->
                                TransactionStatus.entries.find {
                                    it.name.equals(
                                        s,
                                        ignoreCase = true
                                    )
                                } ?: existing.transactionStatus
                            } ?: existing.transactionStatus,
                            paymentMethod = req.paymentMethod?.let { m ->
                                PaymentMethod.entries.find { it.name.equals(m, ignoreCase = true) }
                            } ?: existing.paymentMethod,
                            tags = req.tags?.toSet() ?: existing.tags,
                            icon = req.icon ?: existing.icon,
                            colorHex = req.colorHex ?: existing.colorHex,
                            isStarred = req.isStarred ?: existing.isStarred,
                            displayOrder = req.displayOrder ?: existing.displayOrder
                        )
                        templateRepository.updateTemplate(updated)
                        call.respond(ApiResponse(success = true, data = updated.toTemplateDto()))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                delete("/templates/{id}") {
                    if (!authenticate(authSessionManager)) return@delete
                    try {
                        val id = call.parameters["id"]?.toLongOrNull()
                            ?: return@delete call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<Unit>(success = false, error = "Invalid id")
                            )
                        val template = templateRepository.getTemplateById(id)
                            ?: return@delete call.respond(
                                HttpStatusCode.NotFound,
                                ApiResponse<Unit>(success = false, error = "Template not found")
                            )
                        templateRepository.deleteTemplate(template)
                        call.respond(ApiResponse(success = true, data = mapOf("deleted" to id)))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                // ── Dashboard card management ─────────────────────────────────────────
                post("/dashboard/cards") {
                    if (!authenticate(authSessionManager)) return@post
                    try {
                        val req = call.receive<AddCardRequest>()
                        val cardType = CardType.entries.find {
                            it.name.equals(
                                req.cardType,
                                ignoreCase = true
                            ) || it.serialName.equals(req.cardType, ignoreCase = true)
                        }
                            ?: return@post call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<Unit>(
                                    success = false,
                                    error = "Unknown cardType: ${req.cardType}"
                                )
                            )
                        val currentConfig = cardRepository.dashboardCardsConfig.first()
                        val existingCount = currentConfig.cards.size
                        val newCard = cardType.toDefaultCardConfig(existingCount)
                        cardRepository.addDashboardCard(newCard)
                        call.respond(
                            HttpStatusCode.Created,
                            ApiResponse(success = true, data = mapOf("cardId" to newCard.cardId))
                        )
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                delete("/dashboard/cards/{cardId}") {
                    if (!authenticate(authSessionManager)) return@delete
                    try {
                        val cardId = call.parameters["cardId"]
                            ?: return@delete call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<Unit>(success = false, error = "Invalid cardId")
                            )
                        cardRepository.removeDashboardCard(cardId)
                        call.respond(ApiResponse(success = true, data = mapOf("deleted" to cardId)))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                put("/dashboard/cards/{cardId}") {
                    if (!authenticate(authSessionManager)) return@put
                    try {
                        val cardId = call.parameters["cardId"]
                            ?: return@put call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<Unit>(success = false, error = "Invalid cardId")
                            )
                        val req = call.receive<CardFilterUpdateDto>()
                        val timelineRange: TimelineRange = when (req.timelineType?.uppercase()) {
                            "MONTH", "MONTHLY" -> TimelineRange.ThisPeriod(ThisPeriodType.MONTH)
                            "YEAR", "YEARLY"   -> TimelineRange.ThisPeriod(ThisPeriodType.YEAR)
                            "WEEK", "WEEKLY"   -> TimelineRange.ThisPeriod(ThisPeriodType.WEEK)
                            "TODAY"            -> TimelineRange.ThisPeriod(ThisPeriodType.TODAY)
                            "QUARTER"          -> TimelineRange.ThisPeriod(ThisPeriodType.QUARTER)
                            "HALF_YEAR"        -> TimelineRange.ThisPeriod(ThisPeriodType.HALF_YEAR)
                            "LAST_24H"         -> TimelineRange.LastPeriod(LastPeriodType.LAST_24H)
                            "LAST_7_DAYS"      -> TimelineRange.LastPeriod(LastPeriodType.LAST_7_DAYS)
                            "LAST_30_DAYS"     -> TimelineRange.LastPeriod(LastPeriodType.LAST_30_DAYS)
                            "LAST_12_WEEKS"    -> TimelineRange.LastPeriod(LastPeriodType.LAST_12_WEEKS)
                            "LAST_6_MONTHS"    -> TimelineRange.LastPeriod(LastPeriodType.LAST_6_MONTHS)
                            "LAST_1_YEAR"      -> TimelineRange.LastPeriod(LastPeriodType.LAST_1_YEAR)
                            "LAST_5_YEARS"     -> TimelineRange.LastPeriod(LastPeriodType.LAST_5_YEARS)
                            "CUSTOM"           -> if (req.dateFrom != null && req.dateTo != null)
                                TimelineRange.CustomRange(req.dateFrom, req.dateTo)
                            else TimelineRange.AllTime
                            "ALL_TIME"         -> TimelineRange.AllTime
                            else               -> TimelineRange.AllTime
                        }
                        val newFilter = TransactionFilter(
                            timelineRange = timelineRange,
                            accountIds = req.accountIds.toSet(),
                            categoryIds = req.categoryIds.toSet(),
                            transactionTypes = req.transactionTypes.mapNotNull {
                                runCatching { TransactionType.valueOf(it.uppercase()) }.getOrNull()
                            }.toSet(),
                            tags = req.tags.toSet(),
                            currencies = req.currencies.toSet(),
                            statuses = req.statuses.mapNotNull {
                                runCatching { TransactionStatus.valueOf(it.uppercase()) }.getOrNull()
                            }.toSet(),
                            paymentMethods = req.paymentMethods.mapNotNull {
                                runCatching { PaymentMethod.valueOf(it.uppercase()) }.getOrNull()
                            }.toSet(),
                            minAmount = req.minAmount?.let { BigDecimal.valueOf(it) },
                            maxAmount = req.maxAmount?.let { BigDecimal.valueOf(it) },
                            queryNote = req.queryNote,
                            queryPayee = req.queryPayee,
                            excludeFuture = req.excludeFuture
                        )
                        cardRepository.updateDashboardCard(cardId) { config ->
                            when (config) {
                                is com.osscabinet.ossWallet.domain.model.settings.NetWorthCardConfig -> config.copy(filter = newFilter)
                                is com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig -> config.copy(filter = newFilter)
                                is com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig -> config.copy(filter = newFilter)
                                is com.osscabinet.ossWallet.domain.model.settings.AccountsCardConfig -> config.copy(filter = newFilter)
                                is com.osscabinet.ossWallet.domain.model.settings.CategoryPieChartCardConfig -> config.copy(filter = newFilter)
                                is com.osscabinet.ossWallet.domain.model.settings.CategoryTreemapCardConfig -> config.copy(filter = newFilter)
                                is com.osscabinet.ossWallet.domain.model.settings.AccountHeroCardConfig -> config.copy(filter = newFilter)
                                is com.osscabinet.ossWallet.domain.model.settings.PlannedPaymentsCardConfig -> config.copy(filter = newFilter)
                                is com.osscabinet.ossWallet.domain.model.settings.BudgetsCardConfig -> config.copy(filter = newFilter)
                                is com.osscabinet.ossWallet.domain.model.settings.LoansCardConfig -> config.copy(filter = newFilter)
                                is com.osscabinet.ossWallet.domain.model.settings.RecentActivitiesCardConfig -> config.copy(filter = newFilter)
                                is com.osscabinet.ossWallet.domain.model.settings.CashFlowComparisonCardConfig -> config.copy(filter = newFilter)
                                is com.osscabinet.ossWallet.domain.model.settings.CategorySpendingTrendCardConfig -> config.copy(filter = newFilter)
                                is com.osscabinet.ossWallet.domain.model.settings.IncomeExpenseBarCardConfig -> config.copy(filter = newFilter)
                                is com.osscabinet.ossWallet.domain.model.settings.TagUpSetPlotCardConfig -> config.copy(filter = newFilter)
                                else -> config
                            }
                        }
                        call.respond(ApiResponse(success = true, data = mapOf("updated" to cardId)))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(success = false, error = e.message ?: "Internal error")
                        )
                    }
                }

                put("/dashboard/cards/reorder") {
                    if (!authenticate(authSessionManager)) return@put
                    try {
                        val req = call.receive<ReorderCardsRequest>()
                        val currentConfig = cardRepository.dashboardCardsConfig.first()
                        val cardMap = currentConfig.cards.associateBy { it.cardId }
                        val reordered = req.cardIds.mapIndexedNotNull { index, cardId ->
                            cardMap[cardId]?.let { it.withOrder(index) }
                        }
                        cardRepository.reorderDashboardCards(reordered)
                        call.respond(
                            ApiResponse(
                                success = true,
                                data = mapOf("reordered" to reordered.size)
                            )
                        )
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                // ── Transaction clone ─────────────────────────────────────────────────
                post("/transactions/{id}/clone") {
                    if (!authenticate(authSessionManager)) return@post
                    try {
                        val profileId = settingsRepository.currentProfileId.first()
                        val id = call.parameters["id"]?.toLongOrNull()
                            ?: return@post call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<Unit>(success = false, error = "Invalid id")
                            )
                        val original = transactionRepository.getTransactionById(id)
                            ?: return@post call.respond(
                                HttpStatusCode.NotFound,
                                ApiResponse<Unit>(success = false, error = "Transaction not found")
                            )
                        val clone = original.copy(id = 0, timestamp = System.currentTimeMillis())
                        val newId = transactionRepository.insertTransaction(clone, profileId)
                        call.respond(
                            HttpStatusCode.Created,
                            ApiResponse(success = true, data = mapOf("id" to newId))
                        )
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                // ── Account reorder ───────────────────────────────────────────────────
                put("/accounts/reorder") {
                    if (!authenticate(authSessionManager)) return@put
                    try {
                        val profileId = settingsRepository.currentProfileId.first()
                        val req = call.receive<ReorderAccountsRequest>()
                        val allAccounts = accountRepository.getAllAccountsSync(profileId)
                        val accountMap = allAccounts.associateBy { it.id }
                        val reordered = req.accountIds.mapNotNull { accountMap[it] }
                        accountRepository.updateAccounts(reordered, profileId)
                        call.respond(
                            ApiResponse(
                                success = true,
                                data = mapOf("reordered" to reordered.size)
                            )
                        )
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                // ── Loan payment delete ───────────────────────────────────────────────
                delete("/loans/{loanId}/payments/{paymentId}") {
                    if (!authenticate(authSessionManager)) return@delete
                    try {
                        val paymentId = call.parameters["paymentId"]?.toLongOrNull()
                            ?: return@delete call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<Unit>(success = false, error = "Invalid paymentId")
                            )
                        loanRepository.deletePayment(paymentId)
                        call.respond(
                            ApiResponse(
                                success = true,
                                data = mapOf("deleted" to paymentId)
                            )
                        )
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
                        )
                    }
                }

                // ── Category reorder ──────────────────────────────────────────────────
                put("/categories/reorder") {
                    if (!authenticate(authSessionManager)) return@put
                    try {
                        val req = call.receive<ReorderCategoriesRequest>()
                        val allCategories = categoryRepository.getAllCategoriesSync()
                        val categoryMap = allCategories.associateBy { it.id }
                        req.categoryIds.forEachIndexed { index, catId ->
                            val cat = categoryMap[catId] ?: return@forEachIndexed
                            categoryRepository.upsertCategory(cat)
                        }
                        call.respond(
                            ApiResponse(
                                success = true,
                                data = mapOf("reordered" to req.categoryIds.size)
                            )
                        )
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                error = e.message ?: "Internal error"
                            )
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
            CashFlowPointDto(
                label = label,
                epochMillis = epoch,
                income = income,
                expense = expense,
                net = income - expense
            )
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

    private fun TransactionFilter.toFilterSummaryDto(): FilterSummaryDto {
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

    private fun CardRenderData.toPayloadDto(
        now: Long,
        categoryMap: Map<Long, Category>,
        accountMap: Map<String, Account>,
        precision: Int,
        baseCurrency: String
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
                transactions = txList.take(20).map { uiModel ->
                    val tx = uiModel.item.transaction
                    TransactionDto(
                        id = tx.id, title = tx.name,
                        amount = tx.amount.toDouble(),
                        currency = tx.currency,
                        type = tx.transactionType.name,
                        categoryId = tx.categoryId,
                        categoryName = uiModel.categoryName.takeIf { it.isNotBlank() },
                        accountId = tx.accountId,
                        accountName = uiModel.accountName,
                        date = tx.timestamp,
                        note = tx.note.takeIf { it.isNotBlank() },
                        payee = tx.payee.takeIf { it.isNotBlank() },
                        tags = tx.tags.toList(),
                        status = tx.status.name,
                        paymentMethod = tx.paymentMethod?.name,
                        warrantyExpiration = tx.warrantyExpiration,
                        location = tx.location,
                    )
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

    private fun Filter.toDto(): FilterDto {
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

    private fun CreateFilterRequest.toTransactionFilter(): TransactionFilter {
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

    private fun Template.toTemplateDto() = TemplateDto(
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

    private fun CardType.toDefaultCardConfig(order: Int): CardConfig {
        val cardId = "${serialName}_${System.currentTimeMillis()}"
        return CardConfig.createDefault(
            type = this,
            context = ScreenContext.DASHBOARD,
            cardId = cardId
        )
    }

    private fun CardConfig.withOrder(newOrder: Int): CardConfig = when (this) {
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

    private fun loadIndexHtml(): String {
        return try {
            context.assets.open("web/index.html").bufferedReader().readText()
        } catch (e: Exception) {
            "<html><body>Web UI not found. Rebuild the app.</body></html>"
        }
    }
}

private fun com.osscabinet.ossWallet.domain.model.Transaction.toDto(
    categoryMap: Map<Long, com.osscabinet.ossWallet.domain.model.Category>,
    accountMap: Map<String, com.osscabinet.ossWallet.domain.model.Account>
) = com.osscabinet.ossWallet.data.web.dto.TransactionDto(
    id = id,
    title = name,
    amount = amount.toDouble(),
    currency = currency,
    type = transactionType.name,
    categoryId = categoryId,
    categoryName = categoryMap[categoryId]?.name,
    accountId = accountId,
    accountName = accountMap[accountId]?.name ?: "",
    date = timestamp,
    note = note.takeIf { it.isNotBlank() },
    payee = payee.takeIf { it.isNotBlank() },
    tags = tags.toList(),
    status = status.name,
    paymentMethod = paymentMethod?.name,
    warrantyExpiration = warrantyExpiration,
    location = location
)
