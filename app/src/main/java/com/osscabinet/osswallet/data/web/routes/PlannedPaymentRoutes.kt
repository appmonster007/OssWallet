package com.osscabinet.ossWallet.data.web.routes

import com.osscabinet.ossWallet.data.web.AuthSessionManager
import com.osscabinet.ossWallet.data.web.authenticate
import com.osscabinet.ossWallet.data.web.dto.ApiResponse
import com.osscabinet.ossWallet.data.web.dto.CreatePlannedPaymentRequest
import com.osscabinet.ossWallet.data.web.dto.UpdatePlannedPaymentRequest
import com.osscabinet.ossWallet.data.web.toDto
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.domain.model.dto.TransactionRequest
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.PlannedPaymentRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import com.osscabinet.ossWallet.domain.service.management.PlannedPaymentService
import com.osscabinet.ossWallet.domain.service.management.TransactionManager
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.first
import java.math.BigDecimal

fun Route.plannedPaymentRoutes(
    authSessionManager: AuthSessionManager,
    plannedPaymentRepository: PlannedPaymentRepository,
    transactionRepository: TransactionRepository,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
    settingsRepository: SettingsRepository,
    transactionManager: TransactionManager
) {
    route("/planned-payments") {
        get {
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
                        error = "Internal error"
                    )
                )
            }
        }

        get("/upcoming") {
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
                        error = "Internal error"
                    )
                )
            }
        }

        get("/overdue") {
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
                        error = "Internal error"
                    )
                )
            }
        }

        post {
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
                    targetAccountId = req.targetAccountId,
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
                plannedPaymentRepository.delete(id)
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

        post("/{id}/confirm") {
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
                        error = "Internal error"
                    )
                )
            }
        }

        post("/{id}/skip") {
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
                        error = "Internal error"
                    )
                )
            }
        }

        post("/{id}/confirm-occurrence") {
            if (!authenticate(authSessionManager)) return@post
            try {
                val profileId = settingsRepository.currentProfileId.first()
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, error = "Invalid id")
                    )
                val date = call.request.queryParameters["date"]?.toLongOrNull()
                    ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, error = "Invalid date")
                    )
                val payment = plannedPaymentRepository.getById(id)
                    ?: return@post call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse<Unit>(success = false, error = "Payment not found")
                    )

                val request = TransactionRequest(
                    name = payment.name,
                    amount = payment.amount,
                    transactionType = payment.transactionType,
                    accountId = payment.accountId,
                    categoryId = payment.categoryId,
                    currency = payment.currency,
                    timestamp = date,
                    tags = payment.tags,
                    payee = payment.payee ?: "",
                    note = payment.note ?: "",
                    status = payment.transactionStatus,
                    sourcePlannedPaymentId = id
                )

                transactionManager.handleSaveTransaction(
                    existingTransaction = null,
                    request = request,
                    profileId = profileId,
                    sourcePlannedPaymentId = id,
                    scheduledDate = date
                )
                call.respond(ApiResponse(success = true, data = mapOf("confirmed" to id)))
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

        post("/{id}/skip-occurrence") {
            if (!authenticate(authSessionManager)) return@post
            try {
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, error = "Invalid id")
                    )
                val date = call.request.queryParameters["date"]?.toLongOrNull()
                    ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, error = "Invalid date")
                    )
                transactionManager.skipPlannedPayment(id, date)
                call.respond(ApiResponse(success = true, data = mapOf("skipped" to id)))
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
                        error = "Internal error"
                    )
                )
            }
        }

        put("/{id}") {
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
                    targetAccountId = req.targetAccountId ?: existing.targetAccountId,
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
                        error = "Internal error"
                    )
                )
            }
        }
    }
}
