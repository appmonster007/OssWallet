package com.osscabinet.ossWallet.data.mapper

import com.osscabinet.ossWallet.data.local.entity.BudgetEntity
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.model.BudgetPeriod
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.util.splitByComma
import com.osscabinet.ossWallet.domain.util.joinByComma

fun BudgetEntity.toDomain(): Budget = Budget(
    id = id,
    name = name,
    currency = currency,
    limitAmount = limitAmount,
    period = BudgetPeriod.valueOf(period),
    categoryIds = categoryIds.splitByComma().mapNotNull { it.toLongOrNull() },
    accountIds = accountIds.splitByComma(),
    tags = tags.splitByComma(),
    alertThreshold = alertThreshold,
    rollover = rollover,
    trackTypes = trackType.splitByComma().map {
        TransactionType.fromValue(it.trim())
    }.ifEmpty { listOf(TransactionType.EXPENSE) },
    isArchived = isArchived,
    profileId = profileId,
    customStartDate = customStartDate,
    customEndDate = customEndDate
)

fun Budget.toEntity(): BudgetEntity = BudgetEntity(
    id = id,
    name = name,
    currency = currency,
    limitAmount = limitAmount,
    period = period.name,
    categoryIds = categoryIds.map { it.toString() }.joinByComma(),
    accountIds = accountIds.joinByComma(),
    tags = tags.joinByComma(),
    alertThreshold = alertThreshold,
    rollover = rollover,
    trackType = trackTypes.joinToString(",") { it.value },
    isArchived = isArchived,
    profileId = profileId,
    customStartDate = customStartDate,
    customEndDate = customEndDate
)
