package com.osscabinet.ossWallet.widget

import android.content.Context
import android.graphics.Color
import android.widget.RemoteViews
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.osscabinet.ossWallet.R
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.ui.components.cards.toIcon

/** Binds the parts of widget_header_row.xml that differ per widget. */
object WidgetHeader {
    private const val ICON_DP = 16

    enum class ChipStyle(@DrawableRes val background: Int, @ColorRes val textColor: Int) {
        NEGATIVE(R.drawable.widget_chip_negative, R.color.widget_negative),
        WARNING(R.drawable.widget_chip_warning, R.color.widget_warning),
        NEUTRAL(R.drawable.widget_chip_neutral, R.color.widget_on_surface_variant)
    }

    /**
     * Shows [card]'s own icon (the same [toIcon] the dashboard uses). The bitmap is painted white;
     * [applyTheme] colours it.
     */
    fun bindIcon(context: Context, views: RemoteViews, card: CardType, palette: WidgetPalette?) {
        val sizePx = (ICON_DP * context.resources.displayMetrics.density).toInt()
        views.setImageViewBitmap(R.id.iv_widget_header_icon, WidgetIconRenderer.render(card.toIcon(), sizePx, Color.WHITE))
    }

    /** Themes the widget root and the shared header row: always sets every one, see [setThemedTextColor]. */
    fun applyTheme(views: RemoteViews, palette: WidgetPalette?) {
        views.setThemedBackgroundColor(R.id.ll_widget_root, palette) { it.surface }
        views.setThemedTextColor(R.id.tv_widget_header_title, palette) { it.onSurfaceVariant }
        views.setThemedColorFilter(R.id.iv_widget_header_icon, palette) { it.onSurfaceVariant }
        views.setThemedColorFilter(R.id.iv_widget_header_settings, palette) { it.onSurfaceVariant }
        views.setThemedBackgroundColor(R.id.v_widget_header_divider, palette) { it.outline }
    }

    /** Empties the header's extra slot; call before [addChip] so a re-render does not stack chips. */
    fun clearExtras(views: RemoteViews) = views.removeAllViews(R.id.ll_widget_header_extra)

    /** Adds a chip to the header's extra slot. A forced theme recolours the text. */
    fun addChip(context: Context, views: RemoteViews, text: String, style: ChipStyle, palette: WidgetPalette?) {
        val chip = RemoteViews(context.packageName, R.layout.widget_header_chip)
        chip.setTextViewText(R.id.tv_header_chip, text)
        chip.setInt(
            R.id.tv_header_chip, "setBackgroundResource",
            palette?.let {
                when (style) {
                    ChipStyle.NEGATIVE -> it.chipNegativeBg
                    ChipStyle.WARNING -> it.chipWarningBg
                    ChipStyle.NEUTRAL -> it.chipNeutralBg
                }
            } ?: style.background
        )
        chip.setThemedTextColor(R.id.tv_header_chip, palette) {
            when (style) {
                ChipStyle.NEGATIVE -> it.negative
                ChipStyle.WARNING -> it.warning
                ChipStyle.NEUTRAL -> it.onSurfaceVariant
            }
        }
        views.addView(R.id.ll_widget_header_extra, chip)
    }
}
