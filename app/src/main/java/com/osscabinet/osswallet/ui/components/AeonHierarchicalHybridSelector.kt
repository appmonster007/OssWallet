package com.osscabinet.ossWallet.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

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
    val rootItems = items.filter { parentIdMapper(it) == null }.sortedBy { labelMapper(it) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Special "None" option for Parent Selection mode
        if (mode == AeonSelectMode.PARENT && noneLabel != null) {
            val isNoneSelected = selectedIds.isEmpty()
            val backgroundColor by animateColorAsState(
                if (isNoneSelected) AppColors.primary.copy(alpha = 0.08f)
                else AppColors.recessedSurface,
                label = "noneBgColor"
            )
            val borderColor by animateColorAsState(
                if (isNoneSelected) AppColors.primary
                else AppColors.divider.copy(alpha = 0.2f),
                label = "noneBorderColor"
            )
            val borderWidth by androidx.compose.animation.core.animateDpAsState(
                if (isNoneSelected) 1.5.dp else 0.5.dp,
                label = "noneBorderWidth"
            )

            Surface(
                onClick = { onParentSelected(null) },
                shape = RoundedCornerShape(24.dp),
                color = backgroundColor,
                border = BorderStroke(borderWidth, borderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(32.dp))
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        color = AppColors.insetSurface.copy(alpha = 0.5f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(noneIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        noneLabel,
                        style = AppTextStyles.screenSubTitle,
                        fontWeight = if (isNoneSelected) FontWeight.Bold else FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    if (isNoneSelected) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = "Selected",
                            tint = AppColors.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        rootItems.forEach { root ->
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
    val children = allItems.filter { parentIdMapper(it) == itemId }.sortedBy { labelMapper(it) }
    var expanded by remember {
        mutableStateOf(startExpanded && (selectedIds.contains(itemId) ||
            selectedIds.any { id -> allItems.find { idMapper(it) == id }?.let { parentIdMapper(it) } == itemId }))
    }

    // Alternating contrast backgrounds for levels
    val backgroundColor = if (depth % 2 == 0) {
        AppColors.insetSurface.copy(alpha = 0.3f)
    } else {
        AppColors.panelSurface.copy(alpha = 0.8f)
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = backgroundColor,
        border = BorderStroke(0.5.dp, AppColors.divider.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
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
                            val newSelected = if (itemId in selectedIds) {
                                selectedIds - itemId
                            } else {
                                selectedIds + itemId
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
                        .padding(top = 8.dp)
                        .padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    children.forEach { child ->
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
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    val backgroundColor by animateColorAsState(
        if (isSelected && mode != AeonSelectMode.MANAGEMENT) AppColors.primary.copy(alpha = 0.08f)
        else AppColors.recessedSurface,
        label = "bgColor"
    )

    val borderColor by animateColorAsState(
        if (isSelected && mode != AeonSelectMode.MANAGEMENT) AppColors.primary
        else AppColors.divider.copy(alpha = 0.2f),
        label = "borderColor"
    )

    val borderWidth by androidx.compose.animation.core.animateDpAsState(
        if (isSelected && mode != AeonSelectMode.MANAGEMENT) 1.5.dp else 0.5.dp,
        label = "borderWidth"
    )

    Surface(
        onClick = {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
            onClick()
        },
        shape = RoundedCornerShape(24.dp),
        color = backgroundColor,
        border = BorderStroke(borderWidth, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: Expansion Control
            if (hasChildren) {
                IconButton(onClick = onToggleExpand, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = AppColors.border
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(32.dp))
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Item Icon
            val icon = iconMapper(item)
            val color = colorMapper(item) ?: AppColors.primary
            Surface(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = color.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (icon != null) {
                        Icon(
                            icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = color
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Item Label
            Text(
                labelMapper(item),
                style = AppTextStyles.listItemTitle.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                ),
                color = if (isSelected && mode != AeonSelectMode.MANAGEMENT) AppColors.primary else AppColors.contentPrimary,
                modifier = Modifier.weight(1f)
            )

            // Right side: Selection Indicator / Management Controls
            when (mode) {
                AeonSelectMode.SINGLE, AeonSelectMode.PARENT -> {
                    if (isSelected) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = "Selected",
                            tint = AppColors.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                AeonSelectMode.MULTI -> {
                    Switch(
                        checked = isSelected,
                        onCheckedChange = { 
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            onClick() 
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AppColors.switchThumbOn,
                            checkedTrackColor = AppColors.primary
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
                                tint = AppColors.border
                            )
                        }
                    }
                }
            }
        }
    }
}
