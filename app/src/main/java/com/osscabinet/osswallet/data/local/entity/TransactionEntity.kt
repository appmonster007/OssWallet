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
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["categoryId"]),
        Index(value = ["profileId"]),
        Index(value = ["timestamp"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: BigDecimal,
    val category: String,
    val timestamp: Long,
    val currency: String,
    val isIncome: Boolean,
    val accountId: String,
    val categoryId: Long? = null,
    val tags: String = "", // Comma-separated tags
    val paymentMethod: String = "Cash",
    val payee: String = "",
    val note: String = "",
    val recurring: String? = null,
    val linkedTransactionId: Long? = null,
    val profileId: Int = 1,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val warrantyExpiration: Long? = null
)
