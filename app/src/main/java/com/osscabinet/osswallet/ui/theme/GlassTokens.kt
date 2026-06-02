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
     * The blur source is a constrained band of relatively neutral list rows, so a
     * light tint alpha is sufficient to keep content legible.
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
     * The blur source spans the entire app — colourful cards, charts, images — so
     * a higher tint alpha and wider blur are needed to prevent the bright/vivid
     * background from washing out text, especially in dark mode.
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
     * Base: AppColors.componentOnCard (surfaceContainerHighest).
     * Dark α: 0.65f   Light α: 0.45f
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
     * Dashboard/analytics cards (light glass over flat screen background).
     * Base: AppColors.cardSurface (surfaceContainer).
     * Dark α: 0.60f   Light α: 0.40f
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
     * Base: AppColors.sheetSurface (surfaceContainerLow).
     * Dark α: 0.78f   Light α: 0.52f
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
     * Base: AppColors.popupSurface (surfaceContainerLowest).
     * Dark α: 0.55f   Light α: 0.38f
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

    /**
     * Filter pill — can't use hazeEffect (Dialog window boundary).
     * Store tint alpha tokens here so the visual-only border/tint is consistent.
     * Dark α: 0.60f   Light α: 0.40f
     * (No HazeStyle function — Dialog prevents real blur)
     */
    val FilterPillDarkAlpha = 0.60f
    val FilterPillLightAlpha = 0.40f
}

/**
 * Layers for the glassmorphism hierarchy.
 */
enum class HazeLayer {
    /** Layer 1: Elements sitting on the screen background (cards, headers). */
    Surface,

    /** Layer 2: Elements sitting on top of everything else (navigation pill, modals). */
    Overlay
}

/**
 * Container for the app's unified haze hierarchy.
 */
data class AeonHazeLayers(
    val surfaceState: HazeState = HazeState(),
    val overlayState: HazeState = HazeState()
)

val LocalAeonHaze = staticCompositionLocalOf<AeonHazeLayers?> { null }

val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

/** When false, all hazeEffect/glassmorphism modifiers are skipped app-wide. */
val LocalGlassmorphismEnabled = staticCompositionLocalOf { true }

/**
 * HazeState whose source is the Scaffold/content-behind-the-pill.
 * @deprecated Use [LocalAeonHaze] and [HazeLayer.Overlay] instead.
 */
@Deprecated("Use LocalAeonHaze instead")
val LocalPillHazeState = staticCompositionLocalOf<HazeState?> { null }

/**
 * HazeState whose source is the flat screen background layer.
 * @deprecated Use [LocalAeonHaze] and [HazeLayer.Surface] instead.
 */
@Deprecated("Use LocalAeonHaze instead")
val LocalCardHazeState = staticCompositionLocalOf<HazeState?> { null }
