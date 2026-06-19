package com.osscabinet.ossWallet.ui.components.cards

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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import com.osscabinet.ossWallet.domain.model.settings.ChartMetric
import com.osscabinet.ossWallet.domain.model.settings.AccountingPerspective
import com.osscabinet.ossWallet.ui.components.AeonGlassSheet
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
            onDismiss = onDismiss,
            modifier = modifier
        ) {
            SheetBody(title, onDismiss, onDelete, content)
        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardPerspectiveSettings(
    perspective: AccountingPerspective,
    onPerspectiveChange: (AccountingPerspective) -> Unit
) {
    CardSettingsSection("Perspective") {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            AccountingPerspective.ALL.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = perspective::class == mode::class,
                    onClick = { onPerspectiveChange(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                    label = {
                        Text(
                            if (mode is AccountingPerspective.Accounting) "Accounting" else "Cash Flow",
                            style = AppTextStyles.caption.copy(fontSize = 11.sp)
                        )
                    }
                )
            }
        }
        val cashFlow = perspective as? AccountingPerspective.CashFlow
        if (cashFlow != null) {
            com.osscabinet.ossWallet.ui.components.AeonSettingsToggle(
                label = "Exclude internal transfers",
                description = "Hides transfers between accounts within the selected scope",
                checked = cashFlow.excludeInternalTransfers,
                onCheckedChange = { onPerspectiveChange(AccountingPerspective.CashFlow(it)) }
            )
        }
    }
}

/**
 * Shared metric selector for various chart cards.
 * Standardizes the toggle row across all 10 cards.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AeonMetricToggle(
    perspective: AccountingPerspective,
    selectedMetric: ChartMetric,
    onMetricChange: (ChartMetric) -> Unit,
    modifier: Modifier = Modifier
) {
    val labels = AccountingPerspective.labelsFor(perspective)
    // Filter out metrics not supported by the current perspective (e.g., TRANSFER in Cash Flow)
    val options = ChartMetric.entries.filter { it in labels }

    SingleChoiceSegmentedButtonRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        options.forEachIndexed { index, metric ->
            val label = labels[metric] ?: metric.name
            SegmentedButton(
                selected = selectedMetric == metric,
                onClick = { onMetricChange(metric) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = {
                    Text(
                        text = label,
                        style = AppTextStyles.caption.copy(fontSize = 11.sp)
                    )
                }
            )
        }
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
        Surface{
            CardSettingsSheet(
                title = "Card Settings",
                onDismiss = {},
                onDelete = {}
            ) {
                CardPerspectiveSettings(
                    perspective = AccountingPerspective.Accounting,
                    onPerspectiveChange = {}
                )
            }
        }
    }
}
