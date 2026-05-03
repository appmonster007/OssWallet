package com.osscabinet.ossWallet.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(
    tableName = "budgets",
    indices = [
        Index(value = ["profileId"]),
        Index(value = ["isArchived"])
    ]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val currency: String,
    val limitAmount: BigDecimal,
    val period: String,
    val categoryIds: String = "",
    val accountIds: String = "",
    val tags: String = "",
    val alertThreshold: Float = 0.8f,
    val rollover: Boolean = false,
    val trackType: String = "EXPENSE",
    val isArchived: Boolean = false,
    val profileId: Int,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null
)
