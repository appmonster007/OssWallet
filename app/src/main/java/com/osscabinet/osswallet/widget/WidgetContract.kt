package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import com.osscabinet.ossWallet.widget.WidgetDataLoader.CommonData

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Interface for fetching and formatting data for a specific widget type.
 */
interface WidgetDataFetcher {
    /**
     * Computes the data for a specific widget instance and writes it to the preferences.
     */
    suspend fun updateState(
        context: Context,
        widgetId: Int,
        common: CommonData,
        prefs: MutablePreferences
    )
}

/**
 * Utility for building comparison text (+/- %)
 */
fun buildComparisonText(
    current: BigDecimal,
    previous: BigDecimal?,
    showComparison: Boolean
): String? {
    if (!showComparison || previous == null || previous.signum() == 0) return null
    val change = current.subtract(previous)
    if (change.signum() == 0) return null
    val pct = change.multiply(BigDecimal("100"))
        .divide(previous.abs(), 1, RoundingMode.HALF_UP)
    val sign = if (change.signum() > 0) "+" else "-"
    return "$sign${pct.abs()}%"
}

/**
 * Registry mapping widget classes to their respective data fetchers.
 */
object WidgetRegistry {
    private val fetchers = mapOf<Class<*>, WidgetDataFetcher>(
        NetWorthGlanceWidget::class.java to NetWorthWidgetFetcher(),
        CashFlowGlanceWidget::class.java to CashFlowWidgetFetcher(),
        RecentTransactionsGlanceWidget::class.java to RecentTransactionsWidgetFetcher()
    )

    fun getFetcher(widgetClass: Class<*>): WidgetDataFetcher? = fetchers[widgetClass]
    
    fun getAllRegisteredWidgets(): Set<Class<*>> = fetchers.keys
}
