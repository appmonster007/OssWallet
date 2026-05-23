package com.osscabinet.ossWallet.data.repository

import com.osscabinet.ossWallet.data.local.dao.TagDao
import com.osscabinet.ossWallet.data.local.dao.TransactionDao
import com.osscabinet.ossWallet.data.local.entity.TagEntity
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagRepositoryImpl @Inject constructor(
    private val tagDao: TagDao,
    private val transactionDao: TransactionDao
) : TagRepository {
    override fun getAllTags(): Flow<List<Tag>> = tagDao.getAllTags().map { entities ->
        entities.map { Tag(id = it.id, name = it.name, colorHex = it.colorHex) }
    }

    override suspend fun insertTag(tag: Tag) =
        tagDao.insertTag(TagEntity(id = tag.id, name = tag.name, colorHex = tag.colorHex))

    override suspend fun deleteTag(tag: Tag) =
        tagDao.deleteTag(TagEntity(id = tag.id, name = tag.name, colorHex = tag.colorHex))

    override suspend fun deleteTagAndRemoveFromTransactions(tag: Tag) {
        val transactions = transactionDao.getTransactionsWithTag(tag.name)
        val updatedTransactions = transactions.map { tx ->
            tx.copy(tags = tx.tags - tag.name)
        }
        transactionDao.updateTransactions(updatedTransactions)
        // Room CASCADE handles the transaction_tag_cross_ref deletion
        tagDao.deleteTag(TagEntity(id = tag.id, name = tag.name, colorHex = tag.colorHex))
    }

    override suspend fun getTransactionCountForTag(tagName: String): Int {
        return transactionDao.getTransactionsWithTag(tagName).size
    }

    override suspend fun updateTag(oldName: String, newTag: Tag) {
        if (oldName != newTag.name) {
            val transactions = transactionDao.getTransactionsWithTag(oldName)
            val updatedTransactions = transactions.map { tx ->
                val currentTags = tx.tags.map { if (it == oldName) newTag.name else it }.toSet()
                tx.copy(tags = currentTags)
            }
            transactionDao.updateTransactions(updatedTransactions)
            // The junction table uses tagId, so no change needed there for a name change.
            // But if the ID were to change (it doesn't here), we'd need more logic.
        }
        tagDao.insertTag(TagEntity(id = newTag.id, name = newTag.name, colorHex = newTag.colorHex))
    }
}
