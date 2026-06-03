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
import com.osscabinet.ossWallet.domain.formatter.ThemeMode
import com.osscabinet.ossWallet.domain.formatter.ThemeHelper
import com.osscabinet.ossWallet.domain.model.settings.GlassSettings

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceDark,
    scrim = Color.Black,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    inverseSurface = InverseSurfaceLight,
    inverseOnSurface = InverseOnSurfaceLight,
    scrim = Color.Black,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight
)

private val AmoledColorScheme = darkColorScheme(
    primary = PrimaryAmoled,
    onPrimary = OnPrimaryAmoled,
    primaryContainer = PrimaryContainerAmoled,
    onPrimaryContainer = OnPrimaryContainerAmoled,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundAmoled,
    onBackground = OnBackgroundAmoled,
    surface = SurfaceAmoled,
    onSurface = OnSurfaceAmoled,
    surfaceVariant = SurfaceVariantAmoled,
    onSurfaceVariant = OnSurfaceVariantAmoled,
    outline = OutlineAmoled,
    outlineVariant = OutlineVariantAmoled,
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceDark,
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
object AeonTheme {
    val dimensions: AeonDimensions
        @Composable
        @ReadOnlyComposable
        get() = LocalAeonDimensions.current
}

@Composable
fun OssWalletTheme(
    theme: String = "System",
    dynamicColor: Boolean = true,
    fontName: String = AppFont.DEFAULT.name,
    glassSettings: GlassSettings = GlassSettings(),
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

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val backgroundColor = colorScheme.background.toArgb()

            window.statusBarColor = backgroundColor
            window.navigationBarColor = backgroundColor

            // Set window background to avoid flash during transitions
            window.setBackgroundDrawable(ColorDrawable(backgroundColor))

            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalAeonDimensions provides AeonDimensions(),
        LocalAppColorScheme provides colorScheme,
        LocalThemeMode provides themeMode,
        LocalGlassSettings provides glassSettings
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = createTypography(AppFont.fromName(fontName)),
            content = content
        )
    }
}
