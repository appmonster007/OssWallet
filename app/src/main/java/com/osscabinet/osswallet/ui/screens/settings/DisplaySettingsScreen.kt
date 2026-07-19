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
import com.osscabinet.ossWallet.ui.screens.settings.display.DesignLabScreen
import com.osscabinet.ossWallet.ui.screens.settings.display.MainDisplaySettings

enum class DisplaySubScreen {
    MAIN, DESIGN_LAB
}

@Composable
fun DisplaySettingsScreen(
    viewModel: SettingsViewModel,
    actions: DisplaySettingsActions
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
                    onBackClick = actions.onBackClick,
                    onNavigateToDesignLab = { currentScreen = DisplaySubScreen.DESIGN_LAB }
                )
            }

            DisplaySubScreen.DESIGN_LAB -> {
                BackHandler { currentScreen = DisplaySubScreen.MAIN }
                DesignLabScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = DisplaySubScreen.MAIN }
                )
            }
        }
    }
}

data class DisplaySettingsActions(
    val onBackClick: () -> Unit
)
