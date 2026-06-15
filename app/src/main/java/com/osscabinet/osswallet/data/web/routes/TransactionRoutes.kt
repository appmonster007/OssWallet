package com.osscabinet.ossWallet.data.web.routes

import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.data.web.AuthSessionManager
import com.osscabinet.ossWallet.data.web.authenticate
import com.osscabinet.ossWallet.data.web.dto.*
import com.osscabinet.ossWallet.data.web.toDto
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.domain.model.dto.TransactionRequest
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.PlannedPaymentRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import com.osscabinet.ossWallet.domain.service.filter.TransactionFilterService
import com.osscabinet.ossWallet.domain.service.management.PlannedPaymentService
import com.osscabinet.ossWallet.domain.service.management.TransactionManager
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.util.*

fun Route.transactionRoutes(
    authSessionManager: AuthSessionManager,
    transactionRepository: TransactionRepository,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
    settingsRepository: SettingsRepository,
    plannedPaymentRepository: PlannedPaymentRepository,
    transactionManager: TransactionManager,
    transactionUseCases: TransactionUseCases
) {
    route("/transactions") {
        get {
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
                val includePlanned = call.request.queryParameters["includePlanned"]?.toBoolean() ?: true
                val excludeFuture = call.request.queryParameters["excludeFuture"]?.toBoolean() ?: false

                val allTransactions =
                    transactionRepository.getAllTransactionsSync(profileId)
                val categoryMap =
                    categoryRepository.getAllCategoriesSync().associateBy { it.id }
                val accountMap =
                    accountRepository.getAllAccountsSync(profileId).associateBy { it.id }

                // Build filter from query params
                val timelineRange = if (dateFrom != null || dateTo != null) {
                    TimelineRange.CustomRange(start = dateFrom, end = dateTo, excludeFuture = excludeFuture)
                } else {
                    TimelineRange.AllTime(excludeFuture = excludeFuture)
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
                    queryPayee = payeeParam
                )

                val filtered =
                    TransactionFilterService.filterTransactions(allTransactions, filter)

                val transactionDtos = filtered.map { tx ->
                    tx.toDto(categoryMap, accountMap)
                }.toMutableList()

                if (includePlanned) {
                    val now = System.currentTimeMillis()
                    val (startTime, endTime) = TimelineService.getDateRange(filter.timelineRange)
                    val limitDate = now + 365L * 24 * 60 * 60 * 1000
                    val effectiveEndTime = when {
                        endTime != null -> minOf(endTime, limitDate)
                        else -> limitDate
                    }

                    val payments = plannedPaymentRepository.getActivePlannedPayments(profileId).first()
                    val occurrences = PlannedPaymentService.generateUpcoming(
                        payments = payments,
                        fromDate = startTime ?: minOf(now, payments.minOfOrNull { it.schedule.nextDueDate } ?: now),
                        daysAhead = ((effectiveEndTime - (startTime ?: now)) / (24 * 60 * 60 * 1000)).toInt().coerceAtLeast(0)
                    )

                    val filteredOccurrences = if (excludeFuture) {
                        occurrences.filter { it.scheduledDate <= now }
                    } else {
                        occurrences
                    }

                    val plannedDtos = filteredOccurrences.map { item ->
                        val pp = item.plannedPayment
                        TransactionDto(
                            id = 0, // Virtual item
                            title = pp.name,
                            amount = pp.amount.toDouble(),
                            currency = pp.currency,
                            type = pp.entryMode.name,
                            categoryId = pp.categoryId,
                            categoryName = categoryMap[pp.categoryId]?.name,
                            accountId = pp.accountId,
                            accountName = accountMap[pp.accountId]?.name ?: "",
                            date = item.scheduledDate,
                            note = pp.note,
                            payee = pp.payee,
                            tags = pp.tags.toList(),
                            status = pp.transactionStatus.name,
                            paymentMethod = pp.paymentMethod?.name,
                            isPlanned = true,
                            isOverdue = item.isOverdue,
                            occurrenceId = item.occurrenceId.toString(),
                            paymentId = pp.id
                        )
                    }
                    transactionDtos.addAll(plannedDtos)
                }

                val sorted = transactionDtos.sortedByDescending { it.date }
                val paged = sorted.drop(page * limit).take(limit)

                call.respond(
                    ApiResponse(
                        success = true,
                        data = PagedResponse(
                            items = paged,
                            total = sorted.size,
                            page = page,
                            limit = limit,
                            hasMore = page * limit + limit < sorted.size
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

        post {
            if (!authenticate(authSessionManager)) return@post
            try {
                val profileId = settingsRepository.currentProfileId.first()
                val req = call.receive<CreateTransactionRequest>()

                val txType = EntryMode.entries.find {
                    it.name.equals(req.transactionType, ignoreCase = true)
                } ?: EntryMode.EXPENSE

                val domainRequest = TransactionRequest(
                    name = req.name,
                    amount = BigDecimal.valueOf(req.amount),
                    timestamp = req.timestamp,
                    currency = req.currency,
                    entryMode = txType,
                    accountId = req.accountId,
                    categoryId = req.categoryId,
                    tags = req.tags.toSet(),
                    paymentMethod = PaymentMethod.entries.find {
                        it.name.equals(req.paymentMethod, ignoreCase = true)
                    } ?: PaymentMethod.CASH,
                    payee = req.payee,
                    note = req.note,
                    status = TransactionStatus.entries.find {
                        it.name.equals(req.status, ignoreCase = true)
                    } ?: TransactionStatus.CLEARED,
                    gpsLocation = req.gpsLocation,
                    location = req.location,
                    warrantyExpiration = req.warrantyExpiration,
                    targetAccountId = req.targetAccountId,
                    targetCurrency = req.targetCurrency,
                    transferRate = BigDecimal.valueOf(req.transferRate)
                )

                val allCategories = categoryRepository.getAllCategoriesSync()

                val newId = if (req.splits.isNotEmpty()) {
                    transactionManager.handleSaveSplitTransactions(
                        existingTransactionId = null,
                        splits = req.splits.map {
                            TransactionSplit(
                                categoryId = it.categoryId,
                                amount = it.amount.toString(),
                                note = it.note
                            )
                        },
                        baseRequest = domainRequest,
                        profileId = profileId,
                        categoryNameLookup = { catId ->
                            allCategories.find { it.id == catId }?.name ?: "Split"
                        }
                    )
                } else {
                    transactionManager.handleSaveTransaction(
                        existingTransaction = null,
                        request = domainRequest,
                        profileId = profileId,
                        linkedTransactionIds = req.linkedTransactionIds
                    )
                }

                val created = transactionUseCases.getTransactionById(newId)
                val categoryMap = allCategories.associateBy { it.id }
                val accountMap = accountRepository.getAllAccountsSync(profileId).associateBy { it.id }

                if (created != null) {
                    call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = created.toDto(categoryMap, accountMap)))
                } else {
                    call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = mapOf("id" to newId)))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = e.message ?: "Internal error"))
            }
        }

        get("/{id}") {
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
                val old = transactionUseCases.getTransactionById(id)
                    ?: return@put call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse<Unit>(success = false, error = "Transaction not found")
                    )
                val req = call.receive<UpdateTransactionRequest>()

                val txType = req.transactionType?.let {
                    EntryMode.entries.find { t -> t.name.equals(it, ignoreCase = true) }
                } ?: old.transactionType.toEntryMode()

                val domainRequest = TransactionRequest(
                    name = req.name ?: old.name,
                    amount = req.amount?.let { BigDecimal.valueOf(it) } ?: old.amount,
                    timestamp = req.timestamp ?: old.timestamp,
                    currency = req.currency ?: old.currency,
                    entryMode = txType,
                    accountId = req.accountId ?: old.accountId,
                    categoryId = req.categoryId ?: old.categoryId,
                    tags = req.tags?.toSet() ?: old.tags,
                    paymentMethod = req.paymentMethod?.let {
                        PaymentMethod.entries.find { p -> p.name.equals(it, ignoreCase = true) }
                    } ?: old.paymentMethod,
                    payee = req.payee ?: old.payee,
                    note = req.note ?: old.note,
                    status = req.status?.let {
                        TransactionStatus.entries.find { s -> s.name.equals(it, ignoreCase = true) }
                    } ?: old.status,
                    gpsLocation = req.gpsLocation ?: old.gpsLocation,
                    location = req.location ?: old.location,
                    warrantyExpiration = req.warrantyExpiration ?: old.warrantyExpiration,
                    targetAccountId = req.targetAccountId ?: old.targetAccountId,
                    targetCurrency = req.targetCurrency ?: old.targetCurrency,
                    transferRate = req.transferRate?.let { BigDecimal.valueOf(it) } ?: old.transferRate
                )

                if (!req.splits.isNullOrEmpty()) {
                    val allCategories = categoryRepository.getAllCategoriesSync()
                    transactionManager.handleSaveSplitTransactions(
                        existingTransactionId = id,
                        splits = req.splits.map {
                            TransactionSplit(
                                categoryId = it.categoryId,
                                amount = it.amount.toString(),
                                note = it.note
                            )
                        },
                        baseRequest = domainRequest,
                        profileId = profileId,
                        categoryNameLookup = { catId ->
                            allCategories.find { it.id == catId }?.name ?: "Split"
                        }
                    )
                } else {
                    transactionManager.handleSaveTransaction(
                        existingTransaction = old,
                        request = domainRequest,
                        profileId = profileId,
                        linkedTransactionIds = req.linkedTransactionIds ?: emptyList()
                    )
                }

                call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = "Updated"))
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
                transactionRepository.deleteTransaction(id)
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

        post("/{id}/clone") {
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
                        error = "Internal error"
                    )
                )
            }
        }
    }

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
                            TransactionType.TRANSFER_INFLOW, TransactionType.TRANSFER_OUTFLOW -> acc + tx.amount
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
                    error = "Internal error"
                )
            )
        }
    }
}
