package com.osscabinet.ossWallet.domain.model

import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.model.util.BigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

enum class BudgetAlertLevel { NONE, WARNING, EXCEEDED }

@Serializable
data class Budget(
    val id: Long = 0,
    val name: String,
    val currency: String,
    @Serializable(with = BigDecimalSerializer::class)
    val limitAmount: BigDecimal,
    val timelineRange: TimelineRange,
    val categoryIds: Set<Long> = emptySet(),
    val accountIds: Set<String> = emptySet(),
    val tags: Set<String> = emptySet(),
    val matchAllTags: Boolean = true,
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

@Serializable
data class BudgetProgress(
    val budget: Budget,
    @Serializable(with = BigDecimalSerializer::class)
    val spent: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val limit: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val remaining: BigDecimal,
    val percentage: Float,
    val alertLevel: BudgetAlertLevel,
    @Serializable(with = BigDecimalSerializer::class)
    val rolloverAmount: BigDecimal = BigDecimal.ZERO
)
