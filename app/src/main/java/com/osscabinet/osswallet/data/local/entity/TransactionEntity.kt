package com.osscabinet.ossWallet.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
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
        Index(value = ["status"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val amount: BigDecimal,
    val timestamp: Long,
    val currency: String,
    val transactionType: String, // "income" or "expense"
    val accountId: String? = null,
    val categoryId: Long,
    val tags: String = "", // Comma-separated tags
    val paymentMethod: String = "Cash",
    val payee: String = "",
    val note: String = "",
    val sourcePlannedPaymentId: Long? = null,
    val profileId: Int = 1,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val warrantyExpiration: Long? = null,
    val status: String = "cleared"
)
