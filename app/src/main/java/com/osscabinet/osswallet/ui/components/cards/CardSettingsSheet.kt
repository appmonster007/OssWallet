package com.osscabinet.ossWallet.ui.components.cards

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.osscabinet.ossWallet.ui.components.GlassSurface
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.GlassTokens
import com.osscabinet.ossWallet.ui.theme.HazeLayer
import com.osscabinet.ossWallet.ui.theme.LocalGlassmorphismEnabled
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

/**
 * Shared scaffold for all card settings sheets.
 * Automatically selects between a standard ModalBottomSheet and a custom AeonGlassSheet
 * based on whether glassmorphism is enabled.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardSettingsSheet(
    title: String,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit
) {
    val glassEnabled = LocalGlassmorphismEnabled.current

    if (glassEnabled) {
        AeonGlassSheet(
            title = title,
            onDismiss = onDismiss,
            onDelete = onDelete,
            modifier = modifier,
            content = content
        )
    } else {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = AppColors.cardSurfaceProminent,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            contentWindowInsets = { WindowInsets.navigationBars },
            modifier = modifier.statusBarsPadding()
        ) {
            SheetBody(title, onDismiss, onDelete, content)
        }
    }
}

/**
 * A custom bottom-sheet-like overlay that supports real glassmorphism by staying
 * within the same window hierarchy.
 */
@Composable
fun AeonGlassSheet(
    title: String,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true, dismissOnClickOutside = true)
    ) {
        var visible by remember { mutableStateOf(false) }
        SideEffect { visible = true }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )

            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = modifier
            ) {
                GlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    hazeLayer = HazeLayer.Overlay,
                    hazeStyle = GlassTokens.sheetStyle(),
                    fallbackColor = AppColors.sheetSurface,
                    borderColor = Color.White.copy(alpha = 0.2f),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Drag Handle
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 32.dp, height = 4.dp)
                                    .background(AppColors.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                            )
                        }
                        SheetBody(title, onDismiss, onDelete, content)
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetBody(
    title: String,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val navPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = AppTextStyles.heroAmount.copy(fontSize = 24.sp),
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { onDelete(); onDismiss() },
                colors = IconButtonDefaults.iconButtonColors(contentColor = AppColors.error)
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete card")
            }
        }

        content()

        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AeonTheme.dimensions.buttonCornerRadius)
        ) {
            Text("Done", style = AppTextStyles.buttonLabel)
        }

        Spacer(modifier = Modifier.height(navPadding + 24.dp))
    }
}

@Composable
fun CardSettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = AppColors.insetSurface.copy(alpha = 0.4f),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Text(
                text = title.uppercase(),
                style = AppTextStyles.transactionSwipeAction.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp,
                    fontSize = 11.sp
                ),
                color = AppColors.contentSecondary.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp)
            )
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun CardSettingsSheetPreview() {
    OssWalletTheme {
        CardSettingsSheet(
            title = "Preview Card",
            onDismiss = {},
            onDelete = {}
        ) {
            CardSettingsSection("Section 1") {
                Text("Content 1", color = AppColors.contentPrimary)
            }
        }
    }
}
