package com.osscabinet.ossWallet.data.web

import android.content.Context
import com.osscabinet.ossWallet.data.web.dto.*
import com.osscabinet.ossWallet.data.web.routes.*
import com.osscabinet.ossWallet.domain.card.CardComputerRegistry
import com.osscabinet.ossWallet.domain.repository.*
import com.osscabinet.ossWallet.domain.service.management.TransactionManager
import com.osscabinet.ossWallet.domain.service.security.AppLockGate
import com.osscabinet.ossWallet.domain.usecase.AccountUseCases
import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.*

class WebApiRoutes(
    private val context: Context,
    private val authSessionManager: AuthSessionManager,
    private val appLockGate: AppLockGate,
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
    private val templateRepository: TemplateRepository,
    private val transactionManager: TransactionManager,
    private val transactionUseCases: TransactionUseCases,
    private val accountUseCases: AccountUseCases
) {
    fun Application.installRoutes() {
        routing {
            // Static and Index
            get("/") {
                call.respondText(loadIndexHtml(), ContentType.Text.Html)
            }

            get("/web/{path...}") {
                val filePath = call.parameters.getAll("path")?.joinToString("/") ?: return@get
                if (filePath.contains("..")) {
                    call.respond(HttpStatusCode.Forbidden)
                    return@get
                }
                val bytes = runCatching {
                    context.assets.open("web/$filePath").readBytes()
                }.getOrElse {
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }
                val contentType = when {
                    filePath.endsWith(".css") -> ContentType.Text.CSS
                    filePath.endsWith(".js") -> ContentType.Text.JavaScript
                    else -> ContentType.Application.OctetStream
                }
                call.respondBytes(bytes, contentType)
            }

            // API Infrastructure
            get("/api/health") {
                call.respond(ApiResponse(success = true, data = mapOf("status" to "ok")))
            }

            post("/api/auth") {
                // The access code is only visible in the unlocked app; a locked app means nobody
                // has proven they hold the phone, so no new web session may start.
                if (appLockGate.isLockedNow()) {
                    call.respond(
                        HttpStatusCode.Locked,
                        ApiResponse<Unit>(success = false, error = "OssWallet is locked. Unlock the app on your phone, then try again.")
                    )
                    return@post
                }
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
                    call.respond(
                        HttpStatusCode.TooManyRequests,
                        ApiResponse<Unit>(success = false, error = "Rate limit exceeded. Try again later.")
                    )
                }
            }

            // Feature Routes
            route("/api") {
                accountRoutes(
                    authSessionManager,
                    accountRepository,
                    transactionRepository,
                    settingsRepository,
                    categoryRepository,
                    accountUseCases
                )
                transactionRoutes(
                    authSessionManager,
                    transactionRepository,
                    accountRepository,
                    categoryRepository,
                    settingsRepository,
                    plannedPaymentRepository,
                    transactionManager,
                    transactionUseCases
                )
                budgetRoutes(
                    authSessionManager,
                    budgetRepository,
                    transactionRepository,
                    categoryRepository,
                    settingsRepository,
                    currencyRepository
                )
                loanRoutes(
                    authSessionManager,
                    loanRepository,
                    settingsRepository
                )
                plannedPaymentRoutes(
                    authSessionManager,
                    plannedPaymentRepository,
                    accountRepository,
                    categoryRepository,
                    settingsRepository,
                    transactionManager
                )
                categoryRoutes(
                    authSessionManager,
                    categoryRepository
                )
                tagRoutes(
                    authSessionManager,
                    tagRepository,
                    settingsRepository
                )
                analyticsRoutes(
                    authSessionManager,
                    transactionRepository,
                    categoryRepository,
                    accountRepository,
                    tagRepository,
                    plannedPaymentRepository,
                    budgetRepository,
                    loanRepository,
                    currencyRepository,
                    settingsRepository,
                    cardRepository,
                    cardComputerRegistry
                )
                dashboardRoutes(
                    authSessionManager,
                    cardRepository,
                    cardComputerRegistry,
                    settingsRepository,
                    categoryRepository,
                    accountRepository,
                    transactionRepository,
                    tagRepository,
                    budgetRepository,
                    loanRepository,
                    plannedPaymentRepository,
                    currencyRepository
                )
                settingsRoutes(
                    authSessionManager,
                    settingsRepository
                )
                templateRoutes(
                    authSessionManager,
                    templateRepository,
                    settingsRepository
                )
                filterRoutes(
                    authSessionManager,
                    filterRepository,
                    settingsRepository
                )
            }
        }
    }

    private fun loadIndexHtml(): String {
        return try {
            context.assets.open("web/index.html").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "<html><body><h1>OssWallet Web Interface</h1><p>Error loading index.html: ${e.message}</p></body></html>"
        }
    }
}
