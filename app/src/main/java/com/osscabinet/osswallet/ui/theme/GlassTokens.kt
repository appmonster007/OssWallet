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
    val BlurRadius = 12.dp
    val NoiseFactor = 0.1f

    /**
     * Theme-aware glass style. Derives tint from the current screen background so
     * the frosted effect matches any background — light, dark, or custom-tinted.
     */
    @Composable
    fun style(): HazeStyle {
        val isDark = isSystemInDarkTheme()
        val bgColor = AppColors.screenBackground
        return HazeStyle(
            tint = HazeTint(bgColor.copy(alpha = if (isDark) 0.55f else 0.75f)),
            blurRadius = BlurRadius,
            noiseFactor = NoiseFactor,
        )
    }
}

val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }
