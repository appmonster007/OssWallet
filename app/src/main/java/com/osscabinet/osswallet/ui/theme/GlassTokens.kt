package com.osscabinet.ossWallet.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.settings.GlassSettings
import com.osscabinet.ossWallet.domain.model.settings.GlassStyleConfig
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint

/**
 * Named variant identifiers for per-variant glass/surface design tokens.
 */
object GlassVariant {
    const val Standard = "standard"
    const val Card = "card"
    const val Pill = "pill"
    const val Drawer = "drawer"
    const val Sheet = "sheet"
    const val Popup = "popup"
    const val Item = "item"
}

/**
 * Extension to allow granular updates to a [HazeStyle].
 *
 * This function allows replacing the [bgColor] while automatically preserving the
 * existing transparency (alpha), blur, and noise settings from the current style.
 * It also supports full overrides via [config].
 *
 * @param config The design token config to pull values from (blur, alpha, noise).
 * @param isDark Required if [config] is provided to determine light/dark alpha.
 * @param bgColor Explicit background color for the tint.
 * @param alpha Explicit alpha override (0.0 to 1.0).
 * @param blurRadius Explicit blur radius override.
 * @param noiseFactor Explicit noise factor override.
 */
fun HazeStyle.copyStyle(
    config: GlassStyleConfig? = null,
    isDark: Boolean? = null,
    bgColor: Color? = null,
    alpha: Float? = null,
    blurRadius: Dp? = null,
    noiseFactor: Float? = null,
): HazeStyle {
    // 1. Resolve current state
    val currentTint = this.tints.firstOrNull() ?: HazeTint(Color.Unspecified)

    // 2. Resolve Base Color: RGB from bgColor, or fallback to current tint's color
    val baseColor = bgColor ?: currentTint.color

    // 3. Resolve Alpha: Explicit alpha > Config alpha (if isDark provided) > current tint's alpha
    val resolvedAlpha = alpha
        ?: if (config != null && isDark != null) {
            if (isDark) config.glass.darkAlpha else config.glass.lightAlpha
        } else {
            // Keep the transparency level of the current style when only changing color
            currentTint.color.alpha
        }

    val newTint = HazeTint(baseColor.copy(alpha = resolvedAlpha))

    return this.copy(
        tints = listOf(newTint),
        blurRadius = blurRadius ?: config?.glass?.blurRadius?.dp ?: this.blurRadius,
        noiseFactor = noiseFactor ?: config?.glass?.noiseFactor ?: this.noiseFactor,
        fallbackTint = newTint
    )
}

/**
 * Design tokens for glassmorphism effects using the Haze library.
 */
object GlassTokens {
    /**
     * Standard glass style for narrow overlay surfaces (e.g. sticky list headers).
     */
    @Composable
    fun style(isDark: Boolean = isSystemInDarkTheme()): HazeStyle = createHazeStyle(
        config = LocalGlassSettings.current.standard,
        bgColor = AppColors.cardSurfaceProminent,
        isDark = isDark
    )

    /**
     * Heavy glass style for large overlay surfaces (drawer, full-screen sheets).
     */
    @Composable
    fun drawerStyle(isDark: Boolean = isSystemInDarkTheme()): HazeStyle = createHazeStyle(
        config = LocalGlassSettings.current.drawer,
        bgColor = AppColors.cardSurfaceProminent,
        isDark = isDark
    )

    /**
     * Small compact floating elements (nav pill, FAB chips).
     */
    @Composable
    fun pillStyle(isDark: Boolean = isSystemInDarkTheme()): HazeStyle = createHazeStyle(
        config = LocalGlassSettings.current.pill,
        bgColor = AppColors.componentOnCard,
        isDark = isDark
    )

    /**
     * Dashboard/analytics cards.
     */
    @Composable
    fun cardStyle(isDark: Boolean = isSystemInDarkTheme()): HazeStyle = createHazeStyle(
        config = LocalGlassSettings.current.card,
        bgColor = AppColors.cardSurface,
        isDark = isDark
    )

    /**
     * Bottom sheets, modals.
     */
    @Composable
    fun sheetStyle(isDark: Boolean = isSystemInDarkTheme()): HazeStyle = createHazeStyle(
        config = LocalGlassSettings.current.sheet,
        bgColor = AppColors.sheetSurface,
        isDark = isDark
    )

    /**
     * Small floating elements (FAB menu label chips, tooltips).
     */
    @Composable
    fun popupStyle(isDark: Boolean = isSystemInDarkTheme()): HazeStyle = createHazeStyle(
        config = LocalGlassSettings.current.popup,
        bgColor = AppColors.popupSurface,
        isDark = isDark
    )

    /**
     * Glass-on-glass items nested inside cards (budget rows, loan rows, flow badges, etc).
     */
    @Composable
    fun itemStyle(isDark: Boolean = isSystemInDarkTheme()): HazeStyle = createHazeStyle(
        config = LocalGlassSettings.current.item,
        bgColor = AppColors.componentOnCard,
        isDark = isDark
    )

    @Composable
    fun config(hazeLayer: Int): GlassStyleConfig {
        val settings = LocalGlassSettings.current
        return when (hazeLayer) {
            HazeLayer.Drawer -> settings.drawer
            HazeLayer.Overlay -> settings.pill
            else -> settings.standard
        }
    }

    @Composable
    fun configForVariant(variant: String): GlassStyleConfig {
        val s = LocalGlassSettings.current
        return when (variant) {
            GlassVariant.Card -> s.card
            GlassVariant.Pill -> s.pill
            GlassVariant.Drawer -> s.drawer
            GlassVariant.Sheet -> s.sheet
            GlassVariant.Popup -> s.popup
            GlassVariant.Item -> s.item
            else -> s.standard
        }
    }

    @Composable
    fun createHazeStyle(config: GlassStyleConfig, bgColor: Color, isDark: Boolean): HazeStyle {
        val tintAlpha = if (isDark) config.glass.darkAlpha else config.glass.lightAlpha
        return HazeStyle(
            // backgroundColor intentionally omitted (transparent) — an opaque base would
            // paint over the blur and make the surface look solid.
            tint = HazeTint(bgColor.copy(alpha = tintAlpha)),
            blurRadius = config.glass.blurRadius.dp,
            noiseFactor = config.glass.noiseFactor,
            // fallbackTint kicks in when GPU blur is unavailable (older hardware / software rendering).
            fallbackTint = HazeTint(bgColor.copy(alpha = tintAlpha)),
        )
    }
}

/**
 * Semantic depth levels for the glassmorphism hierarchy.
 *
 * The AeonHazeProvider background Box is the sole hazeSource for all three states.
 * GlassSurface elements are pure consumers — they apply hazeEffect only.
 *
 * Explicit hazeSource on non-glass content containers (e.g. a cards Column, an OSM map)
 * is allowed only when those containers are SIBLINGS of their hazeEffect consumers.
 */
object HazeLayer {
    /** Blurs the static screen background. Used by cards, form action cards, header cards. */
    const val Surface = 0

    /** Blurs background + any explicitly registered surface-level content (e.g. card columns).
     *  Used by nav pill, top bar pills, search dropdowns. */
    const val Overlay = 1

    /** Blurs all visible screen content captured via NavHost hazeSource in OssWalletNavGraph.
     *  Used by the navigation drawer only. */
    const val Drawer = 2
}

/**
 * Container for the app's unified haze hierarchy.
 * Three states cover all semantic depths: Surface, Overlay, Drawer.
 */
data class AeonHazeLayers(
    val states: List<HazeState> = List(5) { HazeState() }
)

val LocalAeonHaze = staticCompositionLocalOf<AeonHazeLayers?> { null }

/** Non-null inside [AeonGlassHost]. Always carries the **nearest** glass surface as a
 *  hazeSource — automatically scoped by nesting depth via CompositionLocal shadowing.
 *  Reading this from any depth always gives the immediately enclosing glass surface. */
val LocalParentGlassHaze = compositionLocalOf<HazeState?> { null }

val LocalGlassSettings = staticCompositionLocalOf { GlassSettings() }

/**
 * Tracks the current nesting level of glass surfaces.
 * Automatically increments when nesting [GlassSurface].
 */
val LocalHazeDepth = staticCompositionLocalOf { 0 }

/** When false, all hazeEffect/glassmorphism modifiers are skipped app-wide. */
val LocalGlassmorphismEnabled = staticCompositionLocalOf { true }

/**
 * Kinds of glass surface that can be switched off individually (Design Lab → Glass Elements),
 * so the cost of each can be measured and tuned. Pass one to [GlassSurface] via `element`.
 */
enum class GlassElement(val label: String, val description: String) {
    CARD("Cards", "Dashboard and analytics card shells"),
    TOP_BAR("Top bar", "Top bar pills, search field and tab pills"),
    DRAWER("Drawer", "Navigation drawer"),
    NAV_PILL("Bottom navigation", "Bottom nav pill, its menus and the FAB"),
    HEADER("List headers", "Transaction group headers and filter pill"),
    SHEET("Sheets", "Glass bottom sheets and quick add"),
    ENTRY("Entry & lists", "Entry form sections, chips, dialogs and list items"),
    OTHER("Other", "Anything not listed above");
}

/** Elements whose blur is individually disabled. Empty by default (everything blurs). */
val LocalGlassDisabledElements = staticCompositionLocalOf<Set<GlassElement>> { emptySet() }
val LocalContourBgVersion = staticCompositionLocalOf { 4 }
val LocalContourLinesEnabled = staticCompositionLocalOf { true }
val LocalRandomizeContourEachLoad = staticCompositionLocalOf { true }
