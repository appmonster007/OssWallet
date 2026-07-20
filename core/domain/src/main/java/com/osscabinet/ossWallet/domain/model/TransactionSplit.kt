package com.osscabinet.ossWallet.domain.model

import java.math.BigDecimal

data class TransactionSplit(
    val categoryId: Long? = null,
    val amount: String = "0",
    val note: String = ""
)

sealed class SplitStatus {
    object Drafting : SplitStatus()
    data class Splitting(val remaining: BigDecimal) : SplitStatus()
    object Balanced : SplitStatus()
    data class OverAllocated(val excess: BigDecimal) : SplitStatus()

    companion object {
        fun compute(total: BigDecimal, splits: List<TransactionSplit>): SplitStatus {
            if (splits.isEmpty()) return Drafting

            val currentSum = splits.sumOf { it.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO }
            val diff = total - currentSum

            return when {
                diff.signum() == 0 -> Balanced
                diff.signum() < 0 -> OverAllocated(diff.abs())
                else -> Splitting(diff)
            }
        }
    }
}
