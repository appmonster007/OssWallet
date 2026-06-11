package com.osscabinet.ossWallet.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.components.management.AeonListItem
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

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

        // Special "None" option for Parent Selection mode
        if ((mode == AeonSelectMode.PARENT) && (noneLabel != null)) {
            val isNoneSelected = selectedIds.isEmpty()
            AeonListItem(
                title = noneLabel,
                icon = noneIcon,
                iconTint = AppColors.primary,
                isSelected = isNoneSelected,
                onClick = { onParentSelected(null) },
                leadingContent = { Spacer(modifier = Modifier.size(dims.iconSizeMd)) },
                trailingContent = {
                    if (isNoneSelected) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = "Selected",
                            tint = AppColors.primary,
                            modifier = Modifier.size(dims.iconSizeMd)
                        )
                    }
                }
            )
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
    val dims = AeonTheme.dimensions
    val itemColor = colorMapper(item) ?: AppColors.primary
    val itemIcon = iconMapper(item) ?: Icons.Rounded.Block

    AeonListItem(
        title = labelMapper(item),
        icon = itemIcon,
        iconTint = itemColor,
        isSelected = isSelected && mode != AeonSelectMode.MANAGEMENT,
        onClick = onClick,
        leadingContent = {
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
                Spacer(modifier = Modifier.size(dims.iconSizeMd))
            }
        },
        trailingContent = {
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
                                tint = itemColor,
                                modifier = Modifier.size(dims.iconSizeMd)
                            )
                        }
                    }
                    AeonSelectMode.MULTI -> {
                        Switch(
                            checked = isSelected,
                            onCheckedChange = { onClick() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = itemColor,
                                checkedTrackColor = itemColor.copy(alpha = 0.3f),
                                checkedBorderColor = itemColor,
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
    )
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
