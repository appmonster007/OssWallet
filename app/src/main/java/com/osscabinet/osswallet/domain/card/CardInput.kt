package com.osscabinet.ossWallet.domain.card

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import java.math.BigDecimal

/**
 * The ONE shared input for all card computations.
 * Wraps all raw financial data needed for any card.
 */
data class CardInput(
    val transactions: List<Transaction>,
    val accounts: List<Account>,
    val categories: List<Category>,
    val exchangeRates: Map<String, BigDecimal>,
    val baseCurrency: String,
    val precision: Int,
    val timelineRange: TimelineRange = TimelineRange.AllTime,
    val filter: TransactionFilter = TransactionFilter(),
    val scopeAccountId: String? = null
)
