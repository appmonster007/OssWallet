package com.osscabinet.ossWallet.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import java.math.BigDecimal

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["categoryId"]),
        Index(value = ["profileId"]),
        Index(value = ["timestamp"]),
        Index(value = ["status"]),
        Index(value = ["profileId", "timestamp"]),
        Index(value = ["profileId", "accountId"]),
        Index(value = ["profileId", "lastUpdated"]),
        Index(value = ["importBatchId"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val amount: BigDecimal,
    val timestamp: Long,
    val currency: String,
    val transactionType: TransactionType,
    val accountId: String? = null,
    val categoryId: Long,
    val tags: Set<String> = emptySet(),
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val payee: String = "",
    val note: String = "",
    val sourcePlannedPaymentId: Long? = null,
    val profileId: Int = 1,
    val gpsLocation: String? = null,
    val location: String? = null,
    val warrantyExpiration: Long? = null,
    val status: TransactionStatus = TransactionStatus.CLEARED,
    val targetAccountId: String? = null,
    val targetCurrency: String? = null,
    val transferRate: BigDecimal = BigDecimal.ONE,
    val lastUpdated: Long = System.currentTimeMillis(),
    val importBatchId: String? = null
)
