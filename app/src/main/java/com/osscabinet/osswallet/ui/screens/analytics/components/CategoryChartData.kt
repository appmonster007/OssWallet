package com.osscabinet.ossWallet.ui.screens.analytics.components

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

/** Shared UI model for category breakdown charts (pie chart and treemap). */
@Serializable
data class CategoryChartData(
    val category: String,
    val categoryId: Long?,
    @Contextual val amount: BigDecimal,
    val colorHex: Int,
    val percentage: Float,
    val hasSubcategories: Boolean = false,
    val subcategoryCount: Int = 0,
    val subcategoryData: List<CategoryChartData>? = null,
    @Contextual val previousAmount: BigDecimal? = null
) {
    fun allCategoryIds(): List<String> {
        val ids = mutableListOf<String>()
        categoryId?.let { ids.add(it.toString()) }
        subcategoryData?.forEach { ids.addAll(it.allCategoryIds()) }
        return ids
    }
}
