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
    private const val BADGE_ALPHA = 0x28

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

        val views = RemoteViews(context.packageName, WidgetColors.layoutFor(palette, R.layout.widget_accounts_main_t0, R.layout.widget_accounts_main_t1, R.layout.widget_accounts_main_t2))
        views.setTextViewText(R.id.tv_widget_header_title, "ACCOUNTS")
        WidgetHeader.bindIcon(context, views, CardType.ACCOUNTS, palette)
        WidgetHeader.applyTheme(views, palette, prefs[AccountsWidgetKeys.GLASS] == "true")
        views.setThemedTextColor(R.id.tv_accounts_empty_text, palette) { it.onSurfaceVariant }

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
            .apply { data = Uri.parse("remoteviews://accounts/$widgetId?theme=${WidgetColors.themeKey(theme)}") }
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
        // Opaque colour at low alpha, not a pre-faded colour over the white oval: that left the
        // badge pale on a dark surface instead of tinting whatever is behind it.
        views.setInt(R.id.iv_account_badge_bg, "setColorFilter", accountColor or 0xFF000000.toInt())
        views.setInt(R.id.iv_account_badge_bg, "setImageAlpha", BADGE_ALPHA)
        views.setImageViewBitmap(
            R.id.iv_account_icon,
            WidgetIconRenderer.render(getAccountIconByName(row.iconName), (BADGE_ICON_DP * density).toInt(), accountColor)
        )

        views.setTextViewText(R.id.tv_account_name, row.name)
        views.setTextViewText(R.id.tv_account_balance, row.balanceFormatted)
        views.setTextViewText(R.id.tv_account_net, row.netCaption)

        views.setThemedTextColor(R.id.tv_account_balance, palette) { if (row.balanceNegative) it.negative else it.onSurface }
        views.setThemedTextColor(R.id.tv_account_net, palette) {
            when {
                row.netNegative -> it.negative
                row.netCaption.startsWith("+") -> it.positive
                else -> it.onSurfaceVariant
            }
        }
        views.setThemedTextColor(R.id.tv_account_name, palette) { it.onSurface }

        return views
    }
}
