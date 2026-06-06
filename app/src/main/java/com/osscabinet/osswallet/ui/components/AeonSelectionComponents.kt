package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.osscabinet.ossWallet.ui.theme.AeonSize
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

/**
 * A dialog that behaves like a full screen, allowing "screen" transitions without
 * complex navigation changes.
 */
@Composable
fun AeonFullScreenDialog(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    if (androidx.compose.ui.platform.LocalInspectionMode.current) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = AppColors.background
        ) {
            content()
        }
    } else {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = AppColors.background
            ) {
                content()
            }
        }
    }
}

/**
 * A standardized selection screen component that replaces bottom sheets and alert dialogs.
 * Following the Aeon "Floating Pill" glass architecture.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AeonSelectionScreen(
    title: String,
    onBack: () -> Unit,
    containerColor: Color = AppColors.background,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    AeonHazeProvider(backgroundColor = containerColor) { _ ->
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                AeonTopAppBar(
                    left = AeonTopBarSection(
                        content = {
                            AeonTopBarNavTitlePill(
                                title = title,
                                onBackClick = onBack
                            )
                        }
                    ),
                    right = AeonTopBarSection(
                        content = {
                            AeonTopBarActionPill(actions = actions)
                        }
                    )
                )
            },
            bottomBar = {
                Box {
                    bottomBar()
                }
            },
            contentWindowInsets = WindowInsets.systemBars
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize()) {
                content(
                    PaddingValues(
                        top = padding.calculateTopPadding() + 8.dp,
                        bottom = padding.calculateBottomPadding() + 24.dp,
                        start = 0.dp,
                        end = 0.dp
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AeonSelectionScreenPreview() {
    OssWalletTheme {
        AeonSelectionScreen(
            title = "Select Category",
            onBack = {}
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                Text("Content goes here")
            }
        }
    }
}
