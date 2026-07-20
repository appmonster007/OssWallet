package com.osscabinet.ossWallet.data.web.dto

import kotlinx.serialization.Serializable

@Serializable
data class SettingsDto(
    val currentProfileId: Int,
    val baseCurrency: String,
    val decimalPrecision: Int
)
