package com.osscabinet.ossWallet.data.mapper

import com.osscabinet.ossWallet.data.local.entity.PlannedPaymentEntity
import com.osscabinet.ossWallet.data.local.entity.PlannedPaymentOccurrenceEntity
import com.osscabinet.ossWallet.data.local.entity.RecurrenceScheduleEntity
import com.osscabinet.ossWallet.domain.model.PaymentSchedule
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.PlannedPaymentOccurrence
import com.osscabinet.ossWallet.domain.model.RecurrenceRule

fun PlannedPaymentEntity.toDomain(): PlannedPayment = PlannedPayment(
    id = id,
    name = name,
    categoryId = categoryId,
    amount = amount,
    transactionType = transactionType,
    accountId = accountId,
    targetAccountId = targetAccountId,
    targetCurrency = targetCurrency,
    transferRate = transferRate,
    note = note,
    payee = payee,
    location = location,
    latitude = latitude,
    longitude = longitude,
    warrantyExpiration = warrantyExpiration,
    transactionStatus = transactionStatus,
    paymentMethod = paymentMethod,
    currency = currency,
    tags = if (tags.isEmpty()) emptyList() else tags.split(","),
    schedule = PaymentSchedule(
        recurrenceRule = RecurrenceRule(
            type = com.osscabinet.ossWallet.domain.model.RecurrenceType.valueOf(schedule.recurrenceType),
            interval = schedule.interval,
            dayOfWeek = schedule.dayOfWeek,
            dayOfMonth = schedule.dayOfMonth,
            cronExpression = schedule.recurrenceCronExpression
        ),
        startDate = schedule.startDate,
        endDate = schedule.endDate,
        nextDueDate = schedule.nextDueDate,
        lastTriggeredDate = schedule.lastTriggeredDate
    ),
    autoConfirm = autoConfirm,
    status = status,
    profileId = profileId,
    templateId = templateId
)

fun PlannedPayment.toEntity(): PlannedPaymentEntity = PlannedPaymentEntity(
    id = id,
    name = name,
    categoryId = categoryId,
    amount = amount,
    transactionType = transactionType,
    accountId = accountId,
    targetAccountId = targetAccountId,
    targetCurrency = targetCurrency,
    transferRate = transferRate,
    note = note,
    payee = payee,
    location = location,
    latitude = latitude,
    longitude = longitude,
    warrantyExpiration = warrantyExpiration,
    transactionStatus = transactionStatus,
    paymentMethod = paymentMethod,
    currency = currency,
    tags = tags.joinToString(","),
    schedule = RecurrenceScheduleEntity(
        recurrenceType = schedule.recurrenceRule.type.name,
        interval = schedule.recurrenceRule.interval,
        dayOfWeek = schedule.recurrenceRule.dayOfWeek,
        dayOfMonth = schedule.recurrenceRule.dayOfMonth,
        recurrenceCronExpression = schedule.recurrenceRule.cronExpression,
        startDate = schedule.startDate,
        endDate = schedule.endDate,
        nextDueDate = schedule.nextDueDate,
        lastTriggeredDate = schedule.lastTriggeredDate
    ),
    autoConfirm = autoConfirm,
    status = status,
    profileId = profileId,
    templateId = templateId
)

fun PlannedPaymentOccurrenceEntity.toDomain(): PlannedPaymentOccurrence = PlannedPaymentOccurrence(
    id = id,
    plannedPaymentId = plannedPaymentId,
    scheduledDate = scheduledDate,
    generatedTransactionId = generatedTransactionId,
    status = status
)

fun PlannedPaymentOccurrence.toEntity(): PlannedPaymentOccurrenceEntity = PlannedPaymentOccurrenceEntity(
    id = id,
    plannedPaymentId = plannedPaymentId,
    scheduledDate = scheduledDate,
    generatedTransactionId = generatedTransactionId,
    status = status
)
