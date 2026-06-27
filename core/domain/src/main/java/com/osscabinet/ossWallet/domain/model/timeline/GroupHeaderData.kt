package com.osscabinet.ossWallet.domain.model.timeline

import java.math.BigDecimal

data class GroupHeaderData(
    val title: String,
    val subtitle: String,
    val incomeByCurrency: Map<String, BigDecimal>,
    val expenseByCurrency: Map<String, BigDecimal>,
    val openingBalancesByCurrency: Map<String, BigDecimal>,
    val closingBalancesByCurrency: Map<String, BigDecimal>,
    val allCurrencies: List<String>,
    val incomeBase: BigDecimal,
    val expenseBase: BigDecimal,
    val netBase: BigDecimal,
    val totalOpeningBalanceBase: BigDecimal,
    val totalClosingBalanceBase: BigDecimal,
)
