package com.osscabinet.ossWallet.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.widget.RemoteViews
import androidx.datastore.preferences.core.Preferences
import com.osscabinet.ossWallet.MainActivity
import com.osscabinet.ossWallet.R
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.ui.components.getAccountIconByName

/**
 * Builds and pushes the Accounts widget's [RemoteViews]: the shared header shell (total as a chip)
 * and a scrolling ListView of accounts in the order set on the Accounts screen, supplied by
 * [AccountsRemoteViewsService]. Each row is the account's own icon badge, name and period net, and
 * the balance, and opens that account.
 */
object AccountsRemoteViewsRenderer {

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
        val total = prefs[AccountsWidgetKeys.TOTAL_FORMATTED].orEmpty()

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
        val hasAccounts = (prefs[AccountsWidgetKeys.COUNT]?.toIntOrNull() ?: 0) > 0
        if (hasAccounts && total.isNotEmpty()) {
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
        // The title opens the whole Accounts screen, like "VIEW ALL" on Recent Activities.
        views.setOnClickPendingIntent(
            R.id.tv_widget_header_title,
            PendingIntent.getActivity(
                context, widgetId * 10 + 1,
                Intent(context, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_WIDGET_NAV, "accounts")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        // Each row's fill-in Intent (AccountsRemoteViewsFactory) adds the account id to this template.
        views.setPendingIntentTemplate(
            R.id.lv_accounts_list,
            PendingIntent.getActivity(
                context, widgetId * 10 + 2,
                Intent(context, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_WIDGET_NAV, "account_detail")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        )

        val adapterIntent = Intent(context, AccountsRemoteViewsService::class.java)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            .apply { data = Uri.parse("remoteviews://accounts/$widgetId") }
        views.setRemoteAdapter(R.id.lv_accounts_list, adapterIntent)
        views.setEmptyView(R.id.lv_accounts_list, R.id.tv_accounts_empty_text)

        appWidgetManager.updateAppWidget(widgetId, views)
        // setRemoteAdapter alone does not re-run the factory's onDataSetChanged().
        appWidgetManager.notifyAppWidgetViewDataChanged(widgetId, R.id.lv_accounts_list)
    }

    fun buildRow(context: Context, row: AccountWidgetRow, palette: WidgetPalette?): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_accounts_row)
        val density = context.resources.displayMetrics.density
        val accountColor = runCatching { Color.parseColor(row.colorHex) }.getOrDefault(Color.GRAY)

        // The account's own icon in a soft badge of its colour, as on the Accounts card.
        views.setInt(R.id.iv_account_badge_bg, "setColorFilter", (accountColor and 0x00FFFFFF) or 0x28000000)
        views.setImageViewBitmap(
            R.id.iv_account_icon,
            WidgetIconRenderer.render(getAccountIconByName(row.iconName), (BADGE_ICON_DP * density).toInt(), accountColor)
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
