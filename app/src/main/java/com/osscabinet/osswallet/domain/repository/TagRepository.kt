package com.osscabinet.ossWallet.domain.repository

import com.osscabinet.ossWallet.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

interface TagRepository {
    fun getAllTags(): Flow<List<TagEntity>>
    suspend fun insertTag(tag: TagEntity)
    suspend fun deleteTag(tag: TagEntity)
    suspend fun deleteTagAndRemoveFromTransactions(tag: TagEntity)
}
