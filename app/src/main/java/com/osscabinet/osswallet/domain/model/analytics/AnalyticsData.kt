package com.osscabinet.ossWallet.domain.model.analytics

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * Category breakdown data for pie chart card.
 * Represents the net balance (income - expense) for each category.
 */
@Serializable
data class CategoryBreakdownData(
    val category: String,
    val categoryId: Long?,
    @Contextual val netBalance: BigDecimal,
    @Contextual val income: BigDecimal,
    @Contextual val expense: BigDecimal,
    val colorHex: Int,
    val percentage: Float,
    val hasSubcategories: Boolean = false,
    val subcategoryData: List<CategoryBreakdownData>? = null
) {
    fun allCategoryIds(): List<String> {
        val ids = mutableListOf<String>()
        categoryId?.let { ids.add(it.toString()) }
        subcategoryData?.forEach { 
            ids.addAll(it.allCategoryIds())
        }
        return ids
    }
}

/**
 * Timeline trajectory data for multi-line chart card.
 * Represents balance, expense, and income at a specific point in time.
 */
@Serializable
data class TimelineTrajectoryData(
    val timestamp: Long,
    @Contextual val balance: BigDecimal,
    @Contextual val expense: BigDecimal,
    @Contextual val income: BigDecimal
)

/**
 * Category expense data for treemap card.
 * Represents expense distribution across categories for visualization.
 */
@Serializable
data class CategoryExpenseTreemapData(
    val category: String,
    val categoryId: Long?,
    @Contextual val expense: BigDecimal,
    val colorHex: Int,
    val percentage: Float,
    val hasSubcategories: Boolean = false,
    val subcategoryData: List<CategoryExpenseTreemapData>? = null
) {
    fun allCategoryIds(): List<String> {
        val ids = mutableListOf<String>()
        categoryId?.let { ids.add(it.toString()) }
        subcategoryData?.forEach { 
            ids.addAll(it.allCategoryIds())
        }
        return ids
    }
}
