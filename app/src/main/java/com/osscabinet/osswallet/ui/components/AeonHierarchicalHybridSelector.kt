package com.osscabinet.ossWallet.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
    onAddChildClick: (T) -> Unit = {}
) {
    val rootItems = items.filter { parentIdMapper(it) == null }.sortedBy { labelMapper(it) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Special "None" option for Parent Selection mode
        if (mode == AeonSelectMode.PARENT && noneLabel != null) {
            val isNoneSelected = selectedIds.isEmpty()
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = if (isNoneSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onParentSelected(null) }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(32.dp))
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(noneIcon, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        noneLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (isNoneSelected) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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
                depth = 0
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
    depth: Int
) {
    val itemId = idMapper(item)
    val children = allItems.filter { parentIdMapper(it) == itemId }.sortedBy { labelMapper(it) }
    var expanded by remember {
        mutableStateOf(selectedIds.contains(itemId) ||
            selectedIds.any { id -> allItems.find { idMapper(it) == id }?.let { parentIdMapper(it) } == itemId })
    }

    val backgroundColor = if (depth % 2 == 0) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
    }

    Surface(
        shape = if (depth == 0) RoundedCornerShape(24.dp) else RoundedCornerShape(16.dp),
        color = backgroundColor,
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
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
                Column {
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
                            depth = depth + 1
                        )
                    }

                    if (children.isNotEmpty()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: Expansion Control
        if (hasChildren) {
            IconButton(onClick = onToggleExpand, modifier = Modifier.size(24.dp)) {
                Icon(
                    if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            Spacer(modifier = Modifier.width(24.dp))
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Item Icon
        val icon = iconMapper(item)
        val color = colorMapper(item) ?: MaterialTheme.colorScheme.primary
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = color.copy(alpha = 0.2f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (icon != null) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = color
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Item Label
        Text(
            labelMapper(item),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        // Right side: Selection Indicator / Management Controls
        when (mode) {
            AeonSelectMode.SINGLE, AeonSelectMode.PARENT -> {
                if (isSelected) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            AeonSelectMode.MULTI -> {
                Switch(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
            AeonSelectMode.MANAGEMENT -> {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onAddChildClick, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = "Add child",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onEditClick, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Rounded.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}
