package com.osscabinet.ossWallet.domain.model

data class Filter(
    val id: Long = 0,
    val name: String,
    val filter: TransactionFilter
)
