package com.osscabinet.ossWallet.domain.repository

import com.osscabinet.ossWallet.domain.model.Tag
import kotlinx.coroutines.flow.Flow

interface TagRepository {
    fun getAllTags(): Flow<List<Tag>>
    suspend fun insertTag(tag: Tag)
    suspend fun deleteTag(tag: Tag)
    suspend fun deleteTagAndRemoveFromTransactions(tag: Tag)
    suspend fun getTransactionCountForTag(tagName: String, profileId: Int): Int
    suspend fun updateTag(oldName: String, newTag: Tag)
}
