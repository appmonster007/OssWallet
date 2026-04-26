package com.osscabinet.ossWallet.data.mapper

import com.osscabinet.ossWallet.data.local.entity.BudgetEntity
import com.osscabinet.ossWallet.domain.model.Budget

/**
 * Mappers for Budget data.
 */
fun BudgetEntity.toDomain() = Budget(category, spent, limit, currency)

fun Budget.toEntity() = BudgetEntity(category, spent, limit, currency)
