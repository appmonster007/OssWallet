package com.osscabinet.ossWallet.widget

import com.osscabinet.ossWallet.domain.service.financial.CategoryTreeMetrics
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Flattened, widget-local, top-level-only slice — the widget-process analog of
 * `ui/components/cards/types/CategoryChartData.kt`. Deliberately NOT imported from `ui/` (widget/
 * must not depend on ui/ per the Phase 3 task) — this is a from-scratch port of just the pure
 * flattening math in `UnifiedCardRenderer.mapCategoryMetrics()`, trimmed to what
 * CATEGORY_PIE_CHART / CATEGORY_TREEMAP need: top-level slices only, amounts pre-formatted,
 * subcategories excluded (a pie slice / treemap cell can't usefully drill in-widget anyway).
 */
@Serializable
data class WidgetCategorySlice(
    val category: String,
    val categoryId: Long?,
    val amount: Double, // absolute value, base currency, for bitmap proportions
    val amountFormatted: String,
    val colorHex: String, // "#RRGGBB" or "#AARRGGBB", ready for Color.parseColor
    val percentage: Float
)

object CategoryChartDataMapper {

    /**
     * Flattens [items] (recursively, including subcategories — matching
     * `UnifiedCardRenderer.mapCategoryMetrics()`'s own recursion) but returns only the top-level
     * slices, each carrying the recursively-summed amount of itself + its subtree folded into the
     * top ancestor (since the widget never drills into a bitmap region). Percentages are scaled
     * against the selected metric's global total exactly as the Compose path does.
     */
    fun flattenTopLevel(
        items: List<CategoryTreeMetrics>,
        selectedMetric: com.osscabinet.ossWallet.domain.model.settings.ChartMetric,
        formatAmount: (BigDecimal) -> String
    ): List<WidgetCategorySlice> {
        fun metricOf(m: com.osscabinet.ossWallet.domain.service.financial.FinancialSummary): BigDecimal =
            when (selectedMetric) {
                com.osscabinet.ossWallet.domain.model.settings.ChartMetric.INCOME -> m.income
                com.osscabinet.ossWallet.domain.model.settings.ChartMetric.EXPENSE -> m.expense
                com.osscabinet.ossWallet.domain.model.settings.ChartMetric.NET -> m.netFlow
                com.osscabinet.ossWallet.domain.model.settings.ChartMetric.TRANSFER -> m.transfer
                com.osscabinet.ossWallet.domain.model.settings.ChartMetric.TRANSFER_IN -> m.transferIn
                com.osscabinet.ossWallet.domain.model.settings.ChartMetric.TRANSFER_OUT -> m.transferOut
                com.osscabinet.ossWallet.domain.model.settings.ChartMetric.INFLOW -> m.inflow
                com.osscabinet.ossWallet.domain.model.settings.ChartMetric.OUTFLOW -> m.outflow
            }

        // Sum this node + all descendants (recursive) into one top-level amount.
        fun sumSubtree(node: CategoryTreeMetrics): BigDecimal {
            var total = metricOf(node.metrics)
            node.subcategoryData.forEach { total = total.add(sumSubtree(it)) }
            return total
        }

        val globalTotalAbs = items.sumOf { sumSubtree(it).abs() }

        val raw = items.mapNotNull { node ->
            val amount = sumSubtree(node)
            if (amount.signum() == 0) return@mapNotNull null
            val percentage = if (globalTotalAbs.signum() != 0) {
                amount.abs().divide(globalTotalAbs, 4, RoundingMode.HALF_UP).toFloat()
            } else 0f
            WidgetCategorySlice(
                category = node.category,
                categoryId = node.categoryId,
                amount = amount.abs().toDouble(),
                amountFormatted = formatAmount(amount),
                colorHex = colorIntToHex(node.colorHex),
                percentage = percentage
            )
        }.sortedByDescending { it.amount }

        return raw
    }

    private fun colorIntToHex(colorInt: Int): String =
        String.format("#%06X", 0xFFFFFF and colorInt)

    fun encode(slices: List<WidgetCategorySlice>): String =
        Json.encodeToString(slices)

    fun decode(json: String?): List<WidgetCategorySlice> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching { Json.decodeFromString<List<WidgetCategorySlice>>(json) }.getOrDefault(emptyList())
    }
}
