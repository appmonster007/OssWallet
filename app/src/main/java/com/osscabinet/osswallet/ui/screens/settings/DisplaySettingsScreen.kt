package com.osscabinet.ossWallet.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.osscabinet.ossWallet.ui.screens.settings.display.AdvancedGlassSettings
import com.osscabinet.ossWallet.ui.screens.settings.display.GlassLabScreen
import com.osscabinet.ossWallet.ui.screens.settings.display.MainDisplaySettings

enum class DisplaySubScreen {
    MAIN, ADVANCED_GLASS, GLASS_LAB
}

@Composable
fun DisplaySettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit
) {
    var currentScreen by remember { mutableStateOf(DisplaySubScreen.MAIN) }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            if (targetState.ordinal > initialState.ordinal) {
                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> -width } + fadeOut())
            } else {
                (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> width } + fadeOut())
            }
        },
        label = "SettingsScreenTransition"
    ) { targetScreen ->
        when (targetScreen) {
            DisplaySubScreen.MAIN -> {
                MainDisplaySettings(
                    viewModel = viewModel,
                    onBackClick = onBackClick,
                    onNavigateToAdvancedGlass = { currentScreen = DisplaySubScreen.ADVANCED_GLASS }
                )
            }
            DisplaySubScreen.ADVANCED_GLASS -> {
                BackHandler { currentScreen = DisplaySubScreen.MAIN }
                AdvancedGlassSettings(
                    viewModel = viewModel,
                    onBack = { currentScreen = DisplaySubScreen.MAIN },
                    onNavigateToGlassLab = { currentScreen = DisplaySubScreen.GLASS_LAB }
                )
            }
            DisplaySubScreen.GLASS_LAB -> {
                BackHandler { currentScreen = DisplaySubScreen.ADVANCED_GLASS }
                GlassLabScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = DisplaySubScreen.ADVANCED_GLASS }
                )
            }
        }
    }
}
