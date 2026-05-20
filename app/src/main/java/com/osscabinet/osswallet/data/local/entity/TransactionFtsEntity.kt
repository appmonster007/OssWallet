package com.osscabinet.ossWallet.data.local.entity

import androidx.room.Entity
import androidx.room.Fts4

@Entity(tableName = "transactions_fts")
@Fts4(contentEntity = TransactionEntity::class)
data class TransactionFtsEntity(
    val note: String,
    val payee: String,
    val name: String
)
