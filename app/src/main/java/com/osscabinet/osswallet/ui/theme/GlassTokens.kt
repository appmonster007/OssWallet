package com.osscabinet.ossWallet.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint

/**
 * Design tokens for glassmorphism effects using the Haze library.
 */
object GlassTokens {
    val BlurRadius = 8.dp
    val DrawerBlurRadius = 16.dp
    val NoiseFactor = 0f

    // Per-surface blur radii
    val PillBlurRadius = 12.dp
    val CardBlurRadius = 6.dp
    val SheetBlurRadius = 14.dp

    /**
     * Standard glass style for narrow overlay surfaces (e.g. sticky list headers).
     */
    @Composable
    fun style(): HazeStyle {
        val isDark = isSystemInDarkTheme()
        val bgColor = AppColors.cardSurfaceProminent
        return HazeStyle(
            tint = HazeTint(bgColor.copy(alpha = if (isDark) 0.45f else 0.30f)),
            blurRadius = BlurRadius,
            noiseFactor = NoiseFactor,
        )
    }

    /**
     * Heavy glass style for large overlay surfaces (drawer, full-screen sheets).
     */
    @Composable
    fun drawerStyle(): HazeStyle {
        val isDark = isSystemInDarkTheme()
        val bgColor = AppColors.cardSurfaceProminent
        return HazeStyle(
            tint = HazeTint(bgColor.copy(alpha = if (isDark) 0.82f else 0.55f)),
            blurRadius = DrawerBlurRadius,
            noiseFactor = NoiseFactor,
        )
    }

    /**
     * Small compact floating elements (nav pill, FAB chips).
     */
    @Composable
    fun pillStyle(): HazeStyle {
        val isDark = isSystemInDarkTheme()
        val bgColor = AppColors.componentOnCard
        return HazeStyle(
            tint = HazeTint(bgColor.copy(alpha = if (isDark) 0.65f else 0.45f)),
            blurRadius = PillBlurRadius,
            noiseFactor = NoiseFactor,
        )
    }

    /**
     * Dashboard/analytics cards.
     */
    @Composable
    fun cardStyle(): HazeStyle {
        val isDark = isSystemInDarkTheme()
        val bgColor = AppColors.cardSurface
        return HazeStyle(
            tint = HazeTint(bgColor.copy(alpha = if (isDark) 0.60f else 0.40f)),
            blurRadius = CardBlurRadius,
            noiseFactor = NoiseFactor,
        )
    }

    /**
     * Bottom sheets, modals.
     */
    @Composable
    fun sheetStyle(): HazeStyle {
        val isDark = isSystemInDarkTheme()
        val bgColor = AppColors.sheetSurface
        return HazeStyle(
            tint = HazeTint(bgColor.copy(alpha = if (isDark) 0.78f else 0.52f)),
            blurRadius = SheetBlurRadius,
            noiseFactor = NoiseFactor,
        )
    }

    /**
     * Small floating elements (FAB menu label chips, tooltips).
     */
    @Composable
    fun popupStyle(): HazeStyle {
        val isDark = isSystemInDarkTheme()
        val bgColor = AppColors.popupSurface
        return HazeStyle(
            tint = HazeTint(bgColor.copy(alpha = if (isDark) 0.55f else 0.38f)),
            blurRadius = BlurRadius,
            noiseFactor = NoiseFactor,
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

/**
 * Tracks the current nesting level of glass surfaces.
 * Automatically increments when nesting [GlassSurface].
 */
val LocalHazeDepth = staticCompositionLocalOf { 0 }

/** When false, all hazeEffect/glassmorphism modifiers are skipped app-wide. */
val LocalGlassmorphismEnabled = staticCompositionLocalOf { true }
val LocalContourBgVersion = staticCompositionLocalOf { 4 }
