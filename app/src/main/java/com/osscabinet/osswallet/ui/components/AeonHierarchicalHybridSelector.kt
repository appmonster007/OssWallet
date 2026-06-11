package com.osscabinet.ossWallet.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AirplanemodeActive
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Dining
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.ShoppingBasket
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.GlassTokens
import com.osscabinet.ossWallet.ui.theme.GlassVariant
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.util.isDark

/**
 * Collects all descendant IDs of [rootId] by walking [allItems] with the given mappers.
 * Used to cascade select/deselect children when a parent is toggled in MULTI mode.
 */
private fun <T, ID> collectDescendantIds(
    rootId: ID,
    allItems: List<T>,
    idMapper: (T) -> ID,
    parentIdMapper: (T) -> ID?
): Set<ID> {
    val result = mutableSetOf<ID>()
    val queue = ArrayDeque<ID>()
    allItems.filter { parentIdMapper(it) == rootId }.forEach { queue.add(idMapper(it)) }
    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        if (result.add(current)) {
            allItems.filter { parentIdMapper(it) == current }.forEach { queue.add(idMapper(it)) }
        }
    }
    return result
}

/**
 * A unified hierarchical selector that supports single-select, multi-select, parent-selection, and management modes.
 * Implements a "layered" hierarchical design with left-aligned expansion controls.
 * Generic version that works with any data type and any ID type.
 *
 * @param T The type of items in the list
 * @param ID The type of item IDs (typically Long or String)
 */
@Composable
fun <T, ID> AeonHierarchicalHybridSelector(
    items: List<T>,
    mode: AeonSelectMode,
    selectedIds: Set<ID>,
    onSelectionChange: (Set<ID>) -> Unit,
    idMapper: (T) -> ID,
    labelMapper: (T) -> String,
    parentIdMapper: (T) -> ID?,
    modifier: Modifier = Modifier,
    iconMapper: (T) -> ImageVector? = { null },
    colorMapper: (T) -> Color? = { null },
    noneLabel: String? = null,
    noneIcon: ImageVector = Icons.Rounded.Block,
    onParentSelected: (ID?) -> Unit = {},
    onEditClick: (T) -> Unit = {},
    onAddChildClick: (T) -> Unit = {},
    startExpanded: Boolean = true
) {
    val rootItems = remember(items, parentIdMapper, labelMapper) {
        items.filter { parentIdMapper(it) == null }.sortedBy { labelMapper(it) }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val dims = AeonTheme.dimensions
        val haptic = LocalHapticFeedback.current

        // Special "None" option for Parent Selection mode
        if ((mode == AeonSelectMode.PARENT) && (noneLabel != null)) {
            val isNoneSelected = selectedIds.isEmpty()
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "noneScale")

            val backgroundColor by animateColorAsState(
                if (isNoneSelected) AppColors.primary.copy(alpha = 0.15f)
                else AppColors.surfaceContainerLow,
                label = "noneBgColor"
            )
            val borderColor by animateColorAsState(
                if (isNoneSelected) AppColors.primary
                else AppColors.outlineVariant.copy(alpha = 0.6f),
                label = "noneBorderColor"
            )
            val borderWidth by animateDpAsState(
                if (isNoneSelected) 1.5.dp else 1.dp,
                label = "noneBorderWidth"
            )

            GlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(scale)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onParentSelected(null)
                    },
                shape = RoundedCornerShape(dims.componentCornerRadius),
                hazeStyle = GlassTokens.itemStyle(),
                glassVariant = GlassVariant.Item,
                fallbackColor = backgroundColor,
                borderColor = borderColor,
                borderWidth = borderWidth,
                shadowElevation = if (isNoneSelected) 2.dp else 0.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 72.dp)
                        .padding(horizontal = dims.cardPadding, vertical = dims.spacingMd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(24.dp))
                    Surface(
                        modifier = Modifier.size(dims.iconSizeLg),
                        shape = CircleShape,
                        color = AppColors.surfaceContainerHigh.copy(alpha = if (isNoneSelected) 0.25f else 0.12f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                noneIcon,
                                contentDescription = null,
                                modifier = Modifier.size(dims.iconSizeMd),
                                tint = if (isNoneSelected) AppColors.primary else AppColors.contentSecondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(dims.spacingLg))
                    Text(
                        noneLabel,
                        style = AppTextStyles.listItemTitle,
                        color = if (isNoneSelected) AppColors.primary else AppColors.contentPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    if (isNoneSelected) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = "Selected",
                            tint = AppColors.primary,
                            modifier = Modifier.size(dims.iconSizeMd)
                        )
                    }
                }
            }
        }

        rootItems.forEach { root ->
            key(idMapper(root)) {
                RecursiveHybridNode(
                    item = root,
                    allItems = items,
                    mode = mode,
                    selectedIds = selectedIds,
                    onSelectionChange = onSelectionChange,
                    idMapper = idMapper,
                    labelMapper = labelMapper,
                    parentIdMapper = parentIdMapper,
                    iconMapper = iconMapper,
                    colorMapper = colorMapper,
                    onParentSelected = onParentSelected,
                    onEditClick = onEditClick,
                    onAddChildClick = onAddChildClick,
                    depth = 0,
                    startExpanded = startExpanded
                )
            }
        }
    }
}

@Composable
private fun <T, ID> RecursiveHybridNode(
    item: T,
    allItems: List<T>,
    mode: AeonSelectMode,
    selectedIds: Set<ID>,
    onSelectionChange: (Set<ID>) -> Unit,
    idMapper: (T) -> ID,
    labelMapper: (T) -> String,
    parentIdMapper: (T) -> ID?,
    iconMapper: (T) -> ImageVector?,
    colorMapper: (T) -> Color?,
    onParentSelected: (ID?) -> Unit,
    onEditClick: (T) -> Unit,
    onAddChildClick: (T) -> Unit,
    depth: Int,
    startExpanded: Boolean
) {
    val itemId = idMapper(item)
    val children = remember(itemId, allItems, parentIdMapper, labelMapper) {
        allItems.filter { parentIdMapper(it) == itemId }.sortedBy { labelMapper(it) }
    }
    var expanded by remember(itemId, startExpanded) {
        mutableStateOf(
            startExpanded && (selectedIds.contains(itemId) ||
                selectedIds.any { id ->
                    allItems.find { idMapper(it) == id }?.let { parentIdMapper(it) } == itemId
                })
        )
    }

    AeonGlassHost(modifier = Modifier.fillMaxWidth()) {
        Column {
            HybridSelectItem(
                item = item,
                mode = mode,
                isSelected = selectedIds.contains(itemId),
                hasChildren = children.isNotEmpty() || mode == AeonSelectMode.MANAGEMENT,
                isExpanded = expanded,
                onToggleExpand = { expanded = !expanded },
                idMapper = idMapper,
                labelMapper = labelMapper,
                iconMapper = iconMapper,
                colorMapper = colorMapper,
                onEditClick = { onEditClick(item) },
                onAddChildClick = { onAddChildClick(item) },
                onClick = {
                    when (mode) {
                        AeonSelectMode.SINGLE -> onSelectionChange(setOf(itemId))
                        AeonSelectMode.PARENT -> onParentSelected(itemId)
                        AeonSelectMode.MULTI -> {
                            val descendants =
                                collectDescendantIds(itemId, allItems, idMapper, parentIdMapper)
                            val newSelected = if (itemId in selectedIds) {
                                selectedIds - itemId - descendants
                            } else {
                                selectedIds + itemId + descendants
                            }
                            onSelectionChange(newSelected)
                        }

                        AeonSelectMode.MANAGEMENT -> onEditClick(item)
                    }
                }
            )

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .padding(bottom = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    children.forEach { child ->
                        key(idMapper(child)) {
                            RecursiveHybridNode(
                                item = child,
                                allItems = allItems,
                                mode = mode,
                                selectedIds = selectedIds,
                                onSelectionChange = onSelectionChange,
                                idMapper = idMapper,
                                labelMapper = labelMapper,
                                parentIdMapper = parentIdMapper,
                                iconMapper = iconMapper,
                                colorMapper = colorMapper,
                                onParentSelected = onParentSelected,
                                onEditClick = onEditClick,
                                onAddChildClick = onAddChildClick,
                                depth = depth + 1,
                                startExpanded = startExpanded
                            )
                        }
                    }

                    if (children.isNotEmpty()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                            color = AppColors.divider.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun <T, ID> HybridSelectItem(
    item: T,
    mode: AeonSelectMode,
    isSelected: Boolean,
    hasChildren: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    idMapper: (T) -> ID,
    labelMapper: (T) -> String,
    iconMapper: (T) -> ImageVector?,
    colorMapper: (T) -> Color?,
    onEditClick: () -> Unit,
    onAddChildClick: () -> Unit = {},
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val dims = AeonTheme.dimensions
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "itemScale")

    // Resolve item color first so it can drive all selection-state animations.
    val icon = iconMapper(item)
    val color = colorMapper(item) ?: AppColors.primary
    val isActiveSelection = isSelected && mode != AeonSelectMode.MANAGEMENT

    val backgroundColor by animateColorAsState(
        if (isActiveSelection) color.copy(alpha = 0.15f)
        else AppColors.surfaceContainerLow,
        label = "bgColor"
    )

    val borderColor by animateColorAsState(
        if (isActiveSelection) color
        else AppColors.outlineVariant.copy(alpha = 0.6f),
        label = "borderColor"
    )

    val borderWidth by animateDpAsState(
        if (isActiveSelection) 1.5.dp else 1.dp,
        label = "borderWidth"
    )

    val contentAlpha = if (isActiveSelection) 1f else 0.85f

    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        shape = RoundedCornerShape(dims.componentCornerRadius),
        hazeStyle = GlassTokens.itemStyle(),
        glassVariant = GlassVariant.Item,
        fallbackColor = backgroundColor,
        borderColor = borderColor,
        borderWidth = borderWidth,
        shadowElevation = if (isActiveSelection) 2.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 72.dp)
                .padding(horizontal = dims.cardPadding, vertical = dims.spacingMd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: Expansion Control
            if (hasChildren) {
                IconButton(onClick = onToggleExpand, modifier = Modifier.size(dims.iconSizeMd)) {
                    Icon(
                        if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(dims.iconSizeMd),
                        tint = AppColors.contentSecondary
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(dims.iconSizeMd))
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Item Icon — always in the item's own color; alpha increases slightly when selected.
            Surface(
                modifier = Modifier.size(dims.iconSizeLg),
                shape = CircleShape,
                color = color.copy(alpha = if (isActiveSelection) 0.30f else 0.18f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (icon != null) {
                        Icon(
                            icon,
                            contentDescription = null,
                            modifier = Modifier.size(dims.iconSizeMd),
                            tint = color
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(dims.spacingLg))

            // Item Label
            Text(
                labelMapper(item),
                style = AppTextStyles.listItemTitle,
                color = AppColors.contentPrimary,
                modifier = Modifier
                    .weight(1f)
                    .alpha(contentAlpha)
            )

            // Right side: Selection Indicator / Management Controls
            Box(
                modifier = Modifier.defaultMinSize(minWidth = dims.iconSizeMd),
                contentAlignment = Alignment.Center
            ) {
                when (mode) {
                    AeonSelectMode.SINGLE, AeonSelectMode.PARENT -> {
                        if (isSelected) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = "Selected",
                                tint = color,
                                modifier = Modifier.size(dims.iconSizeMd)
                            )
                        }
                    }

                    AeonSelectMode.MULTI -> {
                        Switch(
                            checked = isSelected,
                            onCheckedChange = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onClick()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = color,
                                checkedTrackColor = color.copy(alpha = 0.3f),
                                checkedBorderColor = color,
                                uncheckedThumbColor = AppColors.switchThumbOff,
                                uncheckedTrackColor = AppColors.switchTrackOff,
                                uncheckedBorderColor = AppColors.outline
                            )
                        )
                    }

                    AeonSelectMode.MANAGEMENT -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = onAddChildClick, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    Icons.Rounded.Add,
                                    contentDescription = "Add child",
                                    modifier = Modifier.size(20.dp),
                                    tint = AppColors.primary
                                )
                            }
                            IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    Icons.Rounded.Edit,
                                    contentDescription = "Edit",
                                    modifier = Modifier.size(18.dp),
                                    tint = AppColors.contentSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AeonHierarchicalHybridSelectorSinglePreview() {
    val items = listOf(
        PreviewItem(1, "Travel", null, Icons.Rounded.Flight),
        PreviewItem(2, "Flights", 1, Icons.Rounded.AirplanemodeActive),
        PreviewItem(3, "Hotel", 1, Icons.Rounded.Hotel),
        PreviewItem(4, "Food", null, Icons.Rounded.Restaurant),
        PreviewItem(5, "Groceries", 4, Icons.Rounded.ShoppingBasket),
        PreviewItem(6, "Dining Out", 4, Icons.Rounded.Dining),
    )
    OssWalletTheme {
        Surface(color = AppColors.background, modifier = Modifier.fillMaxSize()) {
            AeonHierarchicalHybridSelector(
                items = items,
                mode = AeonSelectMode.SINGLE,
                selectedIds = setOf(2L),
                onSelectionChange = {},
                idMapper = { it.id },
                labelMapper = { it.name },
                parentIdMapper = { it.parentId },
                iconMapper = { it.icon },
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AeonHierarchicalHybridSelectorMultiPreview() {
    val items = listOf(
        PreviewItem(1, "Travel", null, Icons.Rounded.Flight),
        PreviewItem(2, "Flights", 1, Icons.Rounded.AirplanemodeActive),
        PreviewItem(3, "Hotel", 1, Icons.Rounded.Hotel),
        PreviewItem(4, "Food", null, Icons.Rounded.Restaurant),
        PreviewItem(5, "Groceries", 4, Icons.Rounded.ShoppingBasket),
        PreviewItem(6, "Dining Out", 4, Icons.Rounded.Dining),
    )
    OssWalletTheme {
        Surface(color = AppColors.background, modifier = Modifier.fillMaxSize()) {
            AeonHierarchicalHybridSelector(
                items = items,
                mode = AeonSelectMode.MULTI,
                selectedIds = setOf(2L, 5L),
                onSelectionChange = {},
                idMapper = { it.id },
                labelMapper = { it.name },
                parentIdMapper = { it.parentId },
                iconMapper = { it.icon },
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AeonHierarchicalHybridSelectorManagementPreview() {
    val items = listOf(
        PreviewItem(1, "Travel", null, Icons.Rounded.Flight),
        PreviewItem(2, "Flights", 1, Icons.Rounded.AirplanemodeActive),
        PreviewItem(3, "Hotel", 1, Icons.Rounded.Hotel),
        PreviewItem(4, "Food", null, Icons.Rounded.Restaurant),
        PreviewItem(5, "Groceries", 4, Icons.Rounded.ShoppingBasket),
        PreviewItem(6, "Dining Out", 4, Icons.Rounded.Dining),
    )
    OssWalletTheme {
        Surface(color = AppColors.background, modifier = Modifier.fillMaxSize()) {
            AeonHierarchicalHybridSelector(
                items = items,
                mode = AeonSelectMode.MANAGEMENT,
                selectedIds = emptySet(),
                onSelectionChange = {},
                idMapper = { it.id },
                labelMapper = { it.name },
                parentIdMapper = { it.parentId },
                iconMapper = { it.icon },
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

private data class PreviewItem(
    val id: Long,
    val name: String,
    val parentId: Long?,
    val icon: ImageVector
)
