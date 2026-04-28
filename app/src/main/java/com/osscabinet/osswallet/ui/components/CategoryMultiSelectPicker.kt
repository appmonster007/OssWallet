package com.osscabinet.ossWallet.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.ui.components.AccountIconMap

/**
 * Multi-select category picker with expandable parent-child hierarchy.
 * Similar to AeonCategoryList but supports multiple selections with checkboxes.
 */
@Composable
fun CategoryMultiSelectPicker(
    categories: List<Category>,
    selectedCategoryIds: Set<Long>,
    onCategoryToggle: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val parentCategories = categories.filter { it.parentId == null }.sortedBy { it.name }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        parentCategories.forEach { parent ->
            var expanded by remember { mutableStateOf(false) }
            val children = categories.filter { it.parentId == parent.id }.sortedBy { it.name }
            val isParentSelected = parent.id in selectedCategoryIds
            val anyChildSelected = children.any { it.id in selectedCategoryIds }

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Column {
                    CategoryMultiSelectItem(
                        category = parent,
                        isParent = true,
                        isSelected = isParentSelected,
                        hasChildren = children.isNotEmpty(),
                        isExpanded = expanded,
                        onToggleExpand = { expanded = !expanded },
                        onToggle = { onCategoryToggle(parent.id) }
                    )

                    AnimatedVisibility(
                        visible = expanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier.padding(start = 16.dp, end = 8.dp, bottom = 8.dp)
                        ) {
                            children.forEach { child ->
                                CategoryMultiSelectItem(
                                    category = child,
                                    isParent = false,
                                    isSelected = child.id in selectedCategoryIds,
                                    hasChildren = false,
                                    isExpanded = false,
                                    onToggleExpand = {},
                                    onToggle = { onCategoryToggle(child.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryMultiSelectItem(
    category: Category,
    isParent: Boolean,
    isSelected: Boolean,
    hasChildren: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox for multi-select
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() },
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        val displayColor = if (category.colorHex == -1) 0xFF9E9E9E.toInt() else category.colorHex
        Surface(
            modifier = Modifier.size(if (isParent) 40.dp else 32.dp),
            shape = CircleShape,
            color = Color(displayColor).copy(alpha = 0.2f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    AccountIconMap[category.iconName] ?: Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(if (isParent) 20.dp else 16.dp),
                    tint = Color(displayColor)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            category.name,
            style = if (isParent) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = if (isParent) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        if (isParent && hasChildren) {
            IconButton(onClick = onToggleExpand, modifier = Modifier.size(24.dp)) {
                Icon(
                    if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
