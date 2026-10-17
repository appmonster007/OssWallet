package com.osscabinet.ossWallet.domain.model

data class Filter(
    val id: Long = 0,
    val name: String,
    val filter: TransactionFilter,
    /** Whose filter this is; saved filters (and the ones budgets and loans keep) belong to one profile. */
    val profileId: Int = 1
)
