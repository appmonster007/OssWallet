package com.osscabinet.ossWallet.domain.model

import java.math.BigDecimal

enum class BudgetPeriod { WEEKLY, MONTHLY, QUARTERLY, YEARLY, CUSTOM }
enum class BudgetTrackType { EXPENSE, INCOME }
enum class BudgetAlertLevel { NONE, WARNING, EXCEEDED }

data class Budget(
    val id: Long = 0,
    val name: String,
    val currency: String,
    val limitAmount: BigDecimal,
    val period: BudgetPeriod,
    val categoryIds: List<Long> = emptyList(),
    val accountIds: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val alertThreshold: Float = 0.8f,
    val rollover: Boolean = false,
    val trackTypes: List<BudgetTrackType> = listOf(BudgetTrackType.EXPENSE),
    val isArchived: Boolean = false,
    val profileId: Int,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null
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
