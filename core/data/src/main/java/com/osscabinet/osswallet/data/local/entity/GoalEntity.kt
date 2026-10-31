package com.osscabinet.ossWallet.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(
    tableName = "goals",
    indices = [Index(value = ["profileId"])]
)
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetAmount: BigDecimal,
    val savedAmount: BigDecimal,
    val currency: String,
    val deadline: Long? = null,
    val note: String = "",
    val profileId: Int,
    val createdAt: Long,
    val completedAt: Long? = null
)
