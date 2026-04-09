package com.osscabinet.gleemanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val category: String,
    val timestamp: Long,
    val currency: String,
    val isIncome: Boolean,
    val accountId: String,
    val categoryId: Long? = null,
    val tags: String = "", // Comma-separated tags
    val profileId: Int = 1
)
