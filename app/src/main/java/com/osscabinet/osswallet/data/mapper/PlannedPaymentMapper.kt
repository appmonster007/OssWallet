package com.osscabinet.ossWallet.data.mapper

import com.osscabinet.ossWallet.data.local.entity.PlannedPaymentEntity
import com.osscabinet.ossWallet.data.local.entity.PlannedPaymentOccurrenceEntity
import com.osscabinet.ossWallet.domain.model.OccurrenceStatus
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.PlannedPaymentOccurrence
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
import com.osscabinet.ossWallet.domain.model.RecurrenceRule
import com.osscabinet.ossWallet.domain.model.RecurrenceType
import com.osscabinet.ossWallet.domain.util.splitByComma
import com.osscabinet.ossWallet.domain.util.joinByComma

fun PlannedPaymentEntity.toDomain(): PlannedPayment = PlannedPayment(
    id = id,
    title = title,
    amount = amount,
    currency = currency,
    transactionType = transactionType,
    accountId = accountId,
    targetAccountId = targetAccountId,
    categoryId = categoryId,
    paymentMethod = paymentMethod,
    tags = tags.splitByComma(),
    note = note,
    payee = payee,
    recurrenceRule = RecurrenceRule(
        type = RecurrenceType.valueOf(recurrenceType),
        interval = recurrenceInterval,
        dayOfWeek = recurrenceDayOfWeek,
        dayOfMonth = recurrenceDayOfMonth
    ),
    startDate = startDate,
    endDate = endDate,
    nextDueDate = nextDueDate,
    lastTriggeredDate = lastTriggeredDate,
    autoConfirm = autoConfirm,
    status = PlannedPaymentStatus.valueOf(status),
    profileId = profileId
)

fun PlannedPayment.toEntity(): PlannedPaymentEntity = PlannedPaymentEntity(
    id = id,
    title = title,
    amount = amount,
    currency = currency,
    transactionType = transactionType,
    accountId = accountId,
    targetAccountId = targetAccountId,
    categoryId = categoryId,
    paymentMethod = paymentMethod,
    tags = tags.joinByComma(),
    note = note,
    payee = payee,
    recurrenceType = recurrenceRule.type.name,
    recurrenceInterval = recurrenceRule.interval,
    recurrenceDayOfWeek = recurrenceRule.dayOfWeek,
    recurrenceDayOfMonth = recurrenceRule.dayOfMonth,
    startDate = startDate,
    endDate = endDate,
    nextDueDate = nextDueDate,
    lastTriggeredDate = lastTriggeredDate,
    autoConfirm = autoConfirm,
    status = status.name,
    profileId = profileId
)

fun PlannedPaymentOccurrenceEntity.toDomain(): PlannedPaymentOccurrence = PlannedPaymentOccurrence(
    id = id,
    plannedPaymentId = plannedPaymentId,
    scheduledDate = scheduledDate,
    generatedTransactionId = generatedTransactionId,
    status = OccurrenceStatus.valueOf(status)
)

fun PlannedPaymentOccurrence.toEntity(): PlannedPaymentOccurrenceEntity = PlannedPaymentOccurrenceEntity(
    id = id,
    plannedPaymentId = plannedPaymentId,
    scheduledDate = scheduledDate,
    generatedTransactionId = generatedTransactionId,
    status = status.name
)
