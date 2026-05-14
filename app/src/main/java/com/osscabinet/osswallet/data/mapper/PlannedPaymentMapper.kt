package com.osscabinet.ossWallet.data.mapper

import com.osscabinet.ossWallet.data.local.entity.PlannedPaymentEntity
import com.osscabinet.ossWallet.data.local.entity.PlannedPaymentOccurrenceEntity
import com.osscabinet.ossWallet.data.local.entity.PlannedPaymentWithTemplate
import com.osscabinet.ossWallet.data.local.entity.RecurrenceScheduleEntity
import com.osscabinet.ossWallet.domain.model.PaymentSchedule
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.PlannedPaymentOccurrence
import com.osscabinet.ossWallet.domain.model.RecurrenceRule

fun PlannedPaymentWithTemplate.toDomain(): PlannedPayment = PlannedPayment(
    id = payment.id,
    name = template.name,
    categoryId = template.categoryId,
    amount = template.amount,
    transactionType = template.transactionType,
    accountId = template.accountId,
    targetAccountId = template.targetAccountId,
    targetCurrency = template.targetCurrency,
    transferRate = template.transferRate,
    note = template.note,
    payee = template.payee,
    location = template.location,
    latitude = template.latitude,
    longitude = template.longitude,
    warrantyExpiration = template.warrantyExpiration,
    transactionStatus = template.transactionStatus,
    paymentMethod = template.paymentMethod,
    currency = template.currency,
    tags = template.tags,
    schedule = PaymentSchedule(
        recurrenceRule = RecurrenceRule(
            type = com.osscabinet.ossWallet.domain.model.RecurrenceType.valueOf(payment.schedule.recurrenceType),
            interval = payment.schedule.interval,
            dayOfWeek = payment.schedule.dayOfWeek,
            dayOfMonth = payment.schedule.dayOfMonth,
            cronExpression = payment.schedule.recurrenceCronExpression,
        ),
        startDate = payment.schedule.startDate,
        endDate = payment.schedule.endDate,
        nextDueDate = payment.schedule.nextDueDate,
        lastTriggeredDate = payment.schedule.lastTriggeredDate
    ),
    autoConfirm = payment.autoConfirm,
    status = payment.status,
    profileId = payment.profileId,
    templateId = payment.templateId
)

fun PlannedPayment.toEntity(): PlannedPaymentEntity = PlannedPaymentEntity(
    id = id,
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
    templateId = templateId ?: throw IllegalArgumentException("TemplateId cannot be null for PlannedPaymentEntity")
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
