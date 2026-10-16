package com.osscabinet.ossWallet.data.mapper

import com.osscabinet.ossWallet.data.local.entity.AttachmentEntity
import com.osscabinet.ossWallet.domain.model.Attachment

fun AttachmentEntity.toDomain() = Attachment(
    id = id, transactionId = transactionId, profileId = profileId, fileName = fileName,
    mimeType = mimeType, sizeBytes = sizeBytes, createdAt = createdAt, displayName = displayName
)

fun Attachment.toEntity() = AttachmentEntity(
    id = id, transactionId = transactionId, profileId = profileId, fileName = fileName,
    mimeType = mimeType, sizeBytes = sizeBytes, createdAt = createdAt, displayName = displayName
)
