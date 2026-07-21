package com.osscabinet.ossWallet.data.web.dto

import com.osscabinet.ossWallet.domain.model.AccountStructureType
import kotlinx.serialization.Serializable

@Serializable
data class CreateAccountRequest(
    val name: String,
    val currencyCode: String,
    val structureType: String = AccountStructureType.BANK.name,
    val colorHex: String = AccountStructureType.BANK.colorHex,
    val icon: String = AccountStructureType.BANK.iconKey,
    val allowedCurrencies: List<String> = emptyList(),
    val initialBalance: Double? = null
)

@Serializable
data class UpdateAccountRequest(
    val name: String? = null,
    val currencyCode: String? = null,
    val structureType: String? = null,
    val colorHex: String? = null,
    val icon: String? = null,
    val isArchived: Boolean? = null,
    val allowedCurrencies: List<String>? = null
)
