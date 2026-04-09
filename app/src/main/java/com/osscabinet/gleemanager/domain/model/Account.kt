package com.osscabinet.gleemanager.domain.model

data class Account(
    val id: String,
    val name: String,
    val balance: Double,
    val currencyCode: String,
    val accountType: String,
    val colorHex: Int
)
