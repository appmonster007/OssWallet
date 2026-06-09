package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.components.AeonHazeProvider
import com.osscabinet.ossWallet.ui.components.AeonTopAppBar
import com.osscabinet.ossWallet.ui.components.AeonTopBarActionPill
import com.osscabinet.ossWallet.ui.components.AeonTopBarNavTitlePill
import com.osscabinet.ossWallet.ui.components.AeonTopBarSection
import com.osscabinet.ossWallet.ui.components.AppBarIconButton
import com.osscabinet.ossWallet.ui.theme.AeonSize
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.scaledDp

/**
 * Standardized Scaffold for all Entry/Creation screens in OssWallet.
 * Following the Aeon "Floating Pill" glass architecture.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AeonEntryScaffold(
    title: String,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    canSave: Boolean,
    modifier: Modifier = Modifier,
    isEditMode: Boolean = false,
    onDeleteClick: (() -> Unit)? = null,
    onResetClick: (() -> Unit)? = null,
    containerColor: Color = AppColors.background,
    content: @Composable (PaddingValues) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val uiScale = AeonTheme.uiScale
    val topBarHeight = AeonSize.D64.value
    AeonHazeProvider(backgroundColor = containerColor) { hazeLayers ->
        Box(modifier = modifier.fillMaxSize()) {
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0)
            ) { innerPadding ->
                Box(modifier = Modifier.fillMaxSize()) {
                    content(
                        PaddingValues(
                            top = topBarHeight + scaledDp(8f, uiScale),
                            bottom = innerPadding.calculateBottomPadding() + scaledDp(24f, uiScale),
                            start = 0.dp,
                            end = 0.dp
                        )
                    )
                }
            }

            AeonTopAppBar(
                left = AeonTopBarSection(
                    content = {
                        AeonTopBarNavTitlePill(
                            title = title,
                            onBackClick = onBackClick
                        )
                    }
                ),
                right = AeonTopBarSection(
                    content = {
                        AeonTopBarActionPill {
                            if (onResetClick != null) {
                                AppBarIconButton(
                                    icon = Icons.Rounded.RestartAlt,
                                    onClick = onResetClick
                                )
                            }
                            if (isEditMode && onDeleteClick != null) {
                                AppBarIconButton(
                                    icon = Icons.Rounded.Delete,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onDeleteClick()
                                    },
                                    tint = AppColors.error
                                )
                            }
                            AppBarIconButton(
                                icon = Icons.Rounded.Done,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSaveClick()
                                },
                                enabled = canSave,
                                tint = AppColors.primary
                            )
                        }
                    }
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AeonEntryScaffoldPreview() {
    OssWalletTheme {
        AeonEntryScaffold(
            title = "New Transaction",
            onBackClick = {},
            onSaveClick = {},
            canSave = true
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                Text("Content goes here")
            }
        }
    }
}
