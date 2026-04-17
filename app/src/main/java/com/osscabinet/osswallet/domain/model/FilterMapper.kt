package com.osscabinet.ossWallet.domain.model

import com.osscabinet.ossWallet.data.local.entity.FilterEntity

fun FilterEntity.toDomain(): TransactionFilter {
    return TransactionFilter(
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
        isIncome = isIncome,
        currencies = if (currencies.isEmpty()) emptyList() else currencies.split(",")
    )
}

fun TransactionFilter.toEntity(name: String, id: Long = 0): FilterEntity {
    return FilterEntity(
        id = id,
        name = name,
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
        isIncome = isIncome,
        currencies = currencies.joinToString(",")
    )
}
