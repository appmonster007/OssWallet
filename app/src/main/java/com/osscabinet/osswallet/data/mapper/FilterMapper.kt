package com.osscabinet.ossWallet.data.mapper

import com.osscabinet.ossWallet.data.local.entity.FilterEntity
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.util.splitByComma
import com.osscabinet.ossWallet.domain.util.joinByComma

/**
 * Mappers for Filter data.
 */
fun FilterEntity.toDomain(): TransactionFilter {
    return TransactionFilter(
        timelineRange = timelineRange,
        queryNote = queryNote,
        isNoteRegex = isNoteRegex,
        queryPayee = queryPayee,
        isPayeeRegex = isPayeeRegex,
        categoryIds = categoryIds.splitByComma().map { it.toLong() },
        accountIds = accountIds.splitByComma(),
        startDate = startDate,
        endDate = endDate,
        paymentMethods = paymentMethods.splitByComma(),
        hasWarranty = hasWarranty,
        warrantyExpirationStart = warrantyExpirationStart,
        warrantyExpirationEnd = warrantyExpirationEnd,
        minAmount = minAmount,
        maxAmount = maxAmount,
        transactionType = transactionType,
        currencies = currencies.splitByComma(),
        statuses = statuses.splitByComma().map { TransactionStatus.fromString(it) }
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
        categoryIds = categoryIds.joinByComma(),
        accountIds = accountIds.joinByComma(),
        startDate = startDate,
        endDate = endDate,
        paymentMethods = paymentMethods.joinByComma(),
        hasWarranty = hasWarranty,
        warrantyExpirationStart = warrantyExpirationStart,
        warrantyExpirationEnd = warrantyExpirationEnd,
        minAmount = minAmount,
        maxAmount = maxAmount,
        transactionType = transactionType,
        currencies = currencies.joinByComma(),
        statuses = statuses.joinByComma { it.name }
    )
}
