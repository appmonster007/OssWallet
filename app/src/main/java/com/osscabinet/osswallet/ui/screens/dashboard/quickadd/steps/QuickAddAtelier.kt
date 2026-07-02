package com.osscabinet.ossWallet.ui.screens.dashboard.quickadd.steps

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.ui.components.aeonHairlineBorder
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

/** Shared 4/5-column tile grid used by the QuickAdd Atelier steps. */
@Composable
internal fun AtelierGrid(
    numColumns: Int,
    modifier: Modifier = Modifier,
    content: LazyGridScope.() -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(numColumns),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
//        contentPadding = PaddingValues(bottom = 2.dp),
        modifier = modifier.fillMaxWidth(),
        content = content
    )
}

/** Mono overline with a fading hairline rule — the panel section header. */
@Composable
internal fun PanelOverline(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = text,
            style = AppTextStyles.sectionOverline,
            color = AppColors.accent
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(0.5.dp)
                .background(AppColors.outlineVariant.copy(alpha = 0.6f))
        )
    }
}

@Composable
internal fun EmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            message,
            style = AppTextStyles.listItemSubtitle,
            color = AppColors.contentSecondary.copy(alpha = 0.6f)
        )
    }
}

/**
 * Square Atelier tile — thin icon in [iconColor] over a recessed, hairline-bordered
 * surface with an Inter label below. Selected tiles take a [selectionColor] border,
 * a faint tint, and a check badge. An optional [hasChildren] chevron hints drill-down.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AtelierTile(
    label: String,
    icon: ImageVector,
    iconColor: Color,
    isSelected: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    selectionColor: Color = AppColors.accent,
    selectionContentColor: Color = AppColors.onAccent,
    selectionLabel: String? = null,
    hasChildren: Boolean = false,
    onLongPress: (() -> Unit)? = null
) {
    val tileShape = RoundedCornerShape(10.dp)
    val bg = if (isSelected) selectionColor.copy(alpha = 0.10f) else AppColors.recessedSurface
    val fontSize = 8.sp
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(tileShape)
            .background(bg)
            .then(
                if (isSelected) Modifier.selectedBorder(tileShape, selectionColor)
                else Modifier.aeonHairlineBorder(tileShape)
            )
            .combinedClickable(onClick = onTap, onLongClick = onLongPress),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = AppTextStyles.chartAxisLabel.copy(fontSize = fontSize),
                color = AppColors.contentPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                lineHeight = fontSize
            )
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .then(
                        if (selectionLabel != null) Modifier.defaultMinSize(minWidth = 15.dp)
                        else Modifier.size(15.dp)
                    )
                    .height(15.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(selectionColor)
                    .padding(horizontal = if (selectionLabel != null) 4.dp else 0.dp),
                contentAlignment = Alignment.Center
            ) {
                if (selectionLabel != null) {
                    Text(
                        text = selectionLabel,
                        style = AppTextStyles.sectionOverline.copy(
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = selectionContentColor
                        )
                    )
                } else {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        tint = selectionContentColor,
                        modifier = Modifier.size(9.dp)
                    )
                }
            }
        } else if (hasChildren) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(AppColors.contentSecondary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "Long press for sub-categories",
                    tint = AppColors.contentSecondary,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}

@Composable
private fun Modifier.selectedBorder(shape: Shape, color: Color): Modifier =
    this.border(1.dp, color, shape)

/**
 * Two-tab segmented toggle with a sliding indicator. Used as the editorial
 * mode-switch on QuickAdd Atelier steps (Templates/Categories, Expense/Income).
 * [activeColor] tints the indicator pill so semantic steps can pass a type color.
 */
internal data class AtelierSegment(val label: String, val icon: ImageVector)

@Composable
internal fun AtelierSegmentedToggle(
    segments: List<AtelierSegment>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = AppColors.accent,
    activeContentColor: Color = AppColors.onAccent,
    height: Dp = 40.dp,
) {
    require(segments.size == 2) { "AtelierSegmentedToggle expects exactly 2 segments" }
    val outerShape = RoundedCornerShape(12.dp)
    val pillShape = RoundedCornerShape(9.dp)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(outerShape)
            .background(AppColors.recessedSurface)
            .aeonHairlineBorder(outerShape)
            .padding(3.dp)
    ) {
        val itemWidth = maxWidth / segments.size

        // Sliding indicator
        Box(
            modifier = Modifier
                .width(itemWidth)
                .fillMaxHeight()
                .graphicsLayer { translationX = selectedIndex * itemWidth.toPx() }
                .clip(pillShape)
                .background(activeColor)
        )

        Row(modifier = Modifier.fillMaxSize()) {
            segments.forEachIndexed { index, seg ->
                val isSelected = index == selectedIndex
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) activeContentColor else AppColors.contentSecondary,
                    label = "segTabColor"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(pillShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { if (!isSelected) onSelect(index) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Icon(
                            imageVector = seg.icon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = seg.label,
                            style = AppTextStyles.sectionOverline,
                            color = contentColor
                        )
                    }
                }
            }
        }
    }
}

/**
 * The editorial "panel" container — a recessed glass surface with a gradient
 * hairline border that hosts the grid and its overline. Hooks into the sheet's
 * AeonGlassHost so the panel blurs the contour pixels behind the sheet.
 *
 * Pass [modifier] carrying `weight(1f, fill = false)` so the panel flexes
 * between the pinned header and footer.
 */
@Composable
internal fun AtelierPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
    spacing: Dp = 8.dp,
    header: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.recessedSurface)
            .aeonHairlineBorder(shape)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            header()
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewAtelierTile() {
    OssWalletTheme {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AtelierTile(
                    label = "Coffee",
                    icon = Icons.Rounded.Coffee,
                    iconColor = Color(0xFF795548),
                    isSelected = false,
                    onTap = {},
                    modifier = Modifier.width(80.dp)
                )
                AtelierTile(
                    label = "Restaurant & Fine Dining",
                    icon = Icons.Rounded.Restaurant,
                    iconColor = Color(0xFFFF5722),
                    isSelected = true,
                    onTap = {},
                    modifier = Modifier.width(80.dp)
                )
                AtelierTile(
                    label = "Savings",
                    icon = Icons.Rounded.AccountBalance,
                    iconColor = Color(0xFF2196F3),
                    isSelected = false,
                    hasChildren = true,
                    onTap = {},
                    modifier = Modifier.width(80.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewAtelierPanel() {
    OssWalletTheme {
        Box(modifier = Modifier.padding(20.dp)) {
            AtelierPanel(
                header = { PanelOverline("FREQUENT") }
            ) {
                AtelierGrid(numColumns = 4) {
                    items(8) { index ->
                        AtelierTile(
                            label = "Item $index",
                            icon = Icons.Rounded.Coffee,
                            iconColor = AppColors.accent,
                            isSelected = index == 2,
                            onTap = {}
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewAtelierTileFontSizes() {
    OssWalletTheme {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Short label
            AtelierTile(
                label = "Coffee",
                icon = Icons.Rounded.Coffee,
                iconColor = AppColors.accent,
                isSelected = false,
                onTap = {},
                modifier = Modifier.width(72.dp)
            )
            // Long label (testing 8.sp wrapping)
            AtelierTile(
                label = "Restaurant & Fine Dining",
                icon = Icons.Rounded.Restaurant,
                iconColor = AppColors.accent,
                isSelected = false,
                onTap = {},
                modifier = Modifier.width(72.dp)
            )
            // Very long label with ellipsis
            AtelierTile(
                label = "International Wire Transfer Fees",
                icon = Icons.Rounded.AccountBalance,
                iconColor = AppColors.accent,
                isSelected = false,
                onTap = {},
                modifier = Modifier.width(72.dp)
            )
        }
    }
}
