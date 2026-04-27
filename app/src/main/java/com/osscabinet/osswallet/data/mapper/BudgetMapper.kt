package com.osscabinet.ossWallet.data.mapper

import com.osscabinet.ossWallet.data.local.entity.BudgetEntity
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.model.BudgetPeriod
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Mappers for Budget data.
 */
fun BudgetEntity.toDomain() = Budget(
    id = id,
    name = name,
    limit = limit,
    spent = spent,
    currency = currency,
    period = period,
    filter = Json.decodeFromString(filterJson),
    alertThresholds = Json.decodeFromString(alertThresholdsJson),
    isPredictiveAlertEnabled = isPredictiveAlertEnabled,
    startDate = startDate,
    endDate = endDate,
    lastEvaluated = lastEvaluated
)

fun Budget.toEntity() = BudgetEntity(
    id = id,
    name = name,
    limit = limit,
    spent = spent,
    currency = currency,
    period = period,
    filterJson = Json.encodeToString(filter),
    alertThresholdsJson = Json.encodeToString(alertThresholds),
    isPredictiveAlertEnabled = isPredictiveAlertEnabled,
    startDate = startDate,
    endDate = endDate,
    lastEvaluated = lastEvaluated
)
