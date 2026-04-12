package com.osscabinet.gleemanager.data.repository

import com.osscabinet.gleemanager.data.local.dao.TagDao
import com.osscabinet.gleemanager.data.local.dao.TransactionDao
import com.osscabinet.gleemanager.data.local.entity.TagEntity
import com.osscabinet.gleemanager.domain.repository.TagRepository
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
            val currentTags = tx.tags.split(",").filter { it.isNotBlank() && it != tag.name }
            tx.copy(tags = currentTags.joinToString(","))
        }
        transactionDao.updateTransactions(updatedTransactions)
        tagDao.deleteTag(tag)
    }
}
