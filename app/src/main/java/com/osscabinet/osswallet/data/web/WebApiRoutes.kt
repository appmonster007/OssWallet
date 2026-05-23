package com.osscabinet.ossWallet.data.web

import android.content.Context
import com.osscabinet.ossWallet.data.web.dto.AccountDto
import com.osscabinet.ossWallet.data.web.dto.ApiResponse
import com.osscabinet.ossWallet.data.web.dto.AuthRequest
import com.osscabinet.ossWallet.data.web.dto.AuthResponse
import com.osscabinet.ossWallet.data.web.dto.MonthlySummaryDto
import com.osscabinet.ossWallet.data.web.dto.PagedResponse
import com.osscabinet.ossWallet.data.web.dto.TransactionDto
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.util.Calendar

class WebApiRoutes(
    private val context: Context,
    private val authSessionManager: AuthSessionManager,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository
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
                get("/accounts") {
                    if (!authenticate(authSessionManager)) return@get
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
                }

                get("/transactions") {
                    if (!authenticate(authSessionManager)) return@get
                    val profileId = settingsRepository.currentProfileId.first()
                    val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(0) ?: 0
                    val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 100) ?: 25
                    val accountFilter = call.request.queryParameters["accountId"]

                    val allTransactions = transactionRepository.getAllTransactionsSync(profileId)
                    val categoryMap = categoryRepository.getAllCategoriesSync().associateBy { it.id }
                    val accountMap = accountRepository.getAllAccountsSync(profileId).associateBy { it.id }

                    val filtered = if (accountFilter != null) {
                        allTransactions.filter { it.accountId == accountFilter }
                    } else {
                        allTransactions
                    }
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
                }

                get("/summary") {
                    if (!authenticate(authSessionManager)) return@get
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
                }
            }
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
