package com.osscabinet.ossWallet.domain.model

import java.math.BigDecimal

data class CurrencySummary(
    val currency: String,
    val totalIncome: BigDecimal,
    val totalExpense: BigDecimal
)
