package com.osscabinet.ossWallet.widget

import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.widget.RemoteViews
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
    @ColorInt val warning: Int,
    @DrawableRes val tileBg: Int,
    @DrawableRes val pillBg: Int,
    @DrawableRes val ovalPositiveBg: Int,
    @DrawableRes val ovalNegativeBg: Int,
    @DrawableRes val chipNeutralBg: Int,
    @DrawableRes val chipNegativeBg: Int,
    @DrawableRes val chipWarningBg: Int,
    @ColorInt val track: Int,
    @ColorInt val tick: Int
)

object WidgetColors {
    internal val LIGHT = WidgetPalette(
        surface = 0xFFFAF8F1.toInt(),
        onSurface = 0xFF1B1914.toInt(),
        onSurfaceVariant = 0xFF6B6658.toInt(),
        accent = 0xFF8A6F3C.toInt(),
        outline = 0xFFC9C2B2.toInt(),
        positive = 0xFF1E9B4F.toInt(),
        negative = 0xFFC8463F.toInt(),
        warning = 0xFFB7791F.toInt(),
        tileBg = R.drawable.widget_metric_tile_bg_light,
        pillBg = R.drawable.widget_netflow_pill_bg_light,
        ovalPositiveBg = R.drawable.widget_oval_positive_bg_light,
        ovalNegativeBg = R.drawable.widget_oval_negative_bg_light,
        chipNeutralBg = R.drawable.widget_chip_neutral_light,
        chipNegativeBg = R.drawable.widget_chip_negative_light,
        chipWarningBg = R.drawable.widget_chip_warning_light,
        track = 0xFFEDE8DA.toInt(),
        tick = 0xB31B1914.toInt()
    )

    internal val DARK = WidgetPalette(
        surface = 0xFF131312.toInt(),
        onSurface = 0xFFECE8DD.toInt(),
        onSurfaceVariant = 0xFF9A958A.toInt(),
        accent = 0xFFD6C189.toInt(),
        outline = 0x4DECE8DD,
        positive = 0xFF4DCE85.toInt(),
        negative = 0xFFE27974.toInt(),
        warning = 0xFFE3A94A.toInt(),
        tileBg = R.drawable.widget_metric_tile_bg_dark,
        pillBg = R.drawable.widget_netflow_pill_bg_dark,
        ovalPositiveBg = R.drawable.widget_oval_positive_bg_dark,
        ovalNegativeBg = R.drawable.widget_oval_negative_bg_dark,
        chipNeutralBg = R.drawable.widget_chip_neutral_dark,
        chipNegativeBg = R.drawable.widget_chip_negative_dark,
        chipWarningBg = R.drawable.widget_chip_warning_dark,
        track = 0xFF2A2A27.toInt(),
        tick = 0xB3ECE8DD.toInt()
    )

    /**
     * The app's "Amoled" theme (H identity): same ink/accent/semantic colours as Dark on the
     * pure-black panel (HAmoledSurfaceContainer) with the brighter hairline (HAmoledOutline).
     */
    internal val AMOLED = DARK.copy(
        surface = 0xFF0C0C0B.toInt(),
        outline = 0x33ECE8DD
    )

    /**
     * The app's own theme setting ("System"/"Light"/"Dark"/"Amoled"), kept current by
     * OssWalletApp so [WidgetTheme.SYSTEM] widgets look like the app instead of the device.
     */
    @Volatile
    var appTheme: String? = null

    private fun isSystemNight(): Boolean =
        Resources.getSystem().configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

    /** The palette for the device's current night mode, for colours that have no XML default. */
    internal fun systemPalette(): WidgetPalette = if (isSystemNight()) DARK else LIGHT

    /**
     * Names the palette [resolve] picks for [theme]. Carried in each list widget's adapter intent
     * so the rows are built from exactly the palette the header was just rendered with, and so a
     * change of palette is a different intent: the host then rebinds the adapter and drops its
     * cached rows instead of leaving old-theme rows under a new-theme header.
     */
    fun themeKey(theme: WidgetTheme): String = when (resolve(theme)) {
        LIGHT -> "light"
        DARK -> "dark"
        AMOLED -> "amoled"
        else -> "system"
    }

    /** The theme variant of a widget layout (see the widget_*_t0/t1/t2 wrappers): a different id per theme. */
    fun layoutFor(palette: WidgetPalette?, system: Int, light: Int, dark: Int): Int = when (palette) {
        null -> system
        LIGHT -> light
        else -> dark
    }

    fun paletteForKey(key: String?): WidgetPalette? = when (key) {
        "light" -> LIGHT
        "dark" -> DARK
        "amoled" -> AMOLED
        else -> null
    }

    /** null means "forced neither way" — leave every XML-declared color/drawable alone. */
    fun resolve(theme: WidgetTheme): WidgetPalette? = when (theme) {
        WidgetTheme.SYSTEM -> when (appTheme) {
            "Light" -> LIGHT
            "Dark" -> DARK
            "Amoled" -> AMOLED
            else -> null
        }
        WidgetTheme.LIGHT -> LIGHT
        WidgetTheme.DARK -> DARK
    }
}

/**
 * Sets a text colour from [palette] when a theme is forced. With no palette (the device's night
 * mode decides) the colour must still follow it, but a colour looked up from a Context here is
 * resolved against the widget process's stale configuration (light in a dark launcher). So on
 * API 34+ both sides are handed to the host, which picks one on every night-mode change; older
 * releases get the device's current mode.
 *
 * Every themed property is set on EVERY render, never only when a theme is forced: a host that
 * already shows this widget re-applies the new RemoteViews onto the view it has (the layout is
 * unchanged), so a property the new RemoteViews leaves out keeps whatever an earlier render set.
 * Leaving the forced-theme overrides out when switching back to System is what left part of a
 * widget in the old theme.
 */
fun RemoteViews.setThemedTextColor(viewId: Int, palette: WidgetPalette?, pick: (WidgetPalette) -> Int) {
    when {
        palette != null -> setTextColor(viewId, pick(palette))
        Build.VERSION.SDK_INT >= 34 ->
            setColorInt(viewId, "setTextColor", pick(WidgetColors.LIGHT), pick(WidgetColors.DARK))
        else -> setTextColor(viewId, pick(WidgetColors.systemPalette()))
    }
}

/** Same as [setThemedTextColor] for a view's background colour, at [opacityPercent] of its own alpha. */
fun RemoteViews.setThemedBackgroundColor(
    viewId: Int,
    palette: WidgetPalette?,
    opacityPercent: Int = 100,
    pick: (WidgetPalette) -> Int
) {
    fun withOpacity(color: Int): Int {
        val alpha = (android.graphics.Color.alpha(color) * opacityPercent.coerceIn(0, 100) / 100f).toInt()
        return (alpha shl 24) or (color and 0x00FFFFFF)
    }
    when {
        palette != null -> setInt(viewId, "setBackgroundColor", withOpacity(pick(palette)))
        Build.VERSION.SDK_INT >= 34 ->
            setColorInt(viewId, "setBackgroundColor", withOpacity(pick(WidgetColors.LIGHT)), withOpacity(pick(WidgetColors.DARK)))
        else -> setInt(viewId, "setBackgroundColor", withOpacity(pick(WidgetColors.systemPalette())))
    }
}

/** Same as [setThemedTextColor] for an ImageView's colour filter. */
fun RemoteViews.setThemedColorFilter(viewId: Int, palette: WidgetPalette?, pick: (WidgetPalette) -> Int) {
    when {
        palette != null -> setInt(viewId, "setColorFilter", pick(palette))
        Build.VERSION.SDK_INT >= 34 ->
            setColorInt(viewId, "setColorFilter", pick(WidgetColors.LIGHT), pick(WidgetColors.DARK))
        else -> setInt(viewId, "setColorFilter", pick(WidgetColors.systemPalette()))
    }
}

/**
 * Background drawable: the palette's fixed-colour one when a theme is forced, otherwise the
 * night-aware [systemRes]. Always set one of them — see [setThemedTextColor] for why.
 */
fun RemoteViews.setThemedBackgroundResource(
    viewId: Int,
    palette: WidgetPalette?,
    forced: (WidgetPalette) -> Int,
    @androidx.annotation.DrawableRes systemRes: Int
) {
    setInt(viewId, "setBackgroundResource", palette?.let(forced) ?: systemRes)
}
