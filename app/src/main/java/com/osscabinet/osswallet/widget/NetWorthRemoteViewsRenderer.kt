package com.osscabinet.ossWallet.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.SizeF
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.osscabinet.ossWallet.MainActivity
import com.osscabinet.ossWallet.R

/**
 * Preference keys written by [NetWorthWidgetFetcher]. Plays the same role
 * [NetWorthGlanceWidget.Keys] used to when this widget was Glance-based.
 *
 * Unified 3-slot cell model, used by strip (all 3 packed into one row) and card (cell1/cell2
 * on one row, cell3 on another): cell1/cell2 are Inflow/Outflow (CashFlow) or Income/Expenses
 * (Accounting); cell3 is Net flow (CashFlow, highlighted) or Transfer (Accounting, plain). See
 * NetWorthWidgetFetcher for the exact visibility/value rules per perspective.
 */
object NetWorthWidgetKeys {
    val IS_LOADED = stringPreferencesKey("nw_is_loaded")
    val NET_WORTH = stringPreferencesKey("nw_net_worth")
    val NET_WORTH_COMPARISON = stringPreferencesKey("nw_comparison")
    val PERIOD_LABEL = stringPreferencesKey("nw_period_label")
    val FONT_SIZE = stringPreferencesKey("nw_font_size")
    val BASE_CURRENCY = stringPreferencesKey("nw_base_currency")
    val PERSPECTIVE_LABEL = stringPreferencesKey("nw_perspective_label")
    /** Name of a [WidgetTheme] constant — forces LIGHT/DARK regardless of the device's current night mode, or SYSTEM to leave every color/drawable as XML already declares it. */
    val THEME = stringPreferencesKey("nw_theme")

    val CELL1_LABEL = stringPreferencesKey("nw_cell1_label")
    val CELL1_VALUE = stringPreferencesKey("nw_cell1_value")
    val CELL1_CMP = stringPreferencesKey("nw_cell1_cmp")
    val CELL1_VISIBLE = stringPreferencesKey("nw_cell1_visible")

    val CELL2_LABEL = stringPreferencesKey("nw_cell2_label")
    val CELL2_VALUE = stringPreferencesKey("nw_cell2_value")
    val CELL2_CMP = stringPreferencesKey("nw_cell2_cmp")
    val CELL2_VISIBLE = stringPreferencesKey("nw_cell2_visible")

    val CELL3_LABEL = stringPreferencesKey("nw_cell3_label")
    val CELL3_VALUE = stringPreferencesKey("nw_cell3_value")
    val CELL3_CMP = stringPreferencesKey("nw_cell3_cmp")
    val CELL3_VISIBLE = stringPreferencesKey("nw_cell3_visible")
    val CELL3_HIGHLIGHT = stringPreferencesKey("nw_cell3_highlight")

    // CELL4 only ever appears in Accounting perspective with splitTransfers on — Transfer Out,
    // paired with CELL3 holding Transfer In instead of CashFlow/combined-Accounting's single
    // Transfer. Hidden (CELL4_VISIBLE="false") in every other case; GONE cells don't participate
    // in their row's layout_weight distribution, so CELL3 (or CELL1-3 on strip) simply reclaims
    // the space — no separate "3-cell vs 4-cell row" layout variant is needed.
    val CELL4_LABEL = stringPreferencesKey("nw_cell4_label")
    val CELL4_VALUE = stringPreferencesKey("nw_cell4_value")
    val CELL4_CMP = stringPreferencesKey("nw_cell4_cmp")
    val CELL4_VISIBLE = stringPreferencesKey("nw_cell4_visible")
    val CELL4_FILTER = stringPreferencesKey("nw_cell4_filter")

    // URL-encoded JSON TransactionFilter per tap target, matching NetWorthCard.kt's own
    // click-to-drill-down filters exactly (see NetWorthWidgetFetcher) — ROW1 is the net
    // worth/net flow amount (unfiltered by type, same as the card's onClickNet/onClickNetFlow).
    val ROW1_FILTER = stringPreferencesKey("nw_row1_filter")
    val CELL1_FILTER = stringPreferencesKey("nw_cell1_filter")
    val CELL2_FILTER = stringPreferencesKey("nw_cell2_filter")
    val CELL3_FILTER = stringPreferencesKey("nw_cell3_filter")
}

/**
 * Builds and pushes the NetWorth widget's [RemoteViews] directly.
 *
 * Exactly THREE tiers — no separate "large" tier beyond card; card is the ceiling, capped by
 * maxResizeHeight="180dp" in both widget-info XMLs (there's nothing taller to grow into, and
 * an earlier unbounded maxResizeHeight stretched card's 3 equal-weight rows into oddly-spaced
 * "4-5 row"-looking shapes). Every tier starts with the exact same "row 1"
 * (widget_net_worth_row1.xml — header + amount), via <include>:
 *  - micro (<110dp): row 1 only.
 *  - strip (110-179dp): row 1 + one row packing all 3 cells side by side (not enough height to
 *    give cell3 its own row yet).
 *  - card (>=180dp): row 1 + cell1/cell2 row + cell3 row, three equal-weight rows.
 * MICRO_HEIGHT_DP/STRIP_HEIGHT_DP are net_worth_widget_info.xml's and
 * net_worth_widget_info_card.xml's own declared minHeight values (110dp/180dp) — not
 * independently guessed midpoints. They used to be the formula's raw midpoints (75dp/145dp),
 * but a real device rendered a provider placed at its declared 110dp minHeight tall enough to
 * trigger the (>=145dp) card threshold — launchers round a declared dp size up to their own
 * grid cells, so the on-screen result can run taller than what was declared. Reusing each
 * provider's own minHeight as its activation threshold keeps "what we told Android this tier
 * needs" and "what we check for at render time" the same number, so that mismatch can't recur.
 *
 * On API 31+ (Android 12, "S"), tier selection is handed to the platform entirely via the
 * [RemoteViews] exact-size-map constructor instead of polling
 * [AppWidgetManager.getAppWidgetOptions] ourselves: some launchers don't keep
 * OPTION_APPWIDGET_MIN_HEIGHT in sync with the widget's actual live size after an interactive
 * resize (it was observed stuck at an old, smaller value — permanently, not just transiently —
 * on a real device, which is what caused the strip tier to render even when the widget had
 * clearly been resized tall enough for card). The size map sidesteps that: the system itself
 * measures the widget's current bounds every time it lays the RemoteViews out, not just once at
 * our render() call, so it can't go stale the way our own one-shot options read could. Below
 * API 31, there's no such API, so the thresholds above are the only option — kept as a fallback
 * for that case, with the same known caveat on launchers that mis-report min height.
 */
object NetWorthRemoteViewsRenderer {

    private const val MICRO_HEIGHT_DP = 110
    private const val STRIP_HEIGHT_DP = 180

    fun render(
        context: Context,
        prefs: Preferences,
        widgetId: Int,
        appWidgetManager: AppWidgetManager
    ) {
        val isLoaded = prefs[NetWorthWidgetKeys.IS_LOADED] == "true"
        if (!isLoaded) {
            val loading = RemoteViews(context.packageName, R.layout.widget_net_worth)
            appWidgetManager.updateAppWidget(widgetId, loading)
            return
        }

        val theme = prefs[NetWorthWidgetKeys.THEME]?.let { runCatching { WidgetTheme.valueOf(it) }.getOrNull() }
            ?: WidgetTheme.SYSTEM
        val palette = WidgetColors.resolve(theme)

        val views = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            buildResponsiveViews(context, prefs, widgetId, palette)
        } else {
            val options = appWidgetManager.getAppWidgetOptions(widgetId)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
            when {
                minHeight in 1 until MICRO_HEIGHT_DP -> renderMicro(context, prefs, widgetId, palette)
                minHeight in MICRO_HEIGHT_DP until STRIP_HEIGHT_DP ->
                    renderCellTier(context, prefs, widgetId, R.layout.widget_net_worth_strip, palette)
                else ->
                    renderCellTier(context, prefs, widgetId, R.layout.widget_net_worth_card, palette)
            }
        }
        appWidgetManager.updateAppWidget(widgetId, views)
    }

    /**
     * Each entry's [SizeF] is the minimum (width, height) in dp that tier needs; the system
     * picks the largest entry that still fits the widget's actual current size, live, on every
     * layout pass — not just once per [render] call. Width is left at the floor (0dp) on every
     * entry since only height distinguishes these tiers; each provider's own minWidth already
     * guarantees enough width for its cells regardless of which tier gets picked here.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun buildResponsiveViews(context: Context, prefs: Preferences, widgetId: Int, palette: WidgetPalette?): RemoteViews {
        val micro = renderMicro(context, prefs, widgetId, palette)
        val strip = renderCellTier(context, prefs, widgetId, R.layout.widget_net_worth_strip, palette)
        val card = renderCellTier(context, prefs, widgetId, R.layout.widget_net_worth_card, palette)
        return RemoteViews(
            mapOf(
                SizeF(0f, 0f) to micro,
                SizeF(0f, MICRO_HEIGHT_DP.toFloat()) to strip,
                SizeF(0f, STRIP_HEIGHT_DP.toFloat()) to card
            )
        )
    }

    private fun fontSizeSp(fontSize: String?): Float = when (fontSize) {
        "Small" -> 18f
        "Large" -> 24f
        "X-Large" -> 32f
        else -> 20f
    }

    /**
     * [NetWorthWidgetKeys.NET_WORTH_COMPARISON] (and every per-cell *_CMP key) is a bare
     * "+4.2%"/"-4.2%" (see buildComparisonText) — mirrors NetWorthCard's own
     * "${if (up) "▲" else "▼"} ..." by prepending the arrow and resolving the matching
     * semantic color here. [palette] overrides the positive/negative pair when a theme is
     * forced; null (SYSTEM) falls back to the XML-resolved @color values.
     */
    private fun formatTrend(context: Context, changeText: String, palette: WidgetPalette?): Pair<String, Int> {
        val isUp = changeText.startsWith("+")
        val arrow = if (isUp) "▲" else "▼"
        val color = if (palette != null) {
            if (isUp) palette.positive else palette.negative
        } else {
            ContextCompat.getColor(context, if (isUp) R.color.widget_positive else R.color.widget_negative)
        }
        return "$arrow $changeText" to color
    }

    /**
     * Tap targets (row 1's amount, each cell) open MainActivity straight to a specific screen
     * pre-filtered, mirroring NetWorthCard.kt's own click-to-drill-down behavior — see
     * NetWorthWidgetFetcher for where [filterJson] (already URL-encoded) is computed per target.
     * [requestCode] must be distinct per tap target per widget instance: PendingIntent treats
     * two intents targeting the same component with the same request code as interchangeable
     * regardless of their extras (under FLAG_UPDATE_CURRENT the newer extras just overwrite the
     * older ones in place), so reusing one requestCode across row1/cell1/cell2/cell3 would make
     * every tap open whichever target was bound last.
     */
    private fun buildAppPendingIntent(
        context: Context,
        requestCode: Int,
        nav: String,
        filterJson: String?
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_WIDGET_NAV, nav)
            .apply { filterJson?.let { putExtra(MainActivity.EXTRA_WIDGET_FILTER, it) } }
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Row 1 — identical on every tier: header label + settings button, amount, comparison
     * pill. [widgetId] targets the settings button's PendingIntent at THIS widget instance's
     * own config screen (NetWorthWidgetConfigActivity reads EXTRA_APPWIDGET_ID to know which
     * instance's prefs to load/save — see BaseWidgetConfigActivity). [palette] non-null forces
     * every color below to that theme instead of the XML's resource-qualified default.
     */
    private fun bindRow1(views: RemoteViews, prefs: Preferences, context: Context, widgetId: Int, palette: WidgetPalette?) {
        val perspectiveLabel = prefs[NetWorthWidgetKeys.PERSPECTIVE_LABEL] ?: "Cash Flow"
        views.setTextViewText(R.id.tv_widget_header_title, "NET WORTH · ${perspectiveLabel.uppercase()}")
        if (palette != null) {
            views.setTextColor(R.id.tv_widget_header_title, palette.onSurfaceVariant)
            views.setInt(R.id.iv_widget_header_settings, "setColorFilter", palette.onSurfaceVariant)
            views.setInt(R.id.v_widget_header_divider, "setBackgroundColor", palette.outline)
        }

        // NEW_TASK + MULTIPLE_TASK: launched from the widget host (home screen), not from
        // inside our app, so without an explicit new task this activity can attach itself to
        // the app's existing background task if one is running — finish() after save would
        // then pop back to MainActivity underneath it instead of returning to the home screen.
        val settingsIntent = Intent(context, NetWorthWidgetConfigActivity::class.java)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        val settingsPendingIntent = PendingIntent.getActivity(
            context,
            widgetId * 10,
            settingsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.iv_widget_header_settings, settingsPendingIntent)

        // Tapping the amount opens Analytics pre-filtered — same target as NetWorthCard's own
        // onClickNet/onClickNetFlow (an unfiltered-by-type drill into this widget's base filter).
        views.setOnClickPendingIntent(
            R.id.tv_nw_row1_amount,
            buildAppPendingIntent(context, widgetId * 10 + 1, "analytics", prefs[NetWorthWidgetKeys.ROW1_FILTER])
        )

        views.setTextViewText(R.id.tv_nw_row1_amount, prefs[NetWorthWidgetKeys.NET_WORTH] ?: "")
        views.setTextViewTextSize(
            R.id.tv_nw_row1_amount,
            TypedValue.COMPLEX_UNIT_SP,
            fontSizeSp(prefs[NetWorthWidgetKeys.FONT_SIZE])
        )
        if (palette != null) {
            views.setTextColor(R.id.tv_nw_row1_amount, palette.onSurface)
        }

        val changeText = prefs[NetWorthWidgetKeys.NET_WORTH_COMPARISON]?.takeIf { it.isNotEmpty() }
        views.setViewVisibility(R.id.tv_nw_row1_change, if (changeText != null) View.VISIBLE else View.GONE)
        if (changeText != null) {
            val (text, color) = formatTrend(context, changeText, palette)
            views.setTextViewText(R.id.tv_nw_row1_change, text)
            views.setTextColor(R.id.tv_nw_row1_change, color)
        }
        if (palette != null) {
            views.setInt(R.id.tv_nw_row1_change, "setBackgroundResource", palette.pillBg)
        }
    }

    private fun renderMicro(context: Context, prefs: Preferences, widgetId: Int, palette: WidgetPalette?): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_net_worth_micro)
        bindRow1(views, prefs, context, widgetId, palette)
        if (palette != null) {
            views.setInt(R.id.ll_widget_root, "setBackgroundColor", palette.surface)
        }
        return views
    }

    /**
     * Strip and card share the same row 1 + the same 3 cell ids (ll_nw_cell1/2/3,
     * tv_nw_cell{1,2,3}_label/value/cmp) — only the layout resource differs; both now show the
     * comparison-% line on every cell.
     */
    private fun renderCellTier(
        context: Context,
        prefs: Preferences,
        widgetId: Int,
        layoutResId: Int,
        palette: WidgetPalette?
    ): RemoteViews {
        val views = RemoteViews(context.packageName, layoutResId)
        bindRow1(views, prefs, context, widgetId, palette)
        if (palette != null) {
            views.setInt(R.id.ll_widget_root, "setBackgroundColor", palette.surface)
        }

        bindCell(
            views, prefs, context,
            labelKey = NetWorthWidgetKeys.CELL1_LABEL, valueKey = NetWorthWidgetKeys.CELL1_VALUE,
            cmpKey = NetWorthWidgetKeys.CELL1_CMP, visibleKey = NetWorthWidgetKeys.CELL1_VISIBLE,
            filterKey = NetWorthWidgetKeys.CELL1_FILTER,
            labelId = R.id.tv_nw_cell1_label, valueId = R.id.tv_nw_cell1_value,
            cmpId = R.id.tv_nw_cell1_cmp,
            containerId = R.id.ll_nw_cell1,
            requestCode = widgetId * 10 + 2,
            palette = palette
        )
        bindCell(
            views, prefs, context,
            labelKey = NetWorthWidgetKeys.CELL2_LABEL, valueKey = NetWorthWidgetKeys.CELL2_VALUE,
            cmpKey = NetWorthWidgetKeys.CELL2_CMP, visibleKey = NetWorthWidgetKeys.CELL2_VISIBLE,
            filterKey = NetWorthWidgetKeys.CELL2_FILTER,
            labelId = R.id.tv_nw_cell2_label, valueId = R.id.tv_nw_cell2_value,
            cmpId = R.id.tv_nw_cell2_cmp,
            containerId = R.id.ll_nw_cell2,
            requestCode = widgetId * 10 + 3,
            palette = palette
        )
        bindCell(
            views, prefs, context,
            labelKey = NetWorthWidgetKeys.CELL3_LABEL, valueKey = NetWorthWidgetKeys.CELL3_VALUE,
            cmpKey = NetWorthWidgetKeys.CELL3_CMP, visibleKey = NetWorthWidgetKeys.CELL3_VISIBLE,
            filterKey = NetWorthWidgetKeys.CELL3_FILTER,
            labelId = R.id.tv_nw_cell3_label, valueId = R.id.tv_nw_cell3_value,
            cmpId = R.id.tv_nw_cell3_cmp,
            containerId = R.id.ll_nw_cell3,
            requestCode = widgetId * 10 + 4,
            palette = palette
        )
        // Cell4 only ever appears for Accounting + split transfers (Transfer Out) — always the
        // plain metric-tile style, never the highlighted netflow chip cell3 can take.
        bindCell(
            views, prefs, context,
            labelKey = NetWorthWidgetKeys.CELL4_LABEL, valueKey = NetWorthWidgetKeys.CELL4_VALUE,
            cmpKey = NetWorthWidgetKeys.CELL4_CMP, visibleKey = NetWorthWidgetKeys.CELL4_VISIBLE,
            filterKey = NetWorthWidgetKeys.CELL4_FILTER,
            labelId = R.id.tv_nw_cell4_label, valueId = R.id.tv_nw_cell4_value,
            cmpId = R.id.tv_nw_cell4_cmp,
            containerId = R.id.ll_nw_cell4,
            requestCode = widgetId * 10 + 5,
            palette = palette
        )

        // Cell3 swaps between the highlighted netflow-chip style (CashFlow's Net flow) and the
        // plain metric-tile style + default text colors (Accounting's Transfer) — on both
        // tiers that have a cell3.
        val highlighted = prefs[NetWorthWidgetKeys.CELL3_HIGHLIGHT] != "false"
        val cell3Bg = if (palette != null) {
            if (highlighted) palette.pillBg else palette.tileBg
        } else {
            if (highlighted) R.drawable.widget_netflow_pill_bg else R.drawable.widget_metric_tile_bg
        }
        views.setInt(R.id.ll_nw_cell3, "setBackgroundResource", cell3Bg)
        val cell3LabelColor = if (palette != null) {
            if (highlighted) palette.accent else palette.onSurfaceVariant
        } else {
            ContextCompat.getColor(context, if (highlighted) R.color.widget_accent else R.color.widget_on_surface_variant)
        }
        val cell3ValueColor = if (palette != null) {
            if (highlighted) palette.accent else palette.onSurface
        } else {
            ContextCompat.getColor(context, if (highlighted) R.color.widget_accent else R.color.widget_on_surface)
        }
        views.setTextColor(R.id.tv_nw_cell3_label, cell3LabelColor)
        views.setTextColor(R.id.tv_nw_cell3_value, cell3ValueColor)

        return views
    }

    /**
     * Binds one of the 3 shared cell slots (see [NetWorthWidgetKeys]'s header comment). Tapping
     * the cell opens Records pre-filtered to that metric's drill-down filter (see
     * NetWorthWidgetFetcher for how [filterKey]'s value is computed) — same target as
     * NetWorthCard's own onClickIncome/Expense/Transfer/Inflow/Outflow handlers. [palette]
     * non-null forces the cell's tile background + label/value colors to that theme.
     */
    private fun bindCell(
        views: RemoteViews,
        prefs: Preferences,
        context: Context,
        labelKey: Preferences.Key<String>,
        valueKey: Preferences.Key<String>,
        cmpKey: Preferences.Key<String>,
        visibleKey: Preferences.Key<String>,
        filterKey: Preferences.Key<String>,
        labelId: Int,
        valueId: Int,
        cmpId: Int,
        containerId: Int,
        requestCode: Int,
        palette: WidgetPalette?
    ) {
        views.setTextViewText(labelId, prefs[labelKey] ?: "")
        views.setTextViewText(valueId, prefs[valueKey] ?: "")
        if (palette != null) {
            views.setTextColor(labelId, palette.onSurfaceVariant)
            views.setTextColor(valueId, palette.onSurface)
            views.setInt(containerId, "setBackgroundResource", palette.tileBg)
        }

        val cmp = prefs[cmpKey]?.takeIf { it.isNotEmpty() }
        views.setViewVisibility(cmpId, if (cmp != null) View.VISIBLE else View.GONE)
        if (cmp != null) {
            val (text, color) = formatTrend(context, cmp, palette)
            views.setTextViewText(cmpId, text)
            views.setTextColor(cmpId, color)
        }

        views.setViewVisibility(containerId, if (prefs[visibleKey] != "false") View.VISIBLE else View.GONE)
        views.setOnClickPendingIntent(
            containerId,
            buildAppPendingIntent(context, requestCode, "records", prefs[filterKey])
        )
    }
}
