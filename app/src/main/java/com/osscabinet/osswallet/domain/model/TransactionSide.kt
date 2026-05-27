package com.osscabinet.ossWallet.domain.model

import java.math.BigDecimal

/**
 * Represents one side (leg) of a transaction.
 * Used to group related fields for state management in entry screens.
 */
data class TransactionSide(
    val accountId: String? = null,
    val amount: String = "",
    val currency: String = "",
    val categoryId: Long = Category.SYSTEM_UNKNOWN
) {
    val amountValue: BigDecimal
        get() = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
}
