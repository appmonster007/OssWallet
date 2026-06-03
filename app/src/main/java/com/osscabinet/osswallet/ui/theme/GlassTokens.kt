package com.osscabinet.ossWallet.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.settings.GlassSettings
import com.osscabinet.ossWallet.domain.model.settings.GlassStyleConfig
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint

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

    @Composable
    private fun createHazeStyle(config: GlassStyleConfig, bgColor: Color, isDark: Boolean): HazeStyle {
        return HazeStyle(
            tint = HazeTint(bgColor.copy(alpha = if (isDark) config.darkAlpha else config.lightAlpha)),
            blurRadius = config.blurRadius.dp,
            noiseFactor = config.noiseFactor,
        )
    }
}

/**
 * Semantic depth levels for the glassmorphism hierarchy.
 * depth 0: Blurs Background, sources all levels >= 1
 * depth 1: Blurs Background+Surface, sources all levels >= 2
 * depth 2: Blurs Background+Surface+Overlay, sources all levels >= 3
 */
object HazeLayer {
    /** Layer 1: Elements sitting on the screen background (cards, headers). */
    const val Surface = 0

    /** Layer 2: Elements sitting on top of everything else (navigation pill, modals). */
    const val Overlay = 1

    /** Layer 3: Global system-level overlays (navigation drawer). */
    const val Drawer = 2
}

/**
 * Container for the app's unified haze hierarchy.
 * Supports up to 10 layers of overlapping glass.
 */
data class AeonHazeLayers(
    val states: List<HazeState> = List(10) { HazeState() }
)

val LocalAeonHaze = staticCompositionLocalOf<AeonHazeLayers?> { null }

val LocalGlassSettings = staticCompositionLocalOf { GlassSettings() }

/**
 * Tracks the current nesting level of glass surfaces.
 * Automatically increments when nesting [GlassSurface].
 */
val LocalHazeDepth = staticCompositionLocalOf { 0 }

/** When false, all hazeEffect/glassmorphism modifiers are skipped app-wide. */
val LocalGlassmorphismEnabled = staticCompositionLocalOf { true }
val LocalContourBgVersion = staticCompositionLocalOf { 4 }
val LocalContourLinesEnabled = staticCompositionLocalOf { true }
val LocalRandomizeContourEachLoad = staticCompositionLocalOf { true }
