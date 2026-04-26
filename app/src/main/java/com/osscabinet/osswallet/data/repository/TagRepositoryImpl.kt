package com.osscabinet.ossWallet.data.repository

import com.osscabinet.ossWallet.data.local.dao.TagDao
import com.osscabinet.ossWallet.data.local.dao.TransactionDao
import com.osscabinet.ossWallet.data.local.entity.TagEntity
import com.osscabinet.ossWallet.domain.repository.TagRepository
import com.osscabinet.ossWallet.domain.util.splitByComma
import com.osscabinet.ossWallet.domain.util.joinByComma
import kotlinx.coroutines.flow.Flow

class TagRepositoryImpl(
    private val tagDao: TagDao,
    private val transactionDao: TransactionDao
) : TagRepository {
    override fun getAllTags(): Flow<List<TagEntity>> = tagDao.getAllTags()

    override suspend fun insertTag(tag: TagEntity) = tagDao.insertTag(tag)

    override suspend fun deleteTag(tag: TagEntity) = tagDao.deleteTag(tag)

    override suspend fun deleteTagAndRemoveFromTransactions(tag: TagEntity) {
        val transactions = transactionDao.getTransactionsWithTag(tag.name)
        val updatedTransactions = transactions.map { tx ->
            val currentTags = tx.tags.splitByComma().filter { it.isNotBlank() && it != tag.name }
            tx.copy(tags = currentTags.joinByComma())
        }
        transactionDao.updateTransactions(updatedTransactions)
        tagDao.deleteTag(tag)
    }
}
