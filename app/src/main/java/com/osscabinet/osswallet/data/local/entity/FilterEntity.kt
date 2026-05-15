package com.osscabinet.ossWallet.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import java.math.BigDecimal

@Entity(tableName = "transaction_filters")
data class FilterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val timelineRange: TimelineRange = TimelineRange.AllTime,
    val queryNote: String? = null,
    val isNoteRegex: Boolean = false,
    val queryPayee: String? = null,
    val isPayeeRegex: Boolean = false,
    val categoryIds: Set<Long> = emptySet(),
    val accountIds: Set<String> = emptySet(),
    val startDate: Long? = null,
    val endDate: Long? = null,
    val paymentMethods: Set<PaymentMethod> = emptySet(),
    val hasWarranty: Boolean? = null,
    val warrantyExpirationStart: Long? = null,
    val warrantyExpirationEnd: Long? = null,
    val minAmount: BigDecimal? = null,
    val maxAmount: BigDecimal? = null,
    val transactionTypes: Set<TransactionType> = emptySet(),
    val currencies: Set<String> = emptySet(),
    val statuses: Set<TransactionStatus> = emptySet(),
    val linkedTransactionIds: Set<Long> = emptySet(),
    val tags: Set<String> = emptySet(),
    val excludeFuture: Boolean = true
)
