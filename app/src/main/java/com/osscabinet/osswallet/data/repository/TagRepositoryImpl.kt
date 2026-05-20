package com.osscabinet.ossWallet.data.repository

import com.osscabinet.ossWallet.data.local.dao.TagDao
import com.osscabinet.ossWallet.data.local.dao.TransactionDao
import com.osscabinet.ossWallet.data.local.entity.TagEntity
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.repository.TagRepository
import com.osscabinet.ossWallet.domain.util.splitByComma
import com.osscabinet.ossWallet.domain.util.joinByComma
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

    override suspend fun insertTag(tag: Tag) = tagDao.insertTag(TagEntity(id = tag.id, name = tag.name, colorHex = tag.colorHex))

    override suspend fun deleteTag(tag: Tag) = tagDao.deleteTag(TagEntity(id = tag.id, name = tag.name, colorHex = tag.colorHex))

    override suspend fun deleteTagAndRemoveFromTransactions(tag: Tag) {
        val transactions = transactionDao.getTransactionsWithTag(tag.name)
        val updatedTransactions = transactions.map { tx ->
            tx.copy(tags = tx.tags - tag.name)
        }
        transactionDao.updateTransactions(updatedTransactions)
        tagDao.deleteTag(TagEntity(id = tag.id, name = tag.name, colorHex = tag.colorHex))
    }

    override suspend fun getTransactionCountForTag(tagName: String): Int {
        // More accurate check to avoid substring matches if possible, 
        // but for now keeping it consistent with deletion logic
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
        }
        tagDao.insertTag(TagEntity(id = newTag.id, name = newTag.name, colorHex = newTag.colorHex))
    }
}
