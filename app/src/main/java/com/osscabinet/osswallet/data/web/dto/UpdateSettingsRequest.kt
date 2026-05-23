package com.osscabinet.ossWallet.data.web.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateSettingsRequest(
    val baseCurrency: String? = null,
    val decimalPrecision: Int? = null,
    val theme: String? = null,
    val selectedFont: String? = null,
    val use24HourFormat: Boolean? = null,
    val userName: String? = null,
    val mapsAccessEnabled: Boolean? = null,
    val autoSaveExchangeRates: Boolean? = null,
    val cardAnimationEnabled: Boolean? = null,
    val currencyProvider: String? = null
)
