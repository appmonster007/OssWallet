package com.osscabinet.ossWallet.data.mapper

import com.osscabinet.ossWallet.data.local.entity.FilterEntity
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange

fun FilterEntity.toDomain(): Filter {
    val resolvedTimelineRange = if (timelineRange is TimelineRange.AllTime && (startDate != null || endDate != null)) {
        TimelineRange.CustomRange(startDate, endDate)
    } else {
        timelineRange
    }

    return Filter(
        id = id,
        name = name,
        filter = TransactionFilter(
            timelineRange = resolvedTimelineRange,
            queryNote = queryNote,
            isNoteRegex = isNoteRegex,
            queryPayee = queryPayee,
            isPayeeRegex = isPayeeRegex,
            categoryIds = if (categoryIds.isEmpty()) emptyList() else categoryIds.split(",").map { it.toLong() },
            accountIds = if (accountIds.isEmpty()) emptyList() else accountIds.split(","),
            paymentMethods = if (paymentMethods.isEmpty()) emptyList() else paymentMethods.split(","),
            hasWarranty = hasWarranty,
            warrantyExpirationStart = warrantyExpirationStart,
            warrantyExpirationEnd = warrantyExpirationEnd,
            minAmount = minAmount,
            maxAmount = maxAmount,
            transactionType = transactionType,
            currencies = if (currencies.isEmpty()) emptyList() else currencies.split(","),
            statuses = if (statuses.isEmpty()) emptyList() else statuses.split(",").filter { it.isNotBlank() }.map { TransactionStatus.valueOf(it) }
        )
    )
}

fun Filter.toEntity(): FilterEntity {
    val (entityStart, entityEnd) = if (filter.timelineRange is TimelineRange.CustomRange) {
        Pair(filter.timelineRange.start, filter.timelineRange.end)
    } else {
        Pair(null, null)
    }

    return FilterEntity(
        id = id,
        name = name,
        timelineRange = filter.timelineRange,
        queryNote = filter.queryNote,
        isNoteRegex = filter.isNoteRegex,
        queryPayee = filter.queryPayee,
        isPayeeRegex = filter.isPayeeRegex,
        categoryIds = filter.categoryIds.joinToString(","),
        accountIds = filter.accountIds.joinToString(","),
        startDate = entityStart,
        endDate = entityEnd,
        paymentMethods = filter.paymentMethods.joinToString(","),
        hasWarranty = filter.hasWarranty,
        warrantyExpirationStart = filter.warrantyExpirationStart,
        warrantyExpirationEnd = filter.warrantyExpirationEnd,
        minAmount = filter.minAmount,
        maxAmount = filter.maxAmount,
        transactionType = filter.transactionType,
        currencies = filter.currencies.joinToString(","),
        statuses = filter.statuses.joinToString(",") { it.name }
    )
}
