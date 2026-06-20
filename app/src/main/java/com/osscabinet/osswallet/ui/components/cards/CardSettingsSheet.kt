package com.osscabinet.ossWallet.ui.components.cards

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.width
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.osscabinet.ossWallet.ui.components.AeonGridColumnsSettings
import com.osscabinet.ossWallet.ui.components.AeonLayoutToggle
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import com.osscabinet.ossWallet.domain.model.settings.ChartMetric
import com.osscabinet.ossWallet.domain.model.settings.AccountingPerspective
import com.osscabinet.ossWallet.ui.components.AeonGlassSheet
import com.osscabinet.ossWallet.ui.components.AeonSettingsToggle
import com.osscabinet.ossWallet.ui.components.GlassSurface
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.GlassTokens
import com.osscabinet.ossWallet.ui.theme.HazeLayer
import com.osscabinet.ossWallet.ui.theme.LocalGlassmorphismEnabled
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

/**
 * Shared scaffold for all card settings.
 * If used within an AeonExpressiveCard "Flip", it renders as an inline panel.
 * Otherwise, it defaults to a BottomSheet.
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
    val isInline = !LocalGlassmorphismEnabled.current

    if (isInline) {
        AeonExpressiveCardConfig(title, onDismiss, onDelete, modifier, content)
    } else {
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
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg)
    ) {
        AeonExpressiveCardConfig(
            title = title,
            onDismiss = onDismiss,
            onDelete = onDelete,
        ) {
            content()
        }

        Spacer(modifier = Modifier.height(navPadding + 16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardPerspectiveSettings(
    perspective: AccountingPerspective,
    onPerspectiveChange: (AccountingPerspective) -> Unit,
    excludeInternalTransfers: Boolean = true,
    onExcludeInternalTransfersChange: (Boolean) -> Unit = {}
) {
    CardSettingsSection("Perspective") {
        val allPerspectives = AccountingPerspective.ALL
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            allPerspectives.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = perspective == mode,
                    onClick = { onPerspectiveChange(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = allPerspectives.size),
                    label = {
                        Text(
                            text = when (mode) {
                                is AccountingPerspective.Accounting -> "Accounting"
                                is AccountingPerspective.CashFlow -> "Cash Flow"
                            },
                            style = AppTextStyles.caption.copy(
                                fontSize = 11.sp,
                                fontWeight = if (perspective == mode) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    },
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = AppColors.primary.copy(alpha = 0.1f),
                        activeContentColor = AppColors.primary,
                        inactiveContainerColor = Color.Transparent,
                        inactiveContentColor = AppColors.contentSecondary,
                        activeBorderColor = AppColors.primary.copy(alpha = 0.2f),
                        inactiveBorderColor = AppColors.outlineVariant
                    )
                )
            }
        }
        
        AeonSettingsToggle(
            label = "Exclude internal transfers",
            description = "Hides transfers between accounts",
            checked = excludeInternalTransfers && perspective is AccountingPerspective.CashFlow,
            onCheckedChange = onExcludeInternalTransfersChange,
            enabled = perspective is AccountingPerspective.CashFlow,
            showBackground = false,
            showBorder = false
        )
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
    val options = ChartMetric.entries.filter { it in labels }

    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, metric ->
            val label = labels[metric] ?: metric.name
            SegmentedButton(
                selected = selectedMetric == metric,
                onClick = { onMetricChange(metric) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = {
                    Text(
                        text = label,
                        style = AppTextStyles.caption.copy(
                            fontSize = 11.sp,
                            fontWeight = if (selectedMetric == metric) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                },
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = AppColors.primary.copy(alpha = 0.1f),
                    activeContentColor = AppColors.primary,
                    inactiveContainerColor = Color.Transparent,
                    inactiveContentColor = AppColors.contentSecondary,
                    activeBorderColor = AppColors.primary.copy(alpha = 0.2f),
                    inactiveBorderColor = AppColors.outlineVariant
                )
            )
        }
    }
}

@Composable
fun CardSettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    showBackground: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val sectionContent = @Composable {
        Column(
            modifier = Modifier.padding(vertical = if (showBackground) 16.dp else 8.dp),
            verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)
        ) {
            Text(
                text = title.uppercase(),
                style = AppTextStyles.overlineLabel.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.4.sp,
                    fontSize = 10.sp
                ),
                color = AppColors.contentSecondary.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)
            ) {
                content()
            }
        }
    }

    if (showBackground) {
        Surface(
            color = AppColors.surfaceContainerLow.copy(alpha = 0.7f),
            shape = RoundedCornerShape(AeonTheme.dimensions.containerCornerRadius),
            modifier = modifier.fillMaxWidth(),
            border = BorderStroke(0.5.dp, AppColors.outlineVariant.copy(alpha = 0.3f))
        ) {
            sectionContent()
        }
    } else {
        Box(modifier = modifier.fillMaxWidth()) {
            sectionContent()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Standardization Lab", heightDp = 1200)
@Composable
fun CardSettingsStandardizationLab() {
    var perspective by remember { mutableStateOf<AccountingPerspective>(AccountingPerspective.Accounting) }
    var metric by remember { mutableStateOf(ChartMetric.NET) }
    var layout by remember { mutableStateOf("grid") }
    var columns by remember { mutableIntStateOf(2) }

    OssWalletTheme {
        Surface(
            color = AppColors.background,
        ) {
            AeonExpressiveCardConfig(
                title = "Standardization Lab",
                onDismiss = {},
                onDelete = {},
                modifier = Modifier.fillMaxWidth()
            ) {
                // 1. Perspective Section (Complex Group)
                CardPerspectiveSettings(
                    perspective = perspective,
                    onPerspectiveChange = { perspective = it }
                )

                // 2. Metrics Section (Dynamic Segmented)
                CardSettingsSection("Metric Selection") {
                    AeonMetricToggle(
                        perspective = perspective,
                        selectedMetric = metric,
                        onMetricChange = { metric = it }
                    )
                }

                // 3. Layout & Visuals (Using showBackground = false for components with their own surfaces)
                CardSettingsSection("Layout & Visuals", showBackground = false) {
                    AeonLayoutToggle(layout = layout, onLayoutChange = { layout = it }, showBackground = false)
                    AeonGridColumnsSettings(gridColumns = columns, onGridColumnsChange = { columns = it }, showBackground = false)
                }

                // 4. Display Flags (Unified Boolean Controls)
                CardSettingsSection("Display Flags") {
                    AeonSettingsToggle(
                        label = "Show Comparison",
                        description = "Show change from previous period",
                        checked = true,
                        onCheckedChange = { },
                        showBackground = false
                    )
                    AeonSettingsToggle(
                        label = "Show Labels",
                        checked = false,
                        onCheckedChange = { },
                        showBackground = false
                    )
                }

                // 5. General Configuration (Standard Pickers)
                CardSettingsSection("General") {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        listOf("S", "M", "L", "XL").forEachIndexed { index, size ->
                            SegmentedButton(
                                selected = index == 1,
                                onClick = { },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = 4),
                                label = {
                                    Text(
                                        size,
                                        style = AppTextStyles.caption.copy(
                                            fontWeight = if (index == 1) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                },
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = AppColors.primary.copy(alpha = 0.1f),
                                    activeContentColor = AppColors.primary,
                                    inactiveContainerColor = Color.Transparent,
                                    inactiveContentColor = AppColors.contentSecondary,
                                    activeBorderColor = AppColors.primary.copy(alpha = 0.2f),
                                    inactiveBorderColor = AppColors.outlineVariant
                                )
                            )
                        }
                    }
                }

                // 6. Actions (Outlined variant)
                CardSettingsSection("Actions", showBackground = false) {
                    OutlinedButton(
                        onClick = { },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AppColors.outlineVariant)
                    ) {
                        Icon(Icons.Default.Settings, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Manage Connected Entities", style = AppTextStyles.buttonLabel.copy(fontSize = 13.sp))
                    }
                }
            }
        }
    }
}
