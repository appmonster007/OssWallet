package com.osscabinet.ossWallet.data.mapper

import com.osscabinet.ossWallet.data.local.entity.BudgetEntity
import com.osscabinet.ossWallet.data.local.entity.BudgetWithFilter
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.model.BudgetPeriod

fun BudgetWithFilter.toDomain(): Budget {
    val filterDomain = filter.toDomain()
    return Budget(
        id = budget.id,
        name = budget.name,
        currency = budget.currency,
        limitAmount = budget.limitAmount,
        period = BudgetPeriod.valueOf(budget.period),
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
        customStartDate = budget.customStartDate,
        customEndDate = budget.customEndDate
    )
}

fun Budget.toEntity(filterId: Long): BudgetEntity = BudgetEntity(
    id = id,
    name = name,
    currency = currency,
    limitAmount = limitAmount,
    period = period.name,
    alertThreshold = alertThreshold,
    rollover = rollover,
    isArchived = isArchived,
    profileId = profileId,
    customStartDate = customStartDate,
    customEndDate = customEndDate,
    filterId = filterId
)
