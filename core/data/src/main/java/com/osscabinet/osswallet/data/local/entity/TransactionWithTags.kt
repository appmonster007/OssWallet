package com.osscabinet.ossWallet.data.local.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/**
 * A data class that represents a transaction along with its associated tags.
 * This is used by Room to perform a many-to-many join using the cross-reference table.
 */
data class TransactionWithTags(
    @Embedded
    val transaction: TransactionEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = TransactionTagCrossRef::class,
            parentColumn = "transactionId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity>
)
