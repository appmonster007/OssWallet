package com.osscabinet.ossWallet.ui.screens.transactions.entry

import com.osscabinet.ossWallet.domain.model.Attachment
import com.osscabinet.ossWallet.domain.model.AttachmentLimits
import com.osscabinet.ossWallet.domain.repository.AttachmentRepository
import com.osscabinet.ossWallet.domain.repository.ReceiptImageProcessor
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

/** One receipt on the form: already stored ([attachment]) or picked but not yet saved ([pendingJpeg]). */
class ReceiptItem(val key: String, val attachment: Attachment?, val pendingJpeg: ByteArray?)

data class ReceiptsUiState(
    val items: List<ReceiptItem> = emptyList(),
    val busy: Boolean = false,
    val error: String? = null
) {
    val canAdd: Boolean get() = items.size < AttachmentLimits.MAX_PER_TRANSACTION
}

/**
 * Holds receipt edits for the entry form. Nothing touches storage until [commit], so backing out
 * of the form discards new photos and keeps the ones that were marked for removal.
 */
class ReceiptStaging(
    private val attachments: AttachmentRepository,
    private val processor: ReceiptImageProcessor,
    private val scope: CoroutineScope
) {
    private val existing = MutableStateFlow<List<Attachment>>(emptyList())
    private val pending = MutableStateFlow<List<ReceiptItem>>(emptyList())
    private val removed = MutableStateFlow<Set<Long>>(emptySet())
    private val busy = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)
    private var loadJob: Job? = null

    val state: StateFlow<ReceiptsUiState> = combine(existing, pending, removed, busy, error) { ex, pe, rm, b, e ->
        val stored = ex.filter { it.id !in rm }.map { ReceiptItem("a${it.id}", it, null) }
        ReceiptsUiState(items = stored + pe, busy = b, error = e)
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), ReceiptsUiState())

    /** Follows the stored receipts of [transactionId]; null (new transaction) means none yet. */
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
            error.value = "A transaction can have at most ${AttachmentLimits.MAX_PER_TRANSACTION} receipts"
            return
        }
        scope.launch {
            busy.value = true
            error.value = null
            runCatching { processor.prepare(source) }
                .onSuccess { jpeg -> pending.update { it + ReceiptItem(UUID.randomUUID().toString(), null, jpeg) } }
                .onFailure { error.value = it.message ?: "Couldn't add that photo" }
            busy.value = false
        }
    }

    fun remove(item: ReceiptItem) {
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
            runCatching { attachments.add(transactionId, profileId, item.pendingJpeg ?: return@forEach) }
        }
        pending.value = emptyList()
        removed.value = emptySet()
    }
}
