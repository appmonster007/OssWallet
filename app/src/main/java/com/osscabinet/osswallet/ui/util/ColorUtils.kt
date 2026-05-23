package com.osscabinet.ossWallet.ui.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

fun Color.isDark(): Boolean {
    val luminance = 0.299 * red + 0.587 * green + 0.114 * blue
    return luminance < 0.5
}

fun Int.toHex(): String = String.format("#%06X", 0xFFFFFF and this)

fun String.toColorInt(): Int = try {
    android.graphics.Color.parseColor(this)
} catch (e: Exception) {
    -1
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
 * Unified tint system as defined in colors.md Section 2.8 (Category Tint System).
 * Returns a Pair of (Background Color, Ink Color) for accounts, categories, and tags.
 */
fun Color.applyAeonTint(theme: String): Pair<Color, Color> {
    val isDark = theme == "Dark" || theme == "Amoled"
    val isAmoled = theme == "Amoled"

    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(this.toArgb(), hsl)

    val backgroundColor: Color
    val inkColor: Color

    when {
        isAmoled -> {
            // Pattern: Deeper background + brighter ink (higher contrast for OLED)
            val bgHsl = hsl.copyOf()
            bgHsl[1] = bgHsl[1].coerceIn(0.4f, 0.8f) // High saturation
            bgHsl[2] = 0.08f // Very deep
            backgroundColor = Color(ColorUtils.HSLToColor(bgHsl))

            val inkHsl = hsl.copyOf()
            inkHsl[1] = inkHsl[1].coerceAtMost(0.3f) // Pastel ink
            inkHsl[2] = 0.95f // Brighter ink
            inkColor = Color(ColorUtils.HSLToColor(inkHsl))
        }

        isDark -> {
            // Pattern: Saturated dark background + pastel ink
            val bgHsl = hsl.copyOf()
            bgHsl[1] = bgHsl[1].coerceIn(0.3f, 0.6f) // Saturated
            bgHsl[2] = 0.18f // Dark
            backgroundColor = Color(ColorUtils.HSLToColor(bgHsl))

            val inkHsl = hsl.copyOf()
            inkHsl[1] = inkHsl[1].coerceAtMost(0.4f) // Pastel
            inkHsl[2] = 0.85f // Light ink
            inkColor = Color(ColorUtils.HSLToColor(inkHsl))
        }

        else -> {
            // Pattern: Pastel background + saturated dark ink
            val bgHsl = hsl.copyOf()
            bgHsl[1] = bgHsl[1].coerceAtMost(0.3f) // Desaturated
            bgHsl[2] = 0.92f // Very light pastel
            backgroundColor = Color(ColorUtils.HSLToColor(bgHsl))

            val inkHsl = hsl.copyOf()
            inkHsl[1] = inkHsl[1].coerceIn(0.5f, 0.9f) // Saturated
            inkHsl[2] = 0.25f // Dark ink
            inkColor = Color(ColorUtils.HSLToColor(inkHsl))
        }
    }

    return backgroundColor to inkColor
}

/**
 * Creates a container color from a seed color, following Material You logic.
 */
@Deprecated("Use applyAeonTint for consistent branding", ReplaceWith("applyAeonTint(theme).first"))
fun Color.toContainerColor(isDarkTheme: Boolean, isAmoled: Boolean = false): Color {
    val theme = when {
        isAmoled -> "Amoled"
        isDarkTheme -> "Dark"
        else -> "Light"
    }
    return applyAeonTint(theme).first
}

/**
 * Creates an on-container color that has high contrast with the container color.
 */
@Deprecated("Use applyAeonTint for consistent branding", ReplaceWith("applyAeonTint(theme).second"))
fun Color.toOnContainerColor(isDarkTheme: Boolean, isAmoled: Boolean = false): Color {
    val theme = when {
        isAmoled -> "Amoled"
        isDarkTheme -> "Dark"
        else -> "Light"
    }
    return applyAeonTint(theme).second
}

/**
 * Adjusts a color for use in analytical charts (Pie, Treemap) to ensure 
 * appropriate contrast and vibrance according to the theme.
 */
fun Color.toChartColor(isDarkTheme: Boolean, isAmoled: Boolean = false): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(this.toArgb(), hsl)

    if (isAmoled || isDarkTheme) {
        // In dark themes, we want slightly desaturated but brighter tones
        // to avoid "neon" vibration while maintaining legibility.
        hsl[1] = hsl[1].coerceAtMost(0.7f).coerceAtLeast(0.4f)
        hsl[2] = hsl[2].coerceAtMost(0.85f).coerceAtLeast(0.65f)
    } else {
        // In light theme, we want more saturation and slightly deeper tones
        // so they pop against the light surface container.
        hsl[1] = hsl[1].coerceAtMost(0.9f).coerceAtLeast(0.6f)
        hsl[2] = hsl[2].coerceAtMost(0.6f).coerceAtLeast(0.4f)
    }

    return Color(ColorUtils.HSLToColor(hsl))
}

/**
 * Generates a variation of a color for sub-categories.
 * It adjusts the lightness and saturation to create a cohesive but distinct palette 
 * within the SAME color family (no hue shift).
 */
fun Color.generateShade(
    index: Int,
    total: Int,
    isDarkTheme: Boolean,
    isAmoled: Boolean = false
): Color {
    if (total <= 1) return this
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(this.toArgb(), hsl)

    val ratio = index.toFloat() / (total - 1).toFloat()

    // NO Hue Shift - stick to the exact family
    // hsl[0] = hsl[0] // Preserve original hue

    if (isDarkTheme || isAmoled) {
        // High-contrast alternating steps for Dark Mode
        // We alternate lightness to create separation without changing the core color
        val altFactor = if (index % 2 == 0) 0.15f else -0.15f
        hsl[2] = (0.7f + (ratio * 0.15f) + altFactor).coerceIn(0.5f, 0.92f)

        // Vary saturation to add depth
        hsl[1] = (hsl[1] * (0.6f + (1f - ratio) * 0.4f)).coerceIn(0.2f, 0.85f)
    } else {
        // High-contrast alternating steps for Light Mode
        val altFactor = if (index % 2 == 0) 0.18f else -0.18f
        hsl[2] = (0.55f + ((1f - ratio) * 0.35f) + altFactor).coerceIn(0.25f, 0.88f)

        // Maintain saturation for clarity on white
        hsl[1] = (hsl[1] * (0.8f + ratio * 0.2f)).coerceIn(0.4f, 1.0f)
    }

    return Color(ColorUtils.HSLToColor(hsl))
}
