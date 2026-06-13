package com.osscabinet.ossWallet.ui.theme

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.runtime.staticCompositionLocalOf
import com.osscabinet.ossWallet.domain.formatter.ThemeMode
import com.osscabinet.ossWallet.domain.formatter.ThemeHelper
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
    glassSettings: GlassSettings = GlassSettings(),
    uiDensity: UiDensity = UiDensity.DEFAULT,
    setWindowBackground: Boolean = true,
    content: @Composable () -> Unit
) {
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

    val colorScheme = when {
        theme == "Amoled" -> AmoledColorScheme
        dynamicColor && dynamicColorAvailable -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }

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
        LocalAeonDimensions provides AeonDimensions().scaled(uiDensity.scale),
        LocalUiScale provides uiDensity.scale,
        LocalAppColorScheme provides colorScheme,
        LocalThemeMode provides themeMode,
        LocalUiIdentity provides UiIdentity.fromName(identity),
        LocalGlassSettings provides glassSettings
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
    val colorScheme = when (theme) {
        "Amoled" -> AmoledColorScheme
        "Dark"   -> darkColorScheme()
        else     -> lightColorScheme()
    }
    val themeMode = remember(theme) {
        ThemeHelper.getThemeMode(theme, theme != "Light")
    }
    CompositionLocalProvider(
        LocalAppColorScheme provides colorScheme,
        LocalThemeMode provides themeMode,
    ) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
