package com.osscabinet.ossWallet.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "currency_rates")
data class CurrencyRateEntity(
    @PrimaryKey
    val currencyCode: String,
    val baseCurrency: String,
    @ColumnInfo(name = "rate")
    val rate: String,
    val timestamp: Long,
    val nextUpdate: Long
)
