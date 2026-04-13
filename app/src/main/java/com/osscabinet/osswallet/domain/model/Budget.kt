package com.osscabinet.ossWallet.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

data class Budget(
    val category: String,
    val spent: BigDecimal,
    val limit: BigDecimal,
    val currency: String = "USD"
) {
    val progress: Float
        get() = if (limit.signum() > 0) (spent.divide(limit, 4, RoundingMode.HALF_UP)).toFloat() else 0f

    val remaining: BigDecimal
        get() = (limit.subtract(spent)).max(BigDecimal.ZERO)

    val isOverBudget: Boolean
        get() = spent.compareTo(limit) > 0
}
