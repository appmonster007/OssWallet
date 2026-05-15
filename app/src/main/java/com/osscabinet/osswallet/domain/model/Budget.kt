package com.osscabinet.ossWallet.domain.model

import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import java.math.BigDecimal

enum class BudgetAlertLevel { NONE, WARNING, EXCEEDED }

data class Budget(
    val id: Long = 0,
    val name: String,
    val currency: String,
    val limitAmount: BigDecimal,
    val timelineRange: TimelineRange,
    val categoryIds: Set<Long> = emptySet(),
    val accountIds: Set<String> = emptySet(),
    val tags: Set<String> = emptySet(),
    val paymentMethods: Set<PaymentMethod> = emptySet(),
    val statuses: Set<TransactionStatus> = emptySet(),
    val currencies: Set<String> = emptySet(),
    val linkedTransactionIds: Set<Long> = emptySet(),
    val alertThreshold: Float = 0.8f,
    val rollover: Boolean = false,
    val trackTypes: Set<TransactionType> = setOf(TransactionType.EXPENSE),
    val isArchived: Boolean = false,
    val profileId: Int,
    val note: String = "",
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val filterId: Long? = null
)

data class BudgetProgress(
    val budget: Budget,
    val spent: BigDecimal,
    val limit: BigDecimal,
    val remaining: BigDecimal,
    val percentage: Float,
    val alertLevel: BudgetAlertLevel,
    val rolloverAmount: BigDecimal = BigDecimal.ZERO
)
