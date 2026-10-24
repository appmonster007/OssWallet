package com.osscabinet.ossWallet.domain.model

import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.model.util.BigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

enum class BudgetAlertLevel { NONE, WARNING, EXCEEDED }

/** How a budget is doing against both its limit and how far through its period we are. */
enum class BudgetPaceStatus { ON_TRACK, AHEAD_OF_PACE, NEAR_LIMIT, OVER }

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
    val advanced: AdvancedFilterCriteria = AdvancedFilterCriteria(),
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
    val rolloverAmount: BigDecimal = BigDecimal.ZERO,
    // Share (0..1) of the budget's current period that has passed; null for All Time budgets.
    val elapsedFraction: Float? = null,
    // Future-dated transactions + due planned payments matching this budget's scope, within its
    // current period. Shown separately from [spent] — it's committed, not yet actually spent.
    @Serializable(with = BigDecimalSerializer::class)
    val plannedAmount: BigDecimal = BigDecimal.ZERO
)
