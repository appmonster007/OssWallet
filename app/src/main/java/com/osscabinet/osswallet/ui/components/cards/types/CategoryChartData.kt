package com.osscabinet.ossWallet.ui.components.cards.types

import java.math.BigDecimal

/** Shared UI model for category breakdown charts (pie chart and treemap). */
data class CategoryChartData(
    val category: String,
    val categoryId: Long?,
    val amount: BigDecimal,
    val colorHex: Int,
    val percentage: Float,
    val hasSubcategories: Boolean = false,
    val subcategoryCount: Int = 0,
    val subcategoryData: List<CategoryChartData>? = null,
    val previousAmount: BigDecimal? = null
) {
    fun allCategoryIds(): List<String> {
        val ids = mutableListOf<String>()
        categoryId?.let { ids.add(it.toString()) }
        subcategoryData?.forEach { ids.addAll(it.allCategoryIds()) }
        return ids
    }
}
