package com.osscabinet.ossWallet.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Entity(tableName = "transactions_fts")
@Fts4(contentEntity = TransactionEntity::class)
data class TransactionFtsEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowId: Long = 0,
    val note: String,
    val payee: String,
    val name: String
)
