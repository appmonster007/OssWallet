package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.components.entry.AeonSelectionCard
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

/**
 * A unified selector for regular lists that supports single-select, multi-select, and management modes.
 * Features a modern "layered" design consistent with AeonHierarchicalHybridSelector.
 * Generic version that works with any data type and any ID type.
 *
 * @param T The type of items in the list
 * @param ID The type of item IDs (typically Long or String)
 */
@Composable
fun <T, ID> AeonHybridSelector(
    items: List<T>,
    mode: AeonSelectMode,
    selectedIds: Set<ID>,
    onSelectionChange: (Set<ID>) -> Unit,
    idMapper: (T) -> ID,
    labelMapper: (T) -> String,
    modifier: Modifier = Modifier,
    iconMapper: (T) -> ImageVector? = { null },
    colorMapper: (T) -> Color? = { null },
    detailMapper: (T) -> String? = { null },
    onEditClick: (T) -> Unit = {},
    onAddChildClick: (T) -> Unit = {}
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items.forEach { item ->
            val id = idMapper(item)
            val isSelected = id in selectedIds

            AeonSelectionCard(
                icon = iconMapper(item) ?: Icons.Rounded.Category,
                label = labelMapper(item),
                detail = detailMapper(item),
                isSelected = isSelected,
                iconColor = colorMapper(item) ?: AppColors.primary,
                onClick = {
                    when (mode) {
                        AeonSelectMode.SINGLE -> onSelectionChange(setOf(id))
                        AeonSelectMode.MULTI -> {
                            val newSelected = if (id in selectedIds) {
                                selectedIds - id
                            } else {
                                selectedIds + id
                            }
                            onSelectionChange(newSelected)
                        }

                        AeonSelectMode.MANAGEMENT -> onEditClick(item)
                        else -> {}
                    }
                },
                selectionMode = when (mode) {
                    AeonSelectMode.SINGLE -> AeonSelectMode.SINGLE
                    AeonSelectMode.MULTI -> AeonSelectMode.MULTI
                    AeonSelectMode.MANAGEMENT -> AeonSelectMode.MANAGEMENT
                    else -> AeonSelectMode.SINGLE
                },
                trailingContent = if (mode == AeonSelectMode.MANAGEMENT) {
                    {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = { onAddChildClick(item) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Add,
                                    contentDescription = "Add",
                                    modifier = Modifier.size(20.dp),
                                    tint = AppColors.primary
                                )
                            }
                            IconButton(
                                onClick = { onEditClick(item) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Edit,
                                    contentDescription = "Edit",
                                    modifier = Modifier.size(18.dp),
                                    tint = AppColors.border
                                )
                            }
                        }
                    }
                } else null
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AeonHybridSelectorPreview() {
    val items = listOf("Category 1", "Category 2", "Category 3")
    OssWalletTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            AeonHybridSelector(
                items = items,
                mode = AeonSelectMode.SINGLE,
                selectedIds = setOf("Category 1"),
                onSelectionChange = {},
                idMapper = { it },
                labelMapper = { it },
                iconMapper = { Icons.Rounded.Category }
            )
        }
    }
}
