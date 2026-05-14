package com.osscabinet.ossWallet.domain.model

data class TransactionSplit(
    val categoryId: Long? = null,
    val amount: String = "",
    val note: String = ""
)
