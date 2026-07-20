package com.osscabinet.ossWallet.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "card_cache",
    indices = [
        Index(value = ["profileId", "currency", "cardId"], unique = true)
    ]
)
data class CardCacheEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Int,
    val currency: String,
    val cardId: String,
    val dataJson: String,
    val version: Long, // Matches max(lastUpdated) of transactions
    val timestamp: Long = System.currentTimeMillis()
)
