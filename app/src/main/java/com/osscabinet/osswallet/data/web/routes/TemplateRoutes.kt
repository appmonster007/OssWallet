package com.osscabinet.ossWallet.data.web.routes

import com.osscabinet.ossWallet.data.web.AuthSessionManager
import com.osscabinet.ossWallet.data.web.authenticate
import com.osscabinet.ossWallet.data.web.dto.ApiResponse
import com.osscabinet.ossWallet.data.web.dto.CreateTemplateRequest
import com.osscabinet.ossWallet.data.web.dto.UpdateTemplateRequest
import com.osscabinet.ossWallet.data.web.toTemplateDto
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TemplateRepository
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.first
import java.math.BigDecimal

fun Route.templateRoutes(
    authSessionManager: AuthSessionManager,
    templateRepository: TemplateRepository,
    settingsRepository: SettingsRepository
) {
    route("/templates") {
        get {
            if (!authenticate(authSessionManager)) return@get
            try {
                val profileId = settingsRepository.currentProfileId.first()
                val templates = templateRepository.getAllTemplates(profileId).first()
                val dtos = templates.map { it.toTemplateDto() }
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
                val req = call.receive<CreateTemplateRequest>()
                val template = Template(
                    name = req.name,
                    categoryId = req.categoryId,
                    amount = BigDecimal.valueOf(req.amount),
                    entryMode = EntryMode.entries.find {
                        it.name.equals(
                            req.transactionType,
                            ignoreCase = true
                        )
                    }
                        ?: EntryMode.EXPENSE,
                    currency = req.currency,
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
                    icon = req.icon,
                    colorHex = req.colorHex,
                    isStarred = req.isStarred,
                    displayOrder = 0,
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
                        error = "Internal error"
                    )
                )
            }
        }

        get("/{id}") {
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
                    entryMode = req.transactionType?.let { t ->
                        EntryMode.entries.find {
                            it.name.equals(
                                t,
                                ignoreCase = true
                            )
                        } ?: existing.entryMode
                    } ?: existing.entryMode,
                    currency = req.currency ?: existing.currency,
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
                    icon = req.icon ?: existing.icon,
                    colorHex = req.colorHex ?: existing.colorHex,
                    isStarred = req.isStarred ?: existing.isStarred
                )
                templateRepository.updateTemplate(updated)
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
                        error = "Internal error"
                    )
                )
            }
        }
    }
}
