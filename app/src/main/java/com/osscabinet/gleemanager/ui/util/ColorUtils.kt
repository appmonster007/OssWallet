package com.osscabinet.gleemanager.ui.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

fun Color.isDark(): Boolean {
    val luminance = 0.299 * red + 0.587 * green + 0.114 * blue
    return luminance < 0.5
}

/**
 * Pastels a color by increasing its lightness and decreasing its saturation.
 * This makes it suitable for Material You surfaces.
 */
fun Color.pastel(): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(this.toArgb(), hsl)
    
    // Adjust saturation to be subtle (e.g., 20-40%)
    hsl[1] = hsl[1].coerceAtMost(0.35f).coerceAtLeast(0.15f)
    
    // Adjust lightness to be very light (e.g., 90-95%)
    hsl[2] = 0.92f
    
    return Color(ColorUtils.HSLToColor(hsl))
}

/**
 * Creates a container color from a seed color, following Material You logic.
 */
fun Color.toContainerColor(isDarkTheme: Boolean, isAmoled: Boolean = false): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(this.toArgb(), hsl)
    
    if (isAmoled) {
        // For Amoled, we want a very dark but slightly tinted surface
        hsl[1] = hsl[1].coerceAtMost(0.2f)
        hsl[2] = 0.05f // Almost black
    } else if (isDarkTheme) {
        hsl[1] = hsl[1].coerceAtMost(0.3f)
        hsl[2] = 0.15f
    } else {
        hsl[1] = hsl[1].coerceAtMost(0.35f)
        hsl[2] = 0.92f
    }
    
    return Color(ColorUtils.HSLToColor(hsl))
}

/**
 * Creates an on-container color that has high contrast with the container color.
 */
fun Color.toOnContainerColor(isDarkTheme: Boolean, isAmoled: Boolean = false): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(this.toArgb(), hsl)
    
    if (isAmoled || isDarkTheme) {
        hsl[1] = hsl[1].coerceAtMost(0.4f)
        hsl[2] = 0.9f
    } else {
        hsl[1] = hsl[1].coerceAtMost(0.5f)
        hsl[2] = 0.25f
    }
    
    return Color(ColorUtils.HSLToColor(hsl))
}
