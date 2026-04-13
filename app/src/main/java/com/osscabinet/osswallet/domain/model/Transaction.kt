package com.osscabinet.ossWallet.domain.model

import java.math.BigDecimal

data class Transaction(
    val id: Long = 0,
    val title: String,
    val amount: BigDecimal,
    val category: String,
    val timestamp: Long,
    val currency: String,
    val isIncome: Boolean,
    val accountId: String,
    val categoryId: Long? = null,
    val tags: List<String> = emptyList(),
    val paymentMethod: String = "Cash",
    val payee: String = "",
    val note: String = "",
    val recurring: String? = null,
    val linkedTransactionId: Long? = null
)

data class TransactionWithBalance(
    val transaction: Transaction,
    val balanceAfter: BigDecimal
)
