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
}

val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

/** When false, all hazeEffect/glassmorphism modifiers are skipped app-wide. */
val LocalGlassmorphismEnabled = staticCompositionLocalOf { true }
