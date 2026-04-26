package com.osscabinet.ossWallet.domain.model

import com.osscabinet.ossWallet.data.local.entity.FilterEntity

fun FilterEntity.toDomain(): TransactionFilter {
    return TransactionFilter(
        timelineRange = timelineRange,
        queryNote = queryNote,
        isNoteRegex = isNoteRegex,
        queryPayee = queryPayee,
        isPayeeRegex = isPayeeRegex,
        categoryIds = if (categoryIds.isEmpty()) emptyList() else categoryIds.split(",").map { it.toLong() },
        accountIds = if (accountIds.isEmpty()) emptyList() else accountIds.split(","),
        startDate = startDate,
        endDate = endDate,
        paymentMethods = if (paymentMethods.isEmpty()) emptyList() else paymentMethods.split(","),
        hasWarranty = hasWarranty,
        warrantyExpirationStart = warrantyExpirationStart,
        warrantyExpirationEnd = warrantyExpirationEnd,
        minAmount = minAmount,
        maxAmount = maxAmount,
        transactionType = transactionType,
        currencies = if (currencies.isEmpty()) emptyList() else currencies.split(","),
        statuses = if (statuses.isEmpty()) emptyList() else statuses.split(",").map { TransactionStatus.fromString(it) }
    )
}

fun TransactionFilter.toEntity(name: String, id: Long = 0): FilterEntity {
    return FilterEntity(
        id = id,
        name = name,
        timelineRange = timelineRange,
        queryNote = queryNote,
        isNoteRegex = isNoteRegex,
        queryPayee = queryPayee,
        isPayeeRegex = isPayeeRegex,
        categoryIds = categoryIds.joinToString(","),
        accountIds = accountIds.joinToString(","),
        startDate = startDate,
        endDate = endDate,
        paymentMethods = paymentMethods.joinToString(","),
        hasWarranty = hasWarranty,
        warrantyExpirationStart = warrantyExpirationStart,
        warrantyExpirationEnd = warrantyExpirationEnd,
        minAmount = minAmount,
        maxAmount = maxAmount,
        transactionType = transactionType,
        currencies = currencies.joinToString(","),
        statuses = statuses.joinToString(",") { it.name }
    )
}
