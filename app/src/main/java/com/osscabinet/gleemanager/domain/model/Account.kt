package com.osscabinet.gleemanager.domain.model

data class Account(
    val id: String,
    val name: String,
    val balance: Double,
    val currencyCode: String,
    val structureType: String,
    val colorHex: String,
    val icon: String,
    val allowedCurrencies: List<String> = emptyList()
)
