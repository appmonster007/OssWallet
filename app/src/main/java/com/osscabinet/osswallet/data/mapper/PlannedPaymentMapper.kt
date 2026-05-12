package com.osscabinet.ossWallet.data.mapper

import com.osscabinet.ossWallet.data.local.entity.PlannedPaymentEntity
import com.osscabinet.ossWallet.data.local.entity.PlannedPaymentOccurrenceEntity
import com.osscabinet.ossWallet.data.local.entity.RecurrenceScheduleEntity
import com.osscabinet.ossWallet.data.local.entity.TransactionTemplateEntity
import com.osscabinet.ossWallet.domain.model.OccurrenceStatus
import com.osscabinet.ossWallet.domain.model.PaymentSchedule
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.PlannedPaymentOccurrence
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
import com.osscabinet.ossWallet.domain.model.RecurrenceRule
import com.osscabinet.ossWallet.domain.model.TransactionTemplate
import com.osscabinet.ossWallet.domain.service.management.PlannedPaymentService
import com.osscabinet.ossWallet.domain.util.splitByComma
import com.osscabinet.ossWallet.domain.util.joinByComma

fun PlannedPaymentEntity.toDomain(): PlannedPayment = PlannedPayment(
    id = id,
    template = TransactionTemplate(
        title = template.title,
        amount = template.amount,
        currency = template.currency,
        transactionType = template.transactionType,
        accountId = template.accountId,
        targetAccountId = template.targetAccountId,
        targetCurrency = template.targetCurrency,
        transferRate = template.transferRate,
        categoryId = template.categoryId,
        paymentMethod = template.paymentMethod,
        tags = template.tags.splitByComma(),
        note = template.note,
        payee = template.payee
    ),
    schedule = PaymentSchedule(
        recurrenceRule = PlannedPaymentService.parseCronToRule(schedule.recurrenceCronExpression ?: "")
            ?: RecurrenceRule(com.osscabinet.ossWallet.domain.model.RecurrenceType.ONE_TIME),
        startDate = schedule.startDate,
        endDate = schedule.endDate,
        nextDueDate = schedule.nextDueDate,
        lastTriggeredDate = schedule.lastTriggeredDate
    ),
    autoConfirm = autoConfirm,
    status = PlannedPaymentStatus.valueOf(status),
    profileId = profileId
)

fun PlannedPayment.toEntity(): PlannedPaymentEntity = PlannedPaymentEntity(
    id = id,
    template = TransactionTemplateEntity(
        title = template.title,
        amount = template.amount,
        currency = template.currency,
        transactionType = template.transactionType,
        accountId = template.accountId,
        targetAccountId = template.targetAccountId,
        targetCurrency = template.targetCurrency,
        transferRate = template.transferRate,
        categoryId = template.categoryId,
        paymentMethod = template.paymentMethod,
        tags = template.tags.joinByComma(),
        note = template.note,
        payee = template.payee
    ),
    schedule = RecurrenceScheduleEntity(
        recurrenceCronExpression = PlannedPaymentService.ruleToCron(schedule.recurrenceRule),
        startDate = schedule.startDate,
        endDate = schedule.endDate,
        nextDueDate = schedule.nextDueDate,
        lastTriggeredDate = schedule.lastTriggeredDate
    ),
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
