package com.osscabinet.ossWallet.ui.theme

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.core.view.WindowCompat
import com.osscabinet.ossWallet.domain.formatter.ThemeHelper
import com.osscabinet.ossWallet.domain.formatter.ThemeMode
import com.osscabinet.ossWallet.domain.model.settings.CardOpenAnimation
import com.osscabinet.ossWallet.domain.model.settings.GlassSettings
import com.osscabinet.ossWallet.domain.model.settings.UiDensity
import com.osscabinet.ossWallet.domain.model.settings.UiIdentity

private val AmoledColorScheme = darkColorScheme(
    primary = PrimaryAmoled,
    onPrimary = OnPrimaryAmoled,
    primaryContainer = PrimaryContainerAmoled,
    onPrimaryContainer = OnPrimaryContainerAmoled,
    secondary = Color(0xFFC4C5DD),
    onSecondary = Color(0xFF2D2F42),
    secondaryContainer = Color(0xFF434559),
    onSecondaryContainer = Color(0xFFE0E1F9),
    tertiary = Color(0xFFC6C2EA),
    onTertiary = Color(0xFF2F2D4D),
    tertiaryContainer = Color(0xFF454364),
    onTertiaryContainer = Color(0xFFE3DFFF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = BackgroundAmoled,
    onBackground = OnBackgroundAmoled,
    surface = SurfaceAmoled,
    onSurface = OnSurfaceAmoled,
    surfaceVariant = SurfaceVariantAmoled,
    onSurfaceVariant = OnSurfaceVariantAmoled,
    outline = OutlineAmoled,
    outlineVariant = OutlineVariantAmoled,
    inverseSurface = Color(0xFFE1E2E9),
    inverseOnSurface = Color(0xFF0E1015),
    scrim = Color.Black,
    surfaceContainerLowest = SurfaceContainerLowestAmoled,
    surfaceContainerLow = SurfaceContainerLowAmoled,
    surfaceContainer = SurfaceContainerAmoled,
    surfaceContainerHigh = SurfaceContainerHighAmoled,
    surfaceContainerHighest = SurfaceContainerHighestAmoled
)

// --- H "Editorial Instrument" color schemes ---
// Fixed neutrals; primary carries the champagne/bronze accent (dynamic-accent eligible).

private val HDarkColorScheme = darkColorScheme(
    primary = HDarkAccent,
    onPrimary = HDarkOnAccent,
    primaryContainer = HDarkAccentContainer,
    onPrimaryContainer = HDarkOnAccentContainer,
    secondary = HDarkSecondary,
    onSecondary = HDarkOnSecondary,
    secondaryContainer = HDarkSecondaryContainer,
    onSecondaryContainer = HDarkOnSecondaryContainer,
    tertiary = HDarkSecondary,
    onTertiary = HDarkOnSecondary,
    tertiaryContainer = HDarkSecondaryContainer,
    onTertiaryContainer = HDarkOnSecondaryContainer,
    error = HDarkError,
    onError = HDarkOnError,
    errorContainer = HDarkErrorContainer,
    onErrorContainer = HDarkOnErrorContainer,
    background = HDarkBackground,
    onBackground = HDarkOnSurface,
    surface = HDarkBackground,
    onSurface = HDarkOnSurface,
    surfaceVariant = HDarkSurfaceVariant,
    onSurfaceVariant = HDarkOnSurfaceVariant,
    outline = HDarkOutline,
    outlineVariant = HDarkOutlineVariant,
    surfaceContainerLowest = HDarkSurfaceContainerLowest,
    surfaceContainerLow = HDarkSurfaceContainerLow,
    surfaceContainer = HDarkSurfaceContainer,
    surfaceContainerHigh = HDarkSurfaceContainerHigh,
    surfaceContainerHighest = HDarkSurfaceContainerHighest,
)

private val HLightColorScheme = lightColorScheme(
    primary = HLightAccent,
    onPrimary = HLightOnAccent,
    primaryContainer = HLightAccentContainer,
    onPrimaryContainer = HLightOnAccentContainer,
    secondary = HLightSecondary,
    onSecondary = HLightOnSecondary,
    secondaryContainer = HLightSecondaryContainer,
    onSecondaryContainer = HLightOnSecondaryContainer,
    tertiary = HLightSecondary,
    onTertiary = HLightOnSecondary,
    tertiaryContainer = HLightSecondaryContainer,
    onTertiaryContainer = HLightOnSecondaryContainer,
    error = HLightError,
    onError = HLightOnError,
    errorContainer = HLightErrorContainer,
    onErrorContainer = HLightOnErrorContainer,
    background = HLightBackground,
    onBackground = HLightOnSurface,
    surface = HLightBackground,
    onSurface = HLightOnSurface,
    surfaceVariant = HLightSurfaceVariant,
    onSurfaceVariant = HLightOnSurfaceVariant,
    outline = HLightOutline,
    outlineVariant = HLightOutlineVariant,
    surfaceContainerLowest = HLightSurfaceContainerLowest,
    surfaceContainerLow = HLightSurfaceContainerLow,
    surfaceContainer = HLightSurfaceContainer,
    surfaceContainerHigh = HLightSurfaceContainerHigh,
    surfaceContainerHighest = HLightSurfaceContainerHighest,
)

private val HAmoledColorScheme = darkColorScheme(
    primary = HDarkAccent,
    onPrimary = HDarkOnAccent,
    primaryContainer = HDarkAccentContainer,
    onPrimaryContainer = HDarkOnAccentContainer,
    secondary = HDarkSecondary,
    onSecondary = HDarkOnSecondary,
    secondaryContainer = HDarkSecondaryContainer,
    onSecondaryContainer = HDarkOnSecondaryContainer,
    tertiary = HDarkSecondary,
    onTertiary = HDarkOnSecondary,
    tertiaryContainer = HDarkSecondaryContainer,
    onTertiaryContainer = HDarkOnSecondaryContainer,
    error = HDarkError,
    onError = HDarkOnError,
    errorContainer = HDarkErrorContainer,
    onErrorContainer = HDarkOnErrorContainer,
    background = HAmoledBackground,
    onBackground = HDarkOnSurface,
    surface = HAmoledBackground,
    onSurface = HDarkOnSurface,
    surfaceVariant = HAmoledSurfaceVariant,
    onSurfaceVariant = HDarkOnSurfaceVariant,
    outline = HAmoledOutline,
    outlineVariant = HAmoledOutlineVariant,
    surfaceContainerLowest = HAmoledSurfaceContainerLowest,
    surfaceContainerLow = HAmoledSurfaceContainerLow,
    surfaceContainer = HAmoledSurfaceContainer,
    surfaceContainerHigh = HAmoledSurfaceContainerHigh,
    surfaceContainerHighest = HAmoledSurfaceContainerHighest,
)

/**
 * Applies the active [UiIdentity] palette over [base]. Only H is implemented; A/B
 * fall through to [base]. When [accentOverride] is non-null (dynamic-accent toggle on,
 * Android 12+), only the accent token (`primary`) is replaced — all H neutrals stay fixed.
 */
fun applyIdentityToColorScheme(
    base: ColorScheme,
    identity: UiIdentity,
    themeMode: ThemeMode,
    accentOverride: Color? = null,
    accentContainerOverride: Color? = null,
    onAccentContainerOverride: Color? = null,
): ColorScheme = when (identity) {
    UiIdentity.H -> {
        val h = when {
            themeMode.isAmoled -> HAmoledColorScheme
            themeMode.isDarkTheme -> HDarkColorScheme
            else -> HLightColorScheme
        }
        if (accentOverride != null) {
            val onAccent = if (accentOverride.luminance() > 0.5f) ColorOnLight else ColorOnDark
            h.copy(
                primary = accentOverride,
                onPrimary = onAccent,
                primaryContainer = accentContainerOverride ?: h.primaryContainer,
                onPrimaryContainer = onAccentContainerOverride ?: h.onPrimaryContainer,
            )
        } else h
    }

    else -> base
}

/**
 * Helper object to access the custom theme properties.
 */
val LocalUiScale = staticCompositionLocalOf { 1.0f }

object AeonTheme {
    val dimensions: AeonDimensions
        @Composable
        @ReadOnlyComposable
        get() = LocalAeonDimensions.current

    val uiScale: Float
        @Composable
        @ReadOnlyComposable
        get() = LocalUiScale.current
}

@Composable
fun OssWalletTheme(
    theme: String = "System",
    dynamicColor: Boolean = true,
    fontName: String = AppFont.DEFAULT.name,
    identity: String = UiIdentity.DEFAULT.name,
    dynamicAccent: Boolean = false,
    glassSettings: GlassSettings = GlassSettings(),
    uiDensity: UiDensity = UiDensity.DEFAULT,
    cardOpenAnimation: CardOpenAnimation = CardOpenAnimation.DEFAULT,
    cardListEdgeSpacing: Dp = AeonSize.D14.value,
    cardListGap: Dp = AeonSize.D12.value,
    setWindowBackground: Boolean = true,
    content: @Composable () -> Unit
) {
    val uiIdentity = UiIdentity.fromName(identity)
    val darkTheme = when (theme) {
        "Light" -> false
        "Dark", "Amoled" -> true
        else -> isSystemInDarkTheme()
    }

    // Resolve the actual state once
    val themeMode = remember(theme, darkTheme) {
        ThemeHelper.getThemeMode(theme, darkTheme)
    }

    // Dynamic color is available on Android 12+
    val dynamicColorAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val context = LocalContext.current

    // Full Material You (wallpaper bg/surface) is incompatible with H's editorial palette,
    // so it is only applied for non-H identities. Under H, only the accent token may be
    // wallpaper-derived, via [dynamicAccent].
    val baseScheme = when {
        theme == "Amoled" -> AmoledColorScheme
        dynamicColor && dynamicColorAvailable && uiIdentity != UiIdentity.H -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }

    val dynamicAccentScheme = if (dynamicAccent && dynamicColorAvailable) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else null

    val colorScheme = applyIdentityToColorScheme(
        base = baseScheme,
        identity = uiIdentity,
        themeMode = themeMode,
        accentOverride = dynamicAccentScheme?.primary,
        accentContainerOverride = dynamicAccentScheme?.primaryContainer,
        onAccentContainerOverride = dynamicAccentScheme?.onPrimaryContainer,
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val backgroundColor = colorScheme.background.toArgb()

            if (setWindowBackground) {
                // Set window background to avoid flash during transitions
                window.setBackgroundDrawable(ColorDrawable(backgroundColor))
            }

            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalAeonDimensions provides AeonDimensions().scaled(uiDensity.scale).copy(
            cardListEdgeSpacing = cardListEdgeSpacing,
            cardListGap = cardListGap
        ),
        LocalUiScale provides uiDensity.scale,
        LocalAppColorScheme provides colorScheme,
        LocalThemeMode provides themeMode,
        LocalUiIdentity provides uiIdentity,
        LocalAppFont provides AppFont.fromName(fontName),
        LocalGlassSettings provides glassSettings,
        LocalCardOpenAnimation provides cardOpenAnimation
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = createTypography(AppFont.fromName(fontName)),
            content = content
        )
    }
}

/**
 * Lightweight theme wrapper for in-screen previews (Design Lab, Glass Lab, etc.).
 * Provides the correct [LocalAppColorScheme], [LocalThemeMode], and [MaterialTheme] colors
 * for [theme] without triggering status-bar or window side effects.
 * Always uses static palettes — dynamic color is intentionally not applied.
 */
@Composable
fun OssWalletPreviewTheme(
    theme: String,
    content: @Composable () -> Unit,
) {
    val themeMode = remember(theme) {
        ThemeHelper.getThemeMode(theme, theme != "Light")
    }
    val baseScheme = when (theme) {
        "Amoled" -> AmoledColorScheme
        "Dark" -> darkColorScheme()
        else -> lightColorScheme()
    }
    // Reflect the active identity (H) so previews match the real app across Light/Dark/AMOLED.
    val colorScheme = applyIdentityToColorScheme(baseScheme, UiIdentity.DEFAULT, themeMode)
    CompositionLocalProvider(
        LocalAppColorScheme provides colorScheme,
        LocalThemeMode provides themeMode,
    ) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
