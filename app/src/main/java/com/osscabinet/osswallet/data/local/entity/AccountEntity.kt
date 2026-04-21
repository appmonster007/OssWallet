package com.osscabinet.ossWallet.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(
    tableName = "accounts",
    indices = [Index(value = ["profileId"])]
)
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val balance: BigDecimal,
    val currencyCode: String,
    val structureType: String,
    val colorHex: String,
    val icon: String,
    val allowedCurrencies: String, // JSON or Comma separated
    val profileId: Int = 1,
    val isArchived: Boolean = false,
    val displayOrder: Int = 0
)
