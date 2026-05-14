package com.osscabinet.ossWallet.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
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
    val categoryIds: String = "", // Comma-separated IDs
    val accountIds: String = "", // Comma-separated IDs
    val startDate: Long? = null,
    val endDate: Long? = null,
    val paymentMethods: String = "", // Comma-separated methods
    val hasWarranty: Boolean? = null,
    val warrantyExpirationStart: Long? = null,
    val warrantyExpirationEnd: Long? = null,
    val minAmount: BigDecimal? = null,
    val maxAmount: BigDecimal? = null,
    val transactionType: TransactionType? = null, // null for all
    val currencies: String = "", // Comma-separated currency codes
    val statuses: String = "", // Comma-separated status values
    val linkedTransactionIds: String = "", // Comma-separated IDs
    val tags: String = "", // Comma-separated tags
    val excludeFuture: Boolean = true
)
