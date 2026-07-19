package com.osscabinet.ossWallet.data.web.dto

import kotlinx.serialization.Serializable

@Serializable
data class AccountDetailDto(
    val account: AccountDto,
    val recentTransactions: List<TransactionDto> = emptyList()
)
