package com.osscabinet.ossWallet.data.web.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateSettingsRequest(
    val baseCurrency: String? = null,
    val decimalPrecision: Int? = null
)
