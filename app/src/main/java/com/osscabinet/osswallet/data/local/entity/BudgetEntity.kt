package com.osscabinet.ossWallet.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val category: String,
    val spent: BigDecimal,
    val limit: BigDecimal,
    val currency: String,
    val profileId: Int = 1
)
