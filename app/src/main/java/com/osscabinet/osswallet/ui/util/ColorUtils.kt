package com.osscabinet.ossWallet.ui.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import com.osscabinet.ossWallet.domain.formatter.ThemeMode

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
 * @deprecated Replaced by [applyFlowTint] (income/expense badges) and [applyAccountTint]
 * (account cards). All call sites have been migrated. Remove this function once the
 * deprecated String overload and its two callers below are also cleaned up.
 */
@Deprecated(
    message = "Use applyFlowTint for income/expense badges or applyAccountTint for account cards.",
    replaceWith = ReplaceWith("applyFlowTint(mode) or applyAccountTint(mode)")
)
fun Color.applyAeonTint(mode: ThemeMode): Pair<Color, Color> {
    val isDark = mode.isDarkTheme
    val isAmoled = mode.isAmoled

    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(this.toArgb(), hsl)

    val baseSaturation = hsl[1]
    val baseLuminance = hsl[2]

    val backgroundColor: Color
    val inkColor: Color

    when {
        isAmoled -> {
            // Pattern: Very deep background + bright ink (maximum contrast for OLED)
            // Used for AMOLED displays where burn-in is a concern
            val bgHsl = hsl.copyOf()
            bgHsl[1] = baseSaturation.coerceIn(0.35f, 0.75f) // Maintain saturation
            bgHsl[2] = 0.10f // Very deep (6-12% range)
            backgroundColor = Color(ColorUtils.HSLToColor(bgHsl))

            val inkHsl = hsl.copyOf()
            // Shift ink saturation downward for readability while keeping it vibrant
            inkHsl[1] = (baseSaturation * 0.5f).coerceIn(0.25f, 0.45f)
            inkHsl[2] = 0.92f // Very bright for contrast
            inkColor = Color(ColorUtils.HSLToColor(inkHsl))
        }

        isDark -> {
            // Pattern: Dark saturated background + vibrant pastel ink
            // Dark mode should have more visual depth and better color distinction
            val bgHsl = hsl.copyOf()
            // Preserve original saturation for color vibrancy, coerce to keep it saturated
            bgHsl[1] = baseSaturation.coerceIn(0.30f, 0.65f)
            // Background lightness: adapt based on original color's brightness
            // Lighter original colors -> slightly lighter background, darker colors -> darker
            bgHsl[2] = when {
                baseLuminance > 0.7f -> 0.22f  // Very light original -> lighter background
                baseLuminance > 0.5f -> 0.20f  // Medium-light -> medium dark background
                else -> 0.18f                  // Dark original -> darker background
            }
            backgroundColor = Color(ColorUtils.HSLToColor(bgHsl))

            val inkHsl = hsl.copyOf()
            // Create vibrant but readable ink: reduce saturation for pastel effect
            inkHsl[1] = (baseSaturation * 0.6f).coerceIn(0.20f, 0.50f)
            // Ink lightness: adaptive based on background darkness
            // Ensures minimum contrast between background and ink
            inkHsl[2] = when {
                bgHsl[2] < 0.16f -> 0.88f  // Very dark background -> very bright ink
                bgHsl[2] < 0.20f -> 0.85f  // Dark background -> bright ink
                else -> 0.82f              // Medium dark -> medium bright ink
            }
            inkColor = Color(ColorUtils.HSLToColor(inkHsl))
        }

        else -> {
            // Light Theme: Pastel background + saturated dark ink
            // Creates an airy, clean appearance
            val bgHsl = hsl.copyOf()
            bgHsl[1] = baseSaturation.coerceAtMost(0.30f) // Desaturate for pastel
            bgHsl[2] = 0.92f // Very light (90-94%)
            backgroundColor = Color(ColorUtils.HSLToColor(bgHsl))

            val inkHsl = hsl.copyOf()
            inkHsl[1] = baseSaturation.coerceIn(0.50f, 0.90f) // Keep saturation for ink
            inkHsl[2] = 0.28f // Dark ink for high contrast
            inkColor = Color(ColorUtils.HSLToColor(inkHsl))
        }
    }

    return backgroundColor to inkColor
}

/**
 * Tint for income/expense flow badges (NetWorth, CashFlow, AeonFlowBadge).
 *
 * Follows M3 tonal container philosophy per theme:
 * Light:  pastel bg (92% L, low sat) + dark ink (28% L)   — surface-variant container style
 * Dark:   dark tinted bg (18% L, mid sat) + light ink (85% L) — M3 dark container style
 * AMOLED: near-black tinted bg (10% L, mid sat) + bright ink (90% L) — OLED-optimised container
 */
fun Color.applyFlowTint(mode: ThemeMode): Pair<Color, Color> {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(this.toArgb(), hsl)
    val baseSaturation = hsl[1]

    val (bgL, bgS, inkL, inkS) = when {
        mode.isAmoled -> Quadruple(0.12f, baseSaturation.coerceIn(0.30f, 0.55f), 0.88f, baseSaturation.coerceIn(0.50f, 0.85f))
        mode.isDarkTheme -> Quadruple(0.20f, baseSaturation.coerceIn(0.28f, 0.52f), 0.86f, baseSaturation.coerceIn(0.45f, 0.80f))
        else -> Quadruple(0.92f, baseSaturation.coerceAtMost(0.30f), 0.28f, baseSaturation.coerceIn(0.50f, 0.90f))
    }

    val bgHsl = hsl.copyOf().also { it[1] = bgS; it[2] = bgL }
    val inkHsl = hsl.copyOf().also { it[1] = inkS; it[2] = inkL }
    return Color(ColorUtils.HSLToColor(bgHsl)) to Color(ColorUtils.HSLToColor(inkHsl))
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

/**
 * Tint for account cards — respects the user's exact color choice.
 *
 * Light:  exact user color as container; text color auto-derived from luminance.
 * Dark:   user color darkened to ~18–26% lightness, preserving hue/saturation.
 * AMOLED: user color darkened further to ~10–16% lightness, very bright ink.
 */
fun Color.applyAccountTint(mode: ThemeMode): Pair<Color, Color> {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(this.toArgb(), hsl)
    val baseSaturation = hsl[1]
    val baseLuminance = hsl[2]

    return when {
        mode.isAmoled -> {
            val bgHsl = hsl.copyOf().also {
                it[1] = baseSaturation.coerceIn(0.35f, 0.80f)
                it[2] = 0.13f
            }
            val inkHsl = hsl.copyOf().also {
                it[1] = (baseSaturation * 0.5f).coerceIn(0.20f, 0.50f)
                it[2] = 0.90f
            }
            Color(ColorUtils.HSLToColor(bgHsl)) to Color(ColorUtils.HSLToColor(inkHsl))
        }
        mode.isDarkTheme -> {
            val targetL = when {
                baseLuminance > 0.7f -> 0.25f
                baseLuminance > 0.4f -> 0.21f
                else -> 0.17f
            }
            val bgHsl = hsl.copyOf().also {
                it[1] = baseSaturation.coerceIn(0.30f, 0.75f)
                it[2] = targetL
            }
            val inkHsl = hsl.copyOf().also {
                it[1] = (baseSaturation * 0.5f).coerceIn(0.20f, 0.50f)
                it[2] = 0.85f
            }
            Color(ColorUtils.HSLToColor(bgHsl)) to Color(ColorUtils.HSLToColor(inkHsl))
        }
        else -> {
            // Exact user color; derive readable ink from luminance
            val inkColor = if (this.isDark()) Color.White else Color(0xFF1A1A1A)
            this to inkColor
        }
    }
}

/**
 * Convenience overload for theme strings.
 */
@Deprecated("Use ThemeMode overload", ReplaceWith("applyAeonTint(mode)"))
fun Color.applyAeonTint(theme: String): Pair<Color, Color> {
    val isDark = theme == "Dark" || theme == "Amoled"
    val isAmoled = theme == "Amoled"
    return applyAeonTint(ThemeMode(isDarkTheme = isDark, isAmoled = isAmoled))
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
