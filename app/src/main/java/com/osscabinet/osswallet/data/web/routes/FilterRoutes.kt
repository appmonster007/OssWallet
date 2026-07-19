package com.osscabinet.ossWallet.data.web.routes

import com.osscabinet.ossWallet.data.web.AuthSessionManager
import com.osscabinet.ossWallet.data.web.authenticate
import com.osscabinet.ossWallet.data.web.dto.ApiResponse
import com.osscabinet.ossWallet.data.web.dto.CreateFilterRequest
import com.osscabinet.ossWallet.data.web.dto.UpdateFilterRequest
import com.osscabinet.ossWallet.data.web.toDto
import com.osscabinet.ossWallet.data.web.toTransactionFilter
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.repository.FilterRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.first

fun Route.filterRoutes(
    authSessionManager: AuthSessionManager,
    filterRepository: FilterRepository,
    settingsRepository: SettingsRepository
) {
    route("/filters") {
        get {
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
                        error = "Internal error"
                    )
                )
            }
        }

        post {
            if (!authenticate(authSessionManager)) return@post
            try {
                val req = call.receive<CreateFilterRequest>()
                val filter = Filter(
                    id = 0,
                    name = req.name,
                    filter = req.toTransactionFilter()
                )
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
                val existing = filterRepository.getFilterById(id)
                    ?: return@put call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse<Unit>(success = false, error = "Filter not found")
                    )
                val req = call.receive<UpdateFilterRequest>()
                val updated = existing.copy(
                    name = req.name ?: existing.name,
                    filter = req.filterData?.toTransactionFilter() ?: existing.filter
                )
                filterRepository.updateFilter(updated)
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
                        error = "Internal error"
                    )
                )
            }
        }
    }
}
