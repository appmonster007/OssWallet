package com.osscabinet.ossWallet.data.web.routes

import com.osscabinet.ossWallet.data.web.AuthSessionManager
import com.osscabinet.ossWallet.data.web.authenticate
import com.osscabinet.ossWallet.data.web.dto.ApiResponse
import com.osscabinet.ossWallet.data.web.dto.LoanDto
import com.osscabinet.ossWallet.data.web.dto.LoanPaymentDto
import com.osscabinet.ossWallet.data.web.dto.LoanPaymentRequest
import com.osscabinet.ossWallet.data.web.dto.CreateLoanRequest
import com.osscabinet.ossWallet.data.web.dto.UpdateLoanRequest
import com.osscabinet.ossWallet.domain.model.Loan
import com.osscabinet.ossWallet.domain.model.LoanPayment
import com.osscabinet.ossWallet.domain.model.LoanType
import com.osscabinet.ossWallet.domain.repository.LoanRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.service.management.LoanService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.first
import java.math.BigDecimal

fun Route.loanRoutes(
    authSessionManager: AuthSessionManager,
    loanRepository: LoanRepository,
    settingsRepository: SettingsRepository
) {
    route("/loans") {
        get {
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
                        expectedSettlementDate = loan.expectedSettlementDate,
                        interestRate = loan.interestRate?.toDouble(),
                        linkedAccountId = loan.linkedAccountId
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
                        error = "Internal error"
                    )
                )
            }
        }

        get("/{id}") {
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
                            expectedSettlementDate = loan.expectedSettlementDate,
                            interestRate = loan.interestRate?.toDouble(),
                            linkedAccountId = loan.linkedAccountId
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
                loanRepository.deleteLoan(id)
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

        get("/{id}/payments") {
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
                        error = "Internal error"
                    )
                )
            }
        }

        post("/{id}/payments") {
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
                        error = "Internal error"
                    )
                )
            }
        }

        delete("/{loanId}/payments/{paymentId}") {
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
                        error = "Internal error"
                    )
                )
            }
        }
    }
}
