package com.osscabinet.ossWallet.data.mapper

import com.osscabinet.ossWallet.data.local.entity.BudgetEntity
import com.osscabinet.ossWallet.data.local.entity.BudgetWithFilter
import com.osscabinet.ossWallet.domain.model.Budget

fun BudgetWithFilter.toDomain(): Budget {
    val filterDomain = filter.toDomain()
    return Budget(
        id = budget.id,
        name = budget.name,
        currency = budget.currency,
        limitAmount = budget.limitAmount,
        timelineRange = budget.timelineRange,
        categoryIds = filterDomain.filter.categoryIds,
        accountIds = filterDomain.filter.accountIds,
        tags = filterDomain.filter.tags,
        paymentMethods = filterDomain.filter.paymentMethods,
        statuses = filterDomain.filter.statuses,
        currencies = filterDomain.filter.currencies,
        linkedTransactionIds = filterDomain.filter.linkedTransactionIds,
        alertThreshold = budget.alertThreshold,
        rollover = budget.rollover,
        trackTypes = filterDomain.filter.transactionTypes,
        isArchived = budget.isArchived,
        profileId = budget.profileId,
        filterId = budget.filterId
    )
}

fun Budget.toEntity(filterId: Long): BudgetEntity = BudgetEntity(
    id = id,
    name = name,
    currency = currency,
    limitAmount = limitAmount,
    timelineRange = timelineRange,
    alertThreshold = alertThreshold,
    rollover = rollover,
    isArchived = isArchived,
    profileId = profileId,
    filterId = filterId
)
