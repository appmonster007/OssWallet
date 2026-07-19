package com.osscabinet.ossWallet.data.web.dto

import kotlinx.serialization.Serializable

@Serializable
data class AccountDto(
    val id: String,
    val name: String,
    val currency: String,
    val balance: Double,
    val type: String,
    val colorHex: String = "#42A5F5",
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val transferIn: Double = 0.0,
    val transferOut: Double = 0.0,
    val distribution: Map<String, Double> = emptyMap()
)
