package com.osscabinet.ossWallet.widget

import android.content.Context
import android.graphics.Color
import android.widget.RemoteViews
import com.osscabinet.ossWallet.R
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.ui.components.cards.toIcon

/** Binds the parts of widget_header_row.xml that differ per widget. */
object WidgetHeader {
    private const val ICON_DP = 16

    /**
     * Shows [card]'s own icon (the same [toIcon] the dashboard uses). The bitmap is painted white
     * and the ImageView's XML tint colours it, so it follows the system theme like the title does;
     * a forced theme overrides that with [WidgetPalette.onSurfaceVariant].
     */
    fun bindIcon(context: Context, views: RemoteViews, card: CardType, palette: WidgetPalette?) {
        val sizePx = (ICON_DP * context.resources.displayMetrics.density).toInt()
        views.setImageViewBitmap(R.id.iv_widget_header_icon, WidgetIconRenderer.render(card.toIcon(), sizePx, Color.WHITE))
        palette?.let { views.setInt(R.id.iv_widget_header_icon, "setColorFilter", it.onSurfaceVariant) }
    }
}
