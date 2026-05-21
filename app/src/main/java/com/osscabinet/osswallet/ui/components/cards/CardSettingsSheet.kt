package com.osscabinet.ossWallet.ui.components.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

/**
 * Shared scaffold for all card settings bottom sheets.
 *
 * Provides the ModalBottomSheet wrapper, drag handle, header row (title + delete button),
 * a content slot, and a Done button with correct edge-to-edge bottom padding.
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
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.cardSurfaceProminent,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = (navBarPadding + 16.dp).coerceAtLeast(32.dp)),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = AppTextStyles.heroAmount)
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
                shape = RoundedCornerShape(12.dp)
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
        color = AppColors.insetSurface.copy(alpha = 0.3f),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title.uppercase(),
                style = AppTextStyles.transactionSwipeAction.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    fontSize = 11.sp
                ),
                color = AppColors.contentSecondary.copy(alpha = 0.6f)
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
