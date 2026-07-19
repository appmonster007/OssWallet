package com.osscabinet.ossWallet.data.web.routes

import com.osscabinet.ossWallet.data.web.AuthSessionManager
import com.osscabinet.ossWallet.data.web.authenticate
import com.osscabinet.ossWallet.data.web.dto.ApiResponse
import com.osscabinet.ossWallet.data.web.dto.SettingsDto
import com.osscabinet.ossWallet.data.web.dto.UpdateSettingsRequest
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.first

fun Route.settingsRoutes(
    authSessionManager: AuthSessionManager,
    settingsRepository: SettingsRepository
) {
    route("/settings") {
        get {
            if (!authenticate(authSessionManager)) return@get
            try {
                val currency = settingsRepository.currency.first()
                val precision = settingsRepository.decimalPrecision.first()
                val profileId = settingsRepository.currentProfileId.first()
                call.respond(
                    ApiResponse(
                        success = true,
                        data = SettingsDto(
                            baseCurrency = currency,
                            decimalPrecision = precision,
                            currentProfileId = profileId
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

        put {
            if (!authenticate(authSessionManager)) return@put
            try {
                val req = call.receive<UpdateSettingsRequest>()
                req.baseCurrency?.let { settingsRepository.saveCurrency(it) }
                req.decimalPrecision?.let { settingsRepository.saveDecimalPrecision(it) }
                call.respond(ApiResponse(success = true, data = "Settings updated"))
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
