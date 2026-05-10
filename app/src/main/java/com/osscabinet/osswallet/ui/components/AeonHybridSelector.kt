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
import com.osscabinet.ossWallet.ui.components.entry.EntrySelectionCard
import com.osscabinet.ossWallet.ui.components.entry.EntrySelectionMode

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
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            val id = idMapper(item)
            val isSelected = id in selectedIds

            EntrySelectionCard(
                icon = iconMapper(item) ?: Icons.Rounded.Category,
                label = labelMapper(item),
                detail = detailMapper(item),
                isSelected = isSelected,
                iconColor = colorMapper(item) ?: MaterialTheme.colorScheme.primary,
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
                    AeonSelectMode.SINGLE -> EntrySelectionMode.SINGLE
                    AeonSelectMode.MULTI -> EntrySelectionMode.MULTI
                    AeonSelectMode.MANAGEMENT -> EntrySelectionMode.MANAGEMENT
                    else -> EntrySelectionMode.SINGLE
                },
                trailingContent = if (mode == AeonSelectMode.MANAGEMENT) {
                    {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { onAddChildClick(item) }, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    Icons.Rounded.Add,
                                    contentDescription = "Add",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { onEditClick(item) }, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    Icons.Rounded.Edit,
                                    contentDescription = "Edit",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                } else null
            )
        }
    }
}
