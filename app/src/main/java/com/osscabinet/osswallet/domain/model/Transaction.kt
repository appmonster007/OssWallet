package com.osscabinet.ossWallet.domain.model

import java.math.BigDecimal

interface Timestamped {
    val timestamp: Long
}

data class Transaction(
    val id: Long = 0,
    val title: String,
    val amount: BigDecimal,
    val category: String,
    override val timestamp: Long,
    val currency: String,
    val transactionType: String, // "income" or "expense"
    val accountId: String,
    val categoryId: Long? = null,
    val tags: List<String> = emptyList(),
    val paymentMethod: String = "Cash",
    val payee: String = "",
    val note: String = "",
    val recurring: String? = null,
    val linkedTransactionId: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val warrantyExpiration: Long? = null
) : Timestamped

data class TransactionWithBalance(
    val transaction: Transaction,
    val balanceBefore: BigDecimal,
    val balanceAfter: BigDecimal
)
