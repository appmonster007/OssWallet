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
    /**
     * Optimized collection of all leaf category IDs under this node.
     * Uses a sequence to avoid intermediate list allocations during deep recursion.
     */
    fun allCategoryIds(): List<String> {
        return allCategoryIdsSequence().toList()
    }

    private fun allCategoryIdsSequence(): Sequence<String> = sequence {
        categoryId?.let { yield(it.toString()) }
        subcategoryData?.forEach { yieldAll(it.allCategoryIdsSequence()) }
    }
}
