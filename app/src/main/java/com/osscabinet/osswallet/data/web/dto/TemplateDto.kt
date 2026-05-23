package com.osscabinet.ossWallet.data.web.dto

import kotlinx.serialization.Serializable

@Serializable
data class TemplateDto(
    val id: Long,
    val name: String,
    val categoryId: Long,
    val amount: Double,
    val transactionType: String,
    val currency: String,
    val accountId: String? = null,
    val targetAccountId: String? = null,
    val targetCurrency: String? = null,
    val transferRate: Double = 1.0,
    val note: String? = null,
    val payee: String? = null,
    val location: String? = null,
    val gpsLocation: String? = null,
    val warrantyExpiration: Long? = null,
    val transactionStatus: String = "CLEARED",
    val paymentMethod: String? = null,
    val tags: List<String> = emptyList(),
    val icon: String? = null,
    val colorHex: String? = null,
    val isStarred: Boolean = false,
    val displayOrder: Int = 0
)

@Serializable
data class CreateTemplateRequest(
    val name: String,
    val categoryId: Long,
    val amount: Double,
    val transactionType: String,
    val currency: String,
    val accountId: String? = null,
    val targetAccountId: String? = null,
    val targetCurrency: String? = null,
    val transferRate: Double = 1.0,
    val note: String? = null,
    val payee: String? = null,
    val location: String? = null,
    val gpsLocation: String? = null,
    val warrantyExpiration: Long? = null,
    val transactionStatus: String = "CLEARED",
    val paymentMethod: String? = null,
    val tags: List<String> = emptyList(),
    val icon: String? = null,
    val colorHex: String? = null,
    val isStarred: Boolean = false,
    val displayOrder: Int = 0
)

@Serializable
data class UpdateTemplateRequest(
    val name: String? = null,
    val categoryId: Long? = null,
    val amount: Double? = null,
    val transactionType: String? = null,
    val currency: String? = null,
    val accountId: String? = null,
    val targetAccountId: String? = null,
    val targetCurrency: String? = null,
    val transferRate: Double? = null,
    val note: String? = null,
    val payee: String? = null,
    val location: String? = null,
    val gpsLocation: String? = null,
    val warrantyExpiration: Long? = null,
    val transactionStatus: String? = null,
    val paymentMethod: String? = null,
    val tags: List<String>? = null,
    val icon: String? = null,
    val colorHex: String? = null,
    val isStarred: Boolean? = null,
    val displayOrder: Int? = null
)

@Serializable
data class AddCardRequest(
    val cardType: String = ""
)

@Serializable
data class ReorderCardsRequest(
    val cardIds: List<String> = emptyList()
)

@Serializable
data class ReorderAccountsRequest(
    val accountIds: List<String> = emptyList()
)

@Serializable
data class ReorderCategoriesRequest(
    val categoryIds: List<Long> = emptyList()
)
