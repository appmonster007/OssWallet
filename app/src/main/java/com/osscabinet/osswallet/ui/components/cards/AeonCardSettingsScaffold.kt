package com.osscabinet.ossWallet.ui.components.cards

import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.CompositionLocalProvider
import com.osscabinet.ossWallet.domain.model.settings.BarChartViewMode
import com.osscabinet.ossWallet.domain.model.settings.BarChartGranularity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.settings.AccountingPerspective
import com.osscabinet.ossWallet.domain.model.settings.ChartMetric
import com.osscabinet.ossWallet.ui.components.AeonGridColumnsSettings
import com.osscabinet.ossWallet.ui.components.AeonLayoutToggle
import com.osscabinet.ossWallet.ui.components.AeonSettingsToggle
import com.osscabinet.ossWallet.ui.components.aeonHairlineBorder
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.GlassTokens
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

/**
 * The unified scaffold for card configuration — renders on the back side of a flipped card.
 * Uses the shared AeonCardHeader for exact structural parity with the front side.
 */
@Composable
fun AeonCardSettingsScaffold(
    title: String,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm),
    ) {
        AeonCardHeader(
            title = title,
            subtitle = "CONFIGURATION",
            icon = Icons.Default.ChevronLeft,
            onIconClick = onDismiss,
            trailingIcon = if (onDelete != null) Icons.Default.Delete else null,
            onTrailingIconClick = onDelete,
        )

        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardPerspectiveSettings(
    perspective: AccountingPerspective,
    onPerspectiveChange: (AccountingPerspective) -> Unit,
    excludeInternalTransfers: Boolean = true,
    onExcludeInternalTransfersChange: (Boolean) -> Unit = {},
    splitTransfers: Boolean = false,
    onSplitTransfersChange: (Boolean) -> Unit = {},
) {
    CardSettingsSection("Perspective") {
        val allPerspectives = AccountingPerspective.ALL
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            allPerspectives.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = perspective == mode,
                    onClick = { onPerspectiveChange(mode) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = allPerspectives.size
                    ),
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

        if (perspective is AccountingPerspective.Accounting) {
            AeonSettingsToggle(
                label = "Split transfers",
                description = "Show transfer in and out separately instead of a net value",
                checked = splitTransfers,
                onCheckedChange = onSplitTransfersChange,
                enabled = true,
                showBackground = false,
                showBorder = false
            )
        }

        AeonSettingsToggle(
            label = "Exclude internal transfers",
            description = "Hides transfers between accounts",
            checked = excludeInternalTransfers,
            onCheckedChange = onExcludeInternalTransfersChange,
            enabled = if (perspective is AccountingPerspective.Accounting) splitTransfers else true,
            showBackground = false,
            showBorder = false
        )

    }
}

/**
 * Shared metric selector for various chart cards.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AeonMetricToggle(
    perspective: AccountingPerspective,
    selectedMetric: ChartMetric,
    onMetricChange: (ChartMetric) -> Unit,
    modifier: Modifier = Modifier,
    splitTransfers: Boolean = false
) {
    val labels = AccountingPerspective.labelsFor(perspective, splitTransfers)
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
    val shape = RoundedCornerShape(AeonTheme.dimensions.containerCornerRadius)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (showBackground) {
                    Modifier
                        .background(AppColors.cardSurfaceProminent, shape)
                        .aeonHairlineBorder(shape)
                } else Modifier
            )
    ) {
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
            AeonCardSettingsScaffold(
                title = "Standardization Lab",
                onDismiss = {},
                onDelete = {},
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                CardPerspectiveSettings(
                    perspective = perspective,
                    onPerspectiveChange = { perspective = it }
                )

                CardSettingsSection("Metric Selection") {
                    AeonMetricToggle(
                        perspective = perspective,
                        selectedMetric = metric,
                        onMetricChange = { metric = it }
                    )
                }

                CardSettingsSection("Layout & Visuals", showBackground = false) {
                    AeonLayoutToggle(
                        layout = layout,
                        onLayoutChange = { layout = it },
                        showBackground = false
                    )
                    AeonGridColumnsSettings(
                        gridColumns = columns,
                        onGridColumnsChange = { columns = it },
                        showBackground = false
                    )
                }

                CardSettingsSection("Display Flags") {
                    AeonSettingsToggle(
                        label = "Show Comparison",
                        description = "Show change from previous period",
                        checked = true,
                        onCheckedChange = { },
                        showBackground = false
                    )
                }

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
                        Text(
                            "Manage Connected Entities",
                            style = AppTextStyles.buttonLabel.copy(fontSize = 13.sp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Chart Mode (fixed scale / scrollable) plus, in scrollable mode, the bucket granularity.
 * Shared by the bar chart cards so both settings screens stay identical.
 */
@Composable
fun BarChartModeSettings(
    viewMode: BarChartViewMode,
    onViewModeChange: (BarChartViewMode) -> Unit,
    granularity: BarChartGranularity,
    onGranularityChange: (BarChartGranularity) -> Unit,
) {
    CardSettingsSection("Chart") {
        CardSettingsSegmentedRow(
            options = BarChartViewMode.entries,
            selected = viewMode,
            onSelected = onViewModeChange,
            labelProvider = { it.displayName }
        )
        // Always shown so the setting is discoverable; greyed out while the fixed scale picks buckets itself.
        val scrollable = viewMode == BarChartViewMode.SCROLLABLE
        Text(
            text = "GRANULARITY",
            style = AppTextStyles.overlineLabel.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                fontSize = 10.sp
            ),
            color = AppColors.primary.copy(alpha = if (scrollable) 1f else 0.4f),
            modifier = Modifier.padding(top = 4.dp)
        )
        CardSettingsChipGrid(
            options = BarChartGranularity.entries,
            selected = granularity,
            onSelected = onGranularityChange,
            labelProvider = { it.displayName },
            columns = 3,
            enabled = scrollable
        )
    }
}

/** Segmented row styled like the Perspective selector; for two or three short options. */
@Composable
fun <T> CardSettingsSegmentedRow(
    options: List<T>,
    selected: T,
    onSelected: (T) -> Unit,
    labelProvider: (T) -> String,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = selected == option,
                onClick = { onSelected(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = {
                    Text(
                        text = labelProvider(option),
                        style = AppTextStyles.caption.copy(
                            fontSize = 11.sp,
                            fontWeight = if (selected == option) FontWeight.Bold else FontWeight.Normal
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

/**
 * Single-choice chips laid out in equal-width rows of [columns]; for options too many for a
 * segmented row. When [enabled] is false the grid is dimmed and ignores taps.
 */
@Composable
fun <T> CardSettingsChipGrid(
    options: List<T>,
    selected: T,
    onSelected: (T) -> Unit,
    labelProvider: (T) -> String,
    modifier: Modifier = Modifier,
    columns: Int = 3,
    enabled: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.38f),
        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)
    ) {
        options.chunked(columns).forEach { rowOptions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)
            ) {
                rowOptions.forEach { option ->
                    val isSelected = selected == option
                    // Drop the 48dp touch-target padding so the row gap is spacingSm, like the segmented rows.
                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelected(option) },
                            enabled = enabled,
                            modifier = Modifier.weight(1f),
                            label = {
                                Text(
                                    text = labelProvider(option),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    style = AppTextStyles.caption.copy(
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.Transparent,
                                labelColor = AppColors.contentSecondary,
                                selectedContainerColor = AppColors.primary.copy(alpha = 0.1f),
                                selectedLabelColor = AppColors.primary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = enabled,
                                selected = isSelected,
                                borderColor = AppColors.outlineVariant,
                                selectedBorderColor = AppColors.primary.copy(alpha = 0.2f)
                            )
                        )
                    }
                }
                // Keep the last row's chips the same width as the full rows.
                repeat(columns - rowOptions.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}
