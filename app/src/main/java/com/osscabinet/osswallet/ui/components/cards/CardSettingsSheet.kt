package com.osscabinet.ossWallet.ui.components.cards

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindowProvider
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.LocalGlassmorphismEnabled
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

/**
 * Shared scaffold for all card settings bottom sheets.
 *
 * Provides the ModalBottomSheet wrapper, drag handle, header row (title + delete button),
 * a scrollable content slot, and a Done button with correct edge-to-edge bottom padding.
 * The sheet expands up to the status bar via [Modifier.statusBarsPadding] on the sheet modifier.
 *
 * Usage:
 * ```
 * CardSettingsSheet(title = "My Card", onDismiss = onDismiss, onDelete = onDelete) {
 *     CardSettingsSection("Display") { ... }
 * }
 * ```
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
    val glassActive = glassEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (glassActive) AppColors.cardSurfaceProminent.copy(alpha = 0.5f)
                         else AppColors.cardSurfaceProminent,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        contentWindowInsets = { WindowInsets.navigationBars },
        modifier = modifier.statusBarsPadding()
    ) {
        val view = LocalView.current
        SideEffect {
            if (glassActive) {
                (view.parent as? DialogWindowProvider)?.window?.setBackgroundBlurRadius(12)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 4.dp, bottom = 24.dp),
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
                    style = AppTextStyles.heroAmount,
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
                shape = RoundedCornerShape(16.dp)
            ) { Text("Done") }
        }
    }
}

/**
 * A labeled surface section for grouping related settings inside a [CardSettingsSheet].
 *
 * Usage:
 * ```
 * CardSettingsSection("Display") {
 *     SwitchRow(...)
 *     SwitchRow(...)
 * }
 * ```
 */
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
            CardSettingsSection("Section 2") {
                Text("Content 2", color = AppColors.contentPrimary)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CardSettingsSectionPreview() {
    OssWalletTheme {
        Surface(color = AppColors.background, modifier = Modifier.padding(16.dp)) {
            CardSettingsSection("Display Options") {
                Text("Example Setting", color = AppColors.contentPrimary)
            }
        }
    }
}
