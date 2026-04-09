package com.osscabinet.gleemanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val balance: Double,
    val currencyCode: String,
    val accountType: String,
    val colorHex: Int,
    val profileId: Int = 1
)
