package com.osscabinet.ossWallet.data.web.dto

import kotlinx.serialization.Serializable

@Serializable
data class CategoryBreakdownDto(
    val categoryId: Long,
    val categoryName: String,
    val amount: Double,
    val percentage: Double,
    val transactionCount: Int,
    val colorHex: String? = null,
    val subcategories: List<CategoryBreakdownDto> = emptyList()
)
