package com.osscabinet.ossWallet.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val category: String,
    val spent: Double,
    val limit: Double,
    val currency: String
)
