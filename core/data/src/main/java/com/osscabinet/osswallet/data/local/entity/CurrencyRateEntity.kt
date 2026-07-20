package com.osscabinet.ossWallet.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

/**
 * Composite key (currencyCode, baseCurrency) — ARC-08. Different base currencies
 * produce different rate sets and must coexist in the cache; using currencyCode
 * alone as the key let one base's fetch silently overwrite another's row.
 */
@Entity(
    tableName = "currency_rates",
    primaryKeys = ["currencyCode", "baseCurrency"],
    indices = [Index(value = ["baseCurrency"])] // ARC-10
)
data class CurrencyRateEntity(
    val currencyCode: String,
    val baseCurrency: String,
    @ColumnInfo(name = "rate")
    val rate: String,
    val timestamp: Long,
    val nextUpdate: Long
)
