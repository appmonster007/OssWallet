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
    val NoiseFactor = 0f

    /**
     * Theme-aware glass style. Uses AppColors.cardSurfaceProminent (surfaceContainerHigh)
     * as tint base so the frosted effect looks elevated above the transaction list —
     * matching the persistent background added to TransactionHeader before hazeEffect.
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
}

val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

/** When false, all hazeEffect/glassmorphism modifiers are skipped app-wide. */
val LocalGlassmorphismEnabled = staticCompositionLocalOf { true }
