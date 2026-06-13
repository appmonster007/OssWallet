package com.osscabinet.ossWallet.data.mapper

import com.osscabinet.ossWallet.data.local.entity.FilterEntity
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange

fun FilterEntity.toDomain(): Filter {
    val resolvedTimelineRange =
        if (timelineRange is TimelineRange.AllTime && (startDate != null || endDate != null)) {
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
            categoryIds = categoryIds,
            accountIds = accountIds,
            paymentMethods = paymentMethods,
            hasWarranty = hasWarranty,
            warrantyExpirationStart = warrantyExpirationStart,
            warrantyExpirationEnd = warrantyExpirationEnd,
            minAmount = minAmount,
            maxAmount = maxAmount,
            transactionTypes = transactionTypes,
            currencies = currencies,
            statuses = statuses,
            linkedTransactionIds = linkedTransactionIds,
            tags = tags,
            matchAllTags = matchAllTags
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
        categoryIds = filter.categoryIds,
        accountIds = filter.accountIds,
        startDate = entityStart,
        endDate = entityEnd,
        paymentMethods = filter.paymentMethods,
        hasWarranty = filter.hasWarranty,
        warrantyExpirationStart = filter.warrantyExpirationStart,
        warrantyExpirationEnd = filter.warrantyExpirationEnd,
        minAmount = filter.minAmount,
        maxAmount = filter.maxAmount,
        transactionTypes = filter.transactionTypes,
        currencies = filter.currencies,
        statuses = filter.statuses,
        linkedTransactionIds = filter.linkedTransactionIds,
        tags = filter.tags,
        matchAllTags = filter.matchAllTags
    )
}
