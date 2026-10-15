package com.osscabinet.ossWallet.data.mapper

import com.osscabinet.ossWallet.data.local.entity.GoalEntity
import com.osscabinet.ossWallet.domain.model.Goal

fun GoalEntity.toDomain() = Goal(
    id = id, name = name, targetAmount = targetAmount, savedAmount = savedAmount,
    currency = currency, deadline = deadline, note = note, profileId = profileId,
    createdAt = createdAt, completedAt = completedAt
)

fun Goal.toEntity() = GoalEntity(
    id = id, name = name, targetAmount = targetAmount, savedAmount = savedAmount,
    currency = currency, deadline = deadline, note = note, profileId = profileId,
    createdAt = createdAt, completedAt = completedAt
)
