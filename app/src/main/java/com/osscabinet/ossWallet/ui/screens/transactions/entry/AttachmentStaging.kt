package com.osscabinet.ossWallet.ui.screens.transactions.entry

import com.osscabinet.ossWallet.domain.model.Attachment
import com.osscabinet.ossWallet.domain.model.AttachmentLimits
import com.osscabinet.ossWallet.domain.repository.AttachmentRepository
import com.osscabinet.ossWallet.domain.model.PreparedAttachment
import com.osscabinet.ossWallet.domain.repository.AttachmentProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/** One attachment on the form: already stored ([attachment]) or picked but not yet saved ([pending]). */
class AttachmentItem(val key: String, val attachment: Attachment?, val pending: PreparedAttachment?) {
    val displayName: String get() = attachment?.displayName?.ifBlank { null } ?: pending?.displayName ?: "Attachment"
    val mimeType: String get() = attachment?.mimeType ?: pending?.mimeType ?: AttachmentLimits.MIME_UNKNOWN
    val sizeBytes: Long get() = attachment?.sizeBytes ?: pending?.bytes?.size?.toLong() ?: 0L
    val isImage: Boolean get() = AttachmentLimits.isImage(mimeType)
}

data class AttachmentsUiState(
    val items: List<AttachmentItem> = emptyList(),
    val busy: Boolean = false,
    val error: String? = null
) {
    val canAdd: Boolean get() = items.size < AttachmentLimits.MAX_PER_TRANSACTION
}

/**
 * Holds attachment edits for the entry form. Nothing touches storage until [commit], so backing out
 * of the form discards new files and keeps the ones that were marked for removal.
 */
class AttachmentStaging(
    private val attachments: AttachmentRepository,
    private val processor: AttachmentProcessor,
    private val scope: CoroutineScope
) {
    private val existing = MutableStateFlow<List<Attachment>>(emptyList())
    private val pending = MutableStateFlow<List<AttachmentItem>>(emptyList())
    private val removed = MutableStateFlow<Set<Long>>(emptySet())
    private val busy = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)
    private var loadJob: Job? = null

    val state: StateFlow<AttachmentsUiState> = combine(existing, pending, removed, busy, error) { ex, pe, rm, b, e ->
        val stored = ex.filter { it.id !in rm }.map { AttachmentItem("a${it.id}", it, null) }
        AttachmentsUiState(items = stored + pe, busy = b, error = e)
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), AttachmentsUiState())

    /** Follows the stored attachments of [transactionId]; null (new transaction) means none yet. */
    fun load(transactionId: Long?, profileId: Int) {
        loadJob?.cancel()
        existing.value = emptyList()
        if (transactionId == null) return
        loadJob = scope.launch {
            attachments.observeForTransaction(transactionId, profileId).collect { existing.value = it }
        }
    }

    fun add(source: String) {
        if (!state.value.canAdd) {
            error.value = "A transaction can have at most ${AttachmentLimits.MAX_PER_TRANSACTION} attachments"
            return
        }
        scope.launch {
            busy.value = true
            error.value = null
            runCatching { processor.prepare(source) }
                .onSuccess { file -> pending.update { it + AttachmentItem(UUID.randomUUID().toString(), null, file) } }
                .onFailure { error.value = it.message ?: "Couldn't add that file" }
            busy.value = false
        }
    }

    fun remove(item: AttachmentItem) {
        val stored = item.attachment
        if (stored != null) removed.update { it + stored.id } else pending.update { list -> list.filter { it.key != item.key } }
    }

    fun clearError() {
        error.value = null
    }

    /** Applies the staged edits to [transactionId]. Failures here must not undo the saved transaction. */
    suspend fun commit(transactionId: Long, profileId: Int) {
        removed.value.forEach { runCatching { attachments.delete(it, profileId) } }
        pending.value.forEach { item ->
            val file = item.pending ?: return@forEach
            runCatching { attachments.add(transactionId, profileId, file.bytes, file.mimeType, file.displayName) }
        }
        pending.value = emptyList()
        removed.value = emptySet()
    }
}
