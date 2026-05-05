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

enum class AeonSelectMode {
    SINGLE, MULTI, PARENT, MANAGEMENT
}

/**
 * A unified selector for regular lists that supports single-select and multi-select modes.
 * Features a modern "layered" design consistent with AeonHierarchicalHybridSelector.
 */
@Composable
fun <T> AeonHybridSelector(
    items: List<T>,
    mode: AeonSelectMode,
    selectedIds: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    idMapper: (T) -> String,
    labelMapper: (T) -> String,
    modifier: Modifier = Modifier,
    iconMapper: (T) -> ImageVector? = { null },
    colorMapper: (T) -> Color? = { null },
    detailMapper: (T) -> String? = { null },
    onEditClick: (T) -> Unit = {}
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEachIndexed { index, item ->
            val id = idMapper(item)
            val isSelected = id in selectedIds
            val backgroundColor = if (index % 2 == 0) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
            }

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = backgroundColor,
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
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
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left spacer for alignment with hierarchical selector
                    Spacer(modifier = Modifier.width(32.dp))

                    // Icon
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

                    // Labels
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            labelMapper(item),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        val detail = detailMapper(item)
                        if (detail != null) {
                            Text(
                                detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Right side: Selection Indicator / Management Controls
                    when (mode) {
                        AeonSelectMode.SINGLE -> {
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
                                onCheckedChange = { 
                                    val newSelected = if (id in selectedIds) {
                                        selectedIds - id
                                    } else {
                                        selectedIds + id
                                    }
                                    onSelectionChange(newSelected)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                        AeonSelectMode.MANAGEMENT -> {
                            IconButton(onClick = { onEditClick(item) }, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    Icons.Rounded.Edit,
                                    contentDescription = "Edit",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}
