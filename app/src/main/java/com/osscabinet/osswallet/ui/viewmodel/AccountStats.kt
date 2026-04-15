package com.osscabinet.ossWallet.ui.viewmodel

import java.math.BigDecimal

data class AccountStats(
    val currencyBalances: Map<String, BigDecimal>,
    val incomeInAccountCurrency: BigDecimal,
    val expenseInAccountCurrency: BigDecimal
)
