package com.osscabinet.ossWallet.ui.screens.dashboard.quickadd

import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.settings.GlassSettings
import com.osscabinet.ossWallet.domain.model.settings.UiDensity
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.ui.theme.LocalGlassmorphismEnabled
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class QuickAddActivity : ComponentActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Disable activity transitions (Modern API 34+ and Legacy fallback)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        window.setBackgroundDrawableResource(android.R.color.transparent)

        setContent {
            val theme by settingsRepository.theme
                .collectAsStateWithLifecycle(initialValue = "System")
            val selectedFont by settingsRepository.selectedFont
                .collectAsStateWithLifecycle(initialValue = "MANROPE")
            val glassSettings by settingsRepository.glassSettings
                .collectAsStateWithLifecycle(initialValue = GlassSettings())
            val uiDensityName by settingsRepository.uiDensity
                .collectAsStateWithLifecycle(initialValue = UiDensity.DEFAULT.name)
            val uiDensity = UiDensity.fromName(uiDensityName)
            val glassmorphismEnabled by settingsRepository.glassmorphismEnabled
                .collectAsStateWithLifecycle(initialValue = true)

            OssWalletTheme(
                theme = theme,
                fontName = selectedFont,
                glassSettings = glassSettings,
                uiDensity = uiDensity,
                setWindowBackground = false
            ) {
                CompositionLocalProvider(LocalGlassmorphismEnabled provides glassmorphismEnabled) {
                    QuickAddSheet(onDismiss = ::finish)
                }
            }
        }
    }
}
