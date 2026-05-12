package com.osscabinet.ossWallet.data.mapper

import com.osscabinet.ossWallet.data.local.entity.PlannedPaymentEntity
import com.osscabinet.ossWallet.data.local.entity.PlannedPaymentOccurrenceEntity
import com.osscabinet.ossWallet.data.local.entity.PlannedPaymentWithTemplate
import com.osscabinet.ossWallet.data.local.entity.RecurrenceScheduleEntity
import com.osscabinet.ossWallet.domain.model.OccurrenceStatus
import com.osscabinet.ossWallet.domain.model.PaymentSchedule
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.PlannedPaymentOccurrence
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
import com.osscabinet.ossWallet.domain.model.RecurrenceRule
import com.osscabinet.ossWallet.domain.util.joinByComma

fun PlannedPaymentWithTemplate.toDomain(): PlannedPayment = PlannedPayment(
    id = payment.id,
    templateId = payment.templateId,
    template = template.toDomain(),
    schedule = PaymentSchedule(
        recurrenceRule = RecurrenceRule(
            type = com.osscabinet.ossWallet.domain.model.RecurrenceType.valueOf(payment.schedule.recurrenceType),
            interval = payment.schedule.interval,
            dayOfWeek = payment.schedule.dayOfWeek,
            dayOfMonth = payment.schedule.dayOfMonth,
            cronExpression = payment.schedule.recurrenceCronExpression
        ),
        startDate = payment.schedule.startDate,
        endDate = payment.schedule.endDate,
        nextDueDate = payment.schedule.nextDueDate,
        lastTriggeredDate = payment.schedule.lastTriggeredDate
    ),
    autoConfirm = payment.autoConfirm,
    status = PlannedPaymentStatus.valueOf(payment.status),
    profileId = payment.profileId
)

fun PlannedPayment.toEntity(): PlannedPaymentEntity = PlannedPaymentEntity(
    id = id,
    templateId = templateId,
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
