package com.osscabinet.ossWallet.data.web.routes

import com.osscabinet.ossWallet.data.web.AuthSessionManager
import com.osscabinet.ossWallet.data.web.authenticate
import com.osscabinet.ossWallet.data.web.dto.ApiResponse
import com.osscabinet.ossWallet.data.web.dto.AttachmentDto
import com.osscabinet.ossWallet.domain.model.Attachment
import com.osscabinet.ossWallet.domain.model.AttachmentLimits
import com.osscabinet.ossWallet.domain.repository.AttachmentProcessor
import com.osscabinet.ossWallet.domain.repository.AttachmentRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import com.osscabinet.ossWallet.domain.service.management.attachmentTargets
import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.first
import java.net.URLDecoder

/** Formats the browser may draw inline. SVG is deliberately absent: it can carry script. */
private val PREVIEWABLE = setOf("image/jpeg", "image/png", "image/webp", "image/gif")

private fun Attachment.toDto() = AttachmentDto(
    id = id,
    name = displayName.ifBlank { "Attachment" },
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    createdAt = createdAt,
    previewable = mimeType in PREVIEWABLE
)

/** A header-safe file name: no path separators, quotes or control characters. */
private fun safeFileName(name: String): String =
    name.replace(Regex("[\\p{Cntrl}\"\\\\/;]+"), "_").trim().take(120).ifBlank { "attachment" }

/**
 * Files attached to a transaction. Upload sends the raw file as the request body, with the original
 * name in `X-File-Name` (URL-encoded).
 */
fun Route.attachmentRoutes(
    authSessionManager: AuthSessionManager,
    attachmentRepository: AttachmentRepository,
    attachmentProcessor: AttachmentProcessor,
    transactionRepository: TransactionRepository,
    settingsRepository: SettingsRepository,
    transactionUseCases: TransactionUseCases
) {
    route("/transactions/{id}/attachments") {
        get {
            if (!authenticate(authSessionManager)) return@get
            val profileId = settingsRepository.currentProfileId.first()
            val txId = call.parameters["id"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, error = "Invalid id"))
            val list = attachmentRepository.getForTransaction(txId, profileId).map { it.toDto() }
            call.respond(ApiResponse(success = true, data = list))
        }

        post {
            if (!authenticate(authSessionManager)) return@post
            val profileId = settingsRepository.currentProfileId.first()
            val txId = call.parameters["id"]?.toLongOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, error = "Invalid id"))
            if (transactionRepository.getTransactionById(txId, profileId) == null) {
                return@post call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, error = "Transaction not found"))
            }
            val limit = AttachmentLimits.MAX_FILE_BYTES
            val declared = call.request.contentLength()
            if (declared != null && declared > limit) {
                return@post call.respond(HttpStatusCode.PayloadTooLarge, ApiResponse<Unit>(false, error = "That file is larger than ${limit / (1024 * 1024)} MB"))
            }
            val name = call.request.header("X-File-Name")
                ?.let { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrNull() }.orEmpty()
            try {
                val body = call.receive<ByteArray>()
                if (body.size > limit) {
                    return@post call.respond(HttpStatusCode.PayloadTooLarge, ApiResponse<Unit>(false, error = "That file is larger than ${limit / (1024 * 1024)} MB"))
                }
                if (body.isEmpty()) {
                    return@post call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, error = "That file is empty"))
                }
                // Images are downscaled and stripped of location data, exactly as on the phone.
                val prepared = attachmentProcessor.prepareBytes(body, name)
                val rows = attachmentRepository.addShared(
                    transactionUseCases.attachmentTargets(txId, profileId),
                    profileId, prepared.bytes, prepared.mimeType, prepared.displayName
                )
                call.respond(ApiResponse(success = true, data = rows.first { it.transactionId == txId }.toDto()))
            } catch (e: IllegalStateException) { // the per-transaction cap
                call.respond(HttpStatusCode.Conflict, ApiResponse<Unit>(false, error = e.message ?: "Attachment limit reached"))
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, error = e.message ?: "Couldn't use that file"))
            }
        }

        get("/{attId}") {
            if (!authenticate(authSessionManager)) return@get
            val profileId = settingsRepository.currentProfileId.first()
            val txId = call.parameters["id"]?.toLongOrNull()
            val attId = call.parameters["attId"]?.toLongOrNull()
            val row = if (txId == null || attId == null) null
            else attachmentRepository.getForTransaction(txId, profileId).firstOrNull { it.id == attId }
            val bytes = row?.let { attachmentRepository.readBytes(it) }
            if (row == null || bytes == null) {
                return@get call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, error = "Attachment not found"))
            }
            // Anything the browser could render as a page is sent as an opaque download instead, and the
            // sandbox policy stops even a mis-sniffed file from running script in the app's origin.
            val inline = row.mimeType in PREVIEWABLE
            call.response.header("Content-Security-Policy", "sandbox; default-src 'none'")
            call.response.header(
                HttpHeaders.ContentDisposition,
                ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, safeFileName(row.displayName)).toString()
            )
            call.respondBytes(bytes, if (inline) ContentType.parse(row.mimeType) else ContentType.Application.OctetStream)
        }

        delete("/{attId}") {
            if (!authenticate(authSessionManager)) return@delete
            val profileId = settingsRepository.currentProfileId.first()
            val txId = call.parameters["id"]?.toLongOrNull()
            val attId = call.parameters["attId"]?.toLongOrNull()
            val owned = txId != null && attId != null &&
                attachmentRepository.getForTransaction(txId, profileId).any { it.id == attId }
            if (!owned) {
                return@delete call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, error = "Attachment not found"))
            }
            attachmentRepository.delete(attId!!, profileId)
            call.respond(ApiResponse(success = true, data = mapOf("deleted" to attId)))
        }
    }
}
