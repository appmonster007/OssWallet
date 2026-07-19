package com.osscabinet.ossWallet.data.web.routes

import com.osscabinet.ossWallet.data.web.AuthSessionManager
import com.osscabinet.ossWallet.data.web.authenticate
import com.osscabinet.ossWallet.data.web.dto.ApiResponse
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TagRepository
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.first

fun Route.tagRoutes(
    authSessionManager: AuthSessionManager,
    tagRepository: TagRepository,
    settingsRepository: SettingsRepository
) {
    route("/tags") {
        get {
            if (!authenticate(authSessionManager)) return@get
            try {
                val tags = tagRepository.getAllTags().first()
                call.respond(ApiResponse(success = true, data = tags))
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
