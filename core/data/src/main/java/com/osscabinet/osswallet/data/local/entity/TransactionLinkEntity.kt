package com.osscabinet.ossWallet.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Junction table for many-to-many relationship between transactions.
 * Allows linking multiple transactions together (e.g., transfers, clones, manual links).
 */
@Entity(
    tableName = "transaction_links",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["linkedTransactionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["transactionId"]),
        Index(value = ["linkedTransactionId"]),
        Index(value = ["transactionId", "linkedTransactionId"], unique = true)
    ]
)
data class TransactionLinkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionId: Long,
    val linkedTransactionId: Long
)
