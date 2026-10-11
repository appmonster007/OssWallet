package com.osscabinet.ossWallet.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import androidx.datastore.preferences.core.Preferences
import com.osscabinet.ossWallet.MainActivity
import com.osscabinet.ossWallet.R
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.ui.components.getIconByName
import kotlinx.serialization.json.Json

/**
 * Builds and pushes the Accounts widget's [RemoteViews]: the shared header shell (total as a
 * chip) plus one [R.layout.widget_accounts_row] per account, in the order set on the Accounts
 * screen. Each row is the account's own icon badge, name and period net, and the balance.
 *
 * The number of rows follows the widget's live height, up to [AccountsWidgetConfig.MAX_ROWS]: on
 * API 31+ one RemoteViews per row count is handed to the host, which picks the one that fits;
 * older devices use the reported minimum height.
 */
object AccountsRemoteViewsRenderer {

    /** Height the header shell takes, and what one row takes, in dp. */
    private const val HEADER_HEIGHT_DP = 32
    private const val ROW_HEIGHT_DP = 38
    private const val BADGE_ICON_DP = 18

    fun render(
        context: Context,
        prefs: Preferences,
        widgetId: Int,
        appWidgetManager: AppWidgetManager
    ) {
        if (prefs[AccountsWidgetKeys.IS_LOADED] != "true") {
            appWidgetManager.updateAppWidget(widgetId, RemoteViews(context.packageName, R.layout.widget_accounts_loading))
            return
        }

        val theme = prefs[AccountsWidgetKeys.THEME]?.let { runCatching { WidgetTheme.valueOf(it) }.getOrNull() }
            ?: WidgetTheme.SYSTEM
        val palette = WidgetColors.resolve(theme)
        val rows = prefs[AccountsWidgetKeys.ROWS_JSON]
            ?.let { runCatching { Json.decodeFromString<List<AccountWidgetRow>>(it) }.getOrNull() }
            .orEmpty()
        val total = prefs[AccountsWidgetKeys.TOTAL_FORMATTED].orEmpty()

        val views = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // The host picks the largest entry that fits the widget's live size on every layout
            // pass. OPTION_APPWIDGET_MIN_HEIGHT under-reports on some launchers, so it can't be
            // used to count rows.
            RemoteViews(
                (1..AccountsWidgetConfig.MAX_ROWS).associate { n ->
                    SizeF(0f, minHeightForRows(n)) to buildViews(context, widgetId, rows, n, total, palette)
                }
            )
        } else {
            val minHeightDp = appWidgetManager.getAppWidgetOptions(widgetId)
                .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
            val n = (1..AccountsWidgetConfig.MAX_ROWS).lastOrNull { minHeightDp >= minHeightForRows(it) } ?: 1
            buildViews(context, widgetId, rows, n, total, palette)
        }
        appWidgetManager.updateAppWidget(widgetId, views)
    }

    /** Smallest widget height, in dp, at which [rows] rows fit; one row is the floor. */
    private fun minHeightForRows(rows: Int): Float =
        if (rows <= 1) 0f else (HEADER_HEIGHT_DP + ROW_HEIGHT_DP * rows).toFloat()

    private fun buildViews(
        context: Context,
        widgetId: Int,
        rows: List<AccountWidgetRow>,
        visibleRows: Int,
        total: String,
        palette: WidgetPalette?
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_accounts_main)
        views.setTextViewText(R.id.tv_widget_header_title, "ACCOUNTS")
        WidgetHeader.bindIcon(context, views, CardType.ACCOUNTS, palette)
        if (palette != null) {
            views.setInt(R.id.ll_widget_root, "setBackgroundColor", palette.surface)
            views.setTextColor(R.id.tv_widget_header_title, palette.onSurfaceVariant)
            views.setInt(R.id.iv_widget_header_settings, "setColorFilter", palette.onSurfaceVariant)
            views.setInt(R.id.v_widget_header_divider, "setBackgroundColor", palette.outline)
            views.setTextColor(R.id.tv_accounts_empty_text, palette.onSurfaceVariant)
        }

        WidgetHeader.clearExtras(views)
        if (rows.isNotEmpty() && total.isNotEmpty()) {
            WidgetHeader.addChip(context, views, "≈ $total", WidgetHeader.ChipStyle.NEUTRAL, palette)
        }

        views.setOnClickPendingIntent(
            R.id.iv_widget_header_settings,
            PendingIntent.getActivity(
                context, widgetId * 10,
                Intent(context, AccountsWidgetConfigActivity::class.java)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        views.setOnClickPendingIntent(
            R.id.ll_accounts_body,
            PendingIntent.getActivity(
                context, widgetId * 10 + 1,
                Intent(context, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_WIDGET_NAV, "accounts")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        views.removeAllViews(R.id.ll_accounts_rows)
        if (rows.isEmpty()) {
            views.setViewVisibility(R.id.ll_accounts_rows, View.GONE)
            views.setViewVisibility(R.id.tv_accounts_empty_text, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.ll_accounts_rows, View.VISIBLE)
            views.setViewVisibility(R.id.tv_accounts_empty_text, View.GONE)
            rows.take(visibleRows).forEach { row ->
                views.addView(R.id.ll_accounts_rows, buildRow(context, row, palette))
            }
        }
        return views
    }

    private fun buildRow(context: Context, row: AccountWidgetRow, palette: WidgetPalette?): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_accounts_row)
        val density = context.resources.displayMetrics.density
        val accountColor = runCatching { Color.parseColor(row.colorHex) }.getOrDefault(Color.GRAY)

        // The account's own icon in a soft badge of its colour, as on the Accounts card.
        views.setInt(R.id.iv_account_badge_bg, "setColorFilter", (accountColor and 0x00FFFFFF) or 0x28000000)
        views.setImageViewBitmap(
            R.id.iv_account_icon,
            WidgetIconRenderer.render(getIconByName(row.iconName), (BADGE_ICON_DP * density).toInt(), accountColor)
        )

        views.setTextViewText(R.id.tv_account_name, row.name)
        views.setTextViewText(R.id.tv_account_balance, row.balanceFormatted)
        views.setTextViewText(R.id.tv_account_net, row.netCaption)

        val negative = palette?.negative ?: context.getColor(R.color.widget_negative)
        val positive = palette?.positive ?: context.getColor(R.color.widget_positive)
        val muted = palette?.onSurfaceVariant ?: context.getColor(R.color.widget_on_surface_variant)
        views.setTextColor(R.id.tv_account_balance, if (row.balanceNegative) negative else palette?.onSurface ?: context.getColor(R.color.widget_on_surface))
        views.setTextColor(
            R.id.tv_account_net,
            when {
                row.netNegative -> negative
                row.netCaption.startsWith("+") -> positive
                else -> muted
            }
        )
        palette?.let { views.setTextColor(R.id.tv_account_name, it.onSurface) }

        return views
    }
}
