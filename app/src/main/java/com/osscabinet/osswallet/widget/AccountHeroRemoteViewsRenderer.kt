package com.osscabinet.ossWallet.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import androidx.datastore.preferences.core.Preferences
import com.osscabinet.ossWallet.R
import kotlinx.serialization.json.Json

private fun parsePoints(json: String?): List<Double> {
    if (json.isNullOrBlank()) return emptyList()
    return runCatching { Json.decodeFromString<List<Double>>(json) }.getOrDefault(emptyList())
}

/**
 * Account Hero widget — 4x2 tier, account-pinned (one widget instance = one accountId). Colored
 * dot + account name + period, big balance + comparison pill, area/line sparkline bitmap below
 * (WidgetLineAreaChartRenderer.renderAreaSparkline). Tap-inert (no deep-link infra, per Phase 1/2
 * precedent). Shows a reconfigure message if the pinned account was deleted.
 */
object AccountHeroRemoteViewsRenderer {

    private const val SPARKLINE_WIDTH_DP = 140
    private const val SPARKLINE_HEIGHT_DP = 36

    fun render(context: Context, prefs: Preferences, widgetId: Int, appWidgetManager: AppWidgetManager) {
        val isLoaded = prefs[AccountHeroWidgetKeys.IS_LOADED] == "true"
        if (!isLoaded) {
            appWidgetManager.updateAppWidget(widgetId, RemoteViews(context.packageName, R.layout.widget_account_hero_loading))
            return
        }
        val found = prefs[AccountHeroWidgetKeys.ACCOUNT_FOUND] == "true"
        val views = if (!found) renderNotFound(context) else renderHero(context, prefs)
        appWidgetManager.updateAppWidget(widgetId, views)
    }

    private fun renderNotFound(context: Context): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_account_hero_not_found)

    private fun renderHero(context: Context, prefs: Preferences): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_account_hero_chart)
        val colorHex = prefs[AccountHeroWidgetKeys.COLOR_HEX]
        val color = WidgetSparklineRenderer.parseColorOrDefault(colorHex)
        views.setInt(R.id.v_ah_dot, "setColorFilter", color)
        views.setTextViewText(R.id.tv_ah_name, prefs[AccountHeroWidgetKeys.ACCOUNT_NAME] ?: "")
        views.setTextViewText(R.id.tv_ah_period, prefs[AccountHeroWidgetKeys.PERIOD_LABEL] ?: "")
        views.setTextViewText(R.id.tv_ah_balance, prefs[AccountHeroWidgetKeys.BALANCE_FORMATTED] ?: "")

        val comparison = prefs[AccountHeroWidgetKeys.COMPARISON]?.takeIf { it.isNotBlank() }
        if (comparison != null) {
            views.setViewVisibility(R.id.tv_ah_comparison, View.VISIBLE)
            val arrow = if (comparison.startsWith("+")) "▲" else "▼"
            views.setTextViewText(R.id.tv_ah_comparison, "$arrow $comparison")
        } else {
            views.setViewVisibility(R.id.tv_ah_comparison, View.GONE)
        }

        val points = parsePoints(prefs[AccountHeroWidgetKeys.SPARKLINE_JSON])
        if (points.size >= 2) {
            val density = context.resources.displayMetrics.density
            val widthPx = (SPARKLINE_WIDTH_DP * density).toInt()
            val heightPx = (SPARKLINE_HEIGHT_DP * density).toInt()
            val bitmap = WidgetLineAreaChartRenderer.renderAreaSparkline(widthPx, heightPx, points, color, 2.5f * density)
            views.setImageViewBitmap(R.id.iv_ah_sparkline, bitmap)
            views.setViewVisibility(R.id.iv_ah_sparkline, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.iv_ah_sparkline, View.GONE)
        }
        return views
    }
}
