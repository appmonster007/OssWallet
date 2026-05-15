package com.osscabinet.ossWallet.domain.model

import java.math.BigDecimal

enum class BudgetPeriod { DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY, CUSTOM }
enum class BudgetAlertLevel { NONE, WARNING, EXCEEDED }

data class Budget(
    val id: Long = 0,
    val name: String,
    val currency: String,
    val limitAmount: BigDecimal,
    val period: BudgetPeriod,
    val categoryIds: List<Long> = emptyList(),
    val accountIds: List<String> = emptyList(),
    val tags: Set<String> = emptySet(),
    val paymentMethods: List<PaymentMethod> = emptyList(),
    val statuses: List<TransactionStatus> = emptyList(),
    val currencies: List<String> = emptyList(),
    val linkedTransactionIds: List<Long> = emptyList(),
    val alertThreshold: Float = 0.8f,
    val rollover: Boolean = false,
    val trackTypes: List<TransactionType> = listOf(TransactionType.EXPENSE),
    val isArchived: Boolean = false,
    val profileId: Int,
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
