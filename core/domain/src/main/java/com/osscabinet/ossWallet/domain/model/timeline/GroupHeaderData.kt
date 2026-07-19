package com.osscabinet.ossWallet.domain.model.timeline

import com.osscabinet.ossWallet.domain.model.util.BigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class GroupHeaderData(
    val title: String,
    val subtitle: String,
    val incomeByCurrency: Map<String, @Serializable(with = BigDecimalSerializer::class) BigDecimal>,
    val expenseByCurrency: Map<String, @Serializable(with = BigDecimalSerializer::class) BigDecimal>,
    val openingBalancesByCurrency: Map<String, @Serializable(with = BigDecimalSerializer::class) BigDecimal>,
    val closingBalancesByCurrency: Map<String, @Serializable(with = BigDecimalSerializer::class) BigDecimal>,
    val allCurrencies: List<String>,
    @Serializable(with = BigDecimalSerializer::class)
    val incomeBase: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val expenseBase: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val netBase: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val totalOpeningBalanceBase: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val totalClosingBalanceBase: BigDecimal,
)
