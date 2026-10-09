package com.osscabinet.ossWallet.widget

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.osscabinet.ossWallet.R

/**
 * Every color/background the two renderers need, forced to one appearance — resolved from
 * [WidgetTheme.LIGHT]/[WidgetTheme.DARK] (see that enum for why [WidgetTheme.SYSTEM] needs no
 * palette: the XML defaults already follow it via values/values-night). Values are hardcoded
 * copies of values/colors.xml and values-night/colors.xml's widget_* tokens, not @color
 * references, since a reference still resolves against the device's actual current
 * configuration regardless of what this specific widget instance was told to use.
 */
data class WidgetPalette(
    @ColorInt val surface: Int,
    @ColorInt val onSurface: Int,
    @ColorInt val onSurfaceVariant: Int,
    @ColorInt val accent: Int,
    @ColorInt val outline: Int,
    @ColorInt val positive: Int,
    @ColorInt val negative: Int,
    @DrawableRes val tileBg: Int,
    @DrawableRes val pillBg: Int,
    @DrawableRes val ovalPositiveBg: Int,
    @DrawableRes val ovalNegativeBg: Int
)

object WidgetColors {
    private val LIGHT = WidgetPalette(
        surface = 0xFFFAF8F1.toInt(),
        onSurface = 0xFF1B1914.toInt(),
        onSurfaceVariant = 0xFF6B6658.toInt(),
        accent = 0xFF8A6F3C.toInt(),
        outline = 0xFFC9C2B2.toInt(),
        positive = 0xFF1E9B4F.toInt(),
        negative = 0xFFC8463F.toInt(),
        tileBg = R.drawable.widget_metric_tile_bg_light,
        pillBg = R.drawable.widget_netflow_pill_bg_light,
        ovalPositiveBg = R.drawable.widget_oval_positive_bg_light,
        ovalNegativeBg = R.drawable.widget_oval_negative_bg_light
    )

    private val DARK = WidgetPalette(
        surface = 0xFF131312.toInt(),
        onSurface = 0xFFECE8DD.toInt(),
        onSurfaceVariant = 0xFF9A958A.toInt(),
        accent = 0xFFD6C189.toInt(),
        outline = 0x4DECE8DD,
        positive = 0xFF4DCE85.toInt(),
        negative = 0xFFE27974.toInt(),
        tileBg = R.drawable.widget_metric_tile_bg_dark,
        pillBg = R.drawable.widget_netflow_pill_bg_dark,
        ovalPositiveBg = R.drawable.widget_oval_positive_bg_dark,
        ovalNegativeBg = R.drawable.widget_oval_negative_bg_dark
    )

    /** null means "forced neither way" — leave every XML-declared color/drawable alone. */
    fun resolve(theme: WidgetTheme): WidgetPalette? = when (theme) {
        WidgetTheme.SYSTEM -> null
        WidgetTheme.LIGHT -> LIGHT
        WidgetTheme.DARK -> DARK
    }
}
