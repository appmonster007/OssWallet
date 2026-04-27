package com.osscabinet.ossWallet.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.osscabinet.ossWallet.domain.model.BudgetPeriod
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import java.math.BigDecimal

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val limit: BigDecimal,
    val spent: BigDecimal,
    val currency: String,
    val period: BudgetPeriod,
    val filterJson: String, // TransactionFilter serialized
    val alertThresholdsJson: String, // Set<Float> serialized
    val isPredictiveAlertEnabled: Boolean,
    val startDate: Long?,
    val endDate: Long?,
    val profileId: Int = 1,
    val lastEvaluated: Long = System.currentTimeMillis()
)
