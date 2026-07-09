package com.osscabinet.ossWallet.data.web.routes

import com.osscabinet.ossWallet.data.web.AuthSessionManager
import com.osscabinet.ossWallet.data.web.authenticate
import com.osscabinet.ossWallet.data.web.dto.*
import com.osscabinet.ossWallet.data.web.toDefaultCardConfig
import com.osscabinet.ossWallet.data.web.toFilterSummaryDto
import com.osscabinet.ossWallet.data.web.toPayloadDto
import com.osscabinet.ossWallet.data.web.withOrder
import com.osscabinet.ossWallet.data.web.toDomain
import com.osscabinet.ossWallet.data.web.withFilter
import com.osscabinet.ossWallet.domain.card.CardComputerRegistry
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.model.settings.ScreenContext
import com.osscabinet.ossWallet.domain.repository.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.first
import java.math.BigDecimal

fun Route.dashboardRoutes(
    authSessionManager: AuthSessionManager,
    cardRepository: CardRepository,
    cardComputerRegistry: CardComputerRegistry,
    settingsRepository: SettingsRepository,
    categoryRepository: CategoryRepository,
    accountRepository: AccountRepository,
    transactionRepository: TransactionRepository,
    tagRepository: TagRepository,
    budgetRepository: BudgetRepository,
    loanRepository: LoanRepository,
    plannedPaymentRepository: PlannedPaymentRepository,
    currencyRepository: CurrencyRepository
) {
    route("/dashboard") {
        get {
            if (!authenticate(authSessionManager)) return@get
            println("WEB_API: Received GET /dashboard")
            try {
                val profileId = settingsRepository.currentProfileId.first()
                val baseCurrency = settingsRepository.currency.first()
                val now = System.currentTimeMillis()

                val version = transactionRepository.getLatestTransactionVersion(profileId).first() ?: 0L
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
                    transactionsVersion = version,
                    accounts = accounts,
                    categories = categories,
                    tags = allTags,
                    exchangeRates = com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot(
                        anchor = baseCurrency,
                        rates = exchangeRates
                    ),
                    targetCurrency = baseCurrency,
                    filter = TransactionFilter(timelineRange = TimelineRange.AllTime(excludeFuture = false)),
                    plannedPayments = allPayments,
                    budgets = budgets,
                    loans = loans,
                    loanPayments = loanPayments
                )

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
                            accountMap
                        ) ?: CardPayloadDto.Error("Card computation produced an error state")
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
                        error = "Internal error: ${e.message ?: e.toString()}"
                    )
                )
            }
        }

        post("/cards") {
            if (!authenticate(authSessionManager)) return@post
            try {
                val req = call.receive<AddCardRequest>()
                val type = CardType.entries.find { it.serialName == req.cardType }
                    ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, error = "Invalid card type")
                    )
                val config = type.toDefaultCardConfig()
                cardRepository.addDashboardCard(config)
                call.respond(
                    HttpStatusCode.Created,
                    ApiResponse(success = true, data = mapOf("id" to config.cardId))
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Unit>(
                        success = false,
                        error = "Internal error: ${e.message ?: e.toString()}"
                    )
                )
            }
        }

        delete("/cards/{id}") {
            if (!authenticate(authSessionManager)) return@delete
            try {
                val id = call.parameters["id"]
                    ?: return@delete call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, error = "Invalid id")
                    )
                cardRepository.removeDashboardCard(id)
                call.respond(ApiResponse(success = true, data = mapOf("deleted" to id)))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Unit>(
                        success = false,
                        error = "Internal error: ${e.message ?: e.toString()}"
                    )
                )
            }
        }

        put("/cards/reorder") {
            if (!authenticate(authSessionManager)) return@put
            try {
                val req = call.receive<ReorderCardsRequest>()
                val dashboardConfig = cardRepository.dashboardCardsConfig.first()
                val configs = dashboardConfig.cards
                val configMap = configs.associateBy { it.cardId }

                val updated = req.cardIds.mapIndexedNotNull { index, id ->
                    configMap[id]?.withOrder(index)
                }
                cardRepository.reorderDashboardCards(updated)
                call.respond(
                    ApiResponse(
                        success = true,
                        data = mapOf("reordered" to updated.size)
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Unit>(
                        success = false,
                        error = "Internal error: ${e.message ?: e.toString()}"
                    )
                )
            }
        }

        put("/cards/{id}") {
            if (!authenticate(authSessionManager)) return@put
            try {
                val id = call.parameters["id"]
                    ?: return@put call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, error = "Invalid id")
                    )
                val update = call.receive<CardFilterUpdateDto>()
                cardRepository.updateDashboardCard(id) { config ->
                    config.withFilter(update.toDomain())
                }

                call.respond(ApiResponse(success = true, data = mapOf("updated" to id)))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Unit>(
                        success = false,
                        error = "Internal error: ${e.message ?: e.toString()}"
                    )
                )
            }
        }
    }
}
