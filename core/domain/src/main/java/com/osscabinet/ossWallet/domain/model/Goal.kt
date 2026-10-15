package com.osscabinet.ossWallet.domain.model

import com.osscabinet.ossWallet.domain.model.util.BigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * A savings target. Progress is tracked by manual contributions into [savedAmount];
 * it is deliberately independent of account balances so withdrawing from an account
 * never silently shrinks a goal.
 */
@Serializable
data class Goal(
    val id: Long = 0,
    val name: String,
    @Serializable(with = BigDecimalSerializer::class)
    val targetAmount: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val savedAmount: BigDecimal = BigDecimal.ZERO,
    val currency: String,
    val deadline: Long? = null,
    val note: String = "",
    val profileId: Int,
    val createdAt: Long,
    /** Set when [savedAmount] first reaches [targetAmount]; null again if it drops below. */
    val completedAt: Long? = null
)

enum class GoalStatus { COMPLETED, ON_TRACK, BEHIND, OVERDUE, NO_DEADLINE }

@Serializable
data class GoalProgress(
    val goal: Goal,
    /** 0..1 for rings and bars; capped at 1 once the target is met. */
    val fraction: Float,
    @Serializable(with = BigDecimalSerializer::class)
    val remaining: BigDecimal,
    /** Whole days until the deadline; negative once passed; null without a deadline. */
    val daysLeft: Long?,
    /** Saving needed per 30 days to land on the deadline; null when done or no deadline. */
    @Serializable(with = BigDecimalSerializer::class)
    val requiredPerMonth: BigDecimal?,
    val status: GoalStatus
)
