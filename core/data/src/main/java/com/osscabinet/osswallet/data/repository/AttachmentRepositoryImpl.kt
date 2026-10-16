package com.osscabinet.ossWallet.data.repository

import com.osscabinet.ossWallet.data.local.AttachmentStore
import com.osscabinet.ossWallet.data.local.dao.AttachmentDao
import com.osscabinet.ossWallet.data.mapper.toDomain
import com.osscabinet.ossWallet.data.mapper.toEntity
import com.osscabinet.ossWallet.domain.model.Attachment
import com.osscabinet.ossWallet.domain.model.AttachmentLimits
import com.osscabinet.ossWallet.domain.repository.AttachmentRepository
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class AttachmentRepositoryImpl @Inject constructor(
    private val dao: AttachmentDao,
    private val store: AttachmentStore
) : AttachmentRepository {

    override fun observeForTransaction(transactionId: Long, profileId: Int): Flow<List<Attachment>> =
        dao.observeForTransaction(transactionId, profileId).map { rows -> rows.map { it.toDomain() } }

    override fun observeCounts(profileId: Int): Flow<Map<Long, Int>> =
        dao.observeCounts(profileId).map { rows -> rows.associate { it.transactionId to it.n } }

    override suspend fun getForTransaction(transactionId: Long, profileId: Int): List<Attachment> =
        dao.getForTransaction(transactionId, profileId).map { it.toDomain() }

    override suspend fun add(
        transactionId: Long,
        profileId: Int,
        bytes: ByteArray,
        mimeType: String,
        displayName: String
    ): Attachment = addShared(listOf(transactionId), profileId, bytes, mimeType, displayName).single()

    override suspend fun addShared(
        transactionIds: List<Long>,
        profileId: Int,
        bytes: ByteArray,
        mimeType: String,
        displayName: String
    ): List<Attachment> = withContext(Dispatchers.IO) {
        val ids = transactionIds.distinct()
        require(ids.isNotEmpty()) { "No transaction to attach to" }
        ids.forEach { id ->
            check(dao.countForTransaction(id, profileId) < AttachmentLimits.MAX_PER_TRANSACTION) {
                "A transaction can have at most ${AttachmentLimits.MAX_PER_TRANSACTION} attachments"
            }
        }
        val name = store.write(bytes)
        val now = System.currentTimeMillis()
        // A failed insert must not leave an unreferenced file (or half the rows) behind.
        runCatching {
            ids.map { id ->
                val draft = Attachment(
                    transactionId = id, profileId = profileId, fileName = name, mimeType = mimeType,
                    sizeBytes = bytes.size.toLong(), createdAt = now, displayName = displayName
                )
                draft.copy(id = dao.insert(draft.toEntity()))
            }
        }.onFailure {
            dao.deleteByFileName(name, profileId)
            store.delete(name)
        }.getOrThrow()
    }

    override suspend fun shareWith(attachments: List<Attachment>, transactionIds: List<Long>, profileId: Int) =
        withContext(Dispatchers.IO) {
            attachments.forEach { a ->
                transactionIds.distinct().filter { it != a.transactionId }.forEach { target ->
                    if (dao.countForTransactionFile(target, a.fileName, profileId) == 0 &&
                        dao.countForTransaction(target, profileId) < AttachmentLimits.MAX_PER_TRANSACTION
                    ) {
                        dao.insert(a.copy(id = 0, transactionId = target).toEntity())
                    }
                }
            }
        }

    override suspend fun readBytes(attachment: Attachment): ByteArray? =
        withContext(Dispatchers.IO) { store.read(attachment.fileName) }

    override suspend fun delete(id: Long, profileId: Int) = withContext(Dispatchers.IO) {
        dao.getById(id, profileId)?.let {
            // Copies on the other leg of a transfer point at the same file; they go with it.
            dao.deleteByFileName(it.fileName, profileId)
            store.delete(it.fileName)
        } ?: Unit
    }

    override suspend fun deleteForTransaction(transactionId: Long, profileId: Int) =
        withContext(Dispatchers.IO) {
            val rows = dao.getForTransaction(transactionId, profileId)
            dao.deleteForTransaction(transactionId, profileId)
            rows.map { it.fileName }.distinct().filter { dao.countByFileName(it) == 0 }.forEach { store.delete(it) }
        }

    override suspend fun removeOrphans() = withContext(Dispatchers.IO) {
        dao.deleteOrphanRows()
        // A restored/imported database can reference files this install doesn't have.
        val onDisk = store.listNames().toSet()
        dao.getAllForBackup().filter { it.fileName !in onDisk }.forEach { dao.deleteById(it.id, it.profileId) }
        val referenced = dao.getAllFileNames().toSet()
        // Also clears stray ".tmp" files from an interrupted write; nothing references those.
        store.listNames().filter { it !in referenced }.forEach { store.delete(it) }
    }

    override suspend fun clearAll() = withContext(Dispatchers.IO) {
        dao.clearAll()
        store.deleteAll()
    }
}
