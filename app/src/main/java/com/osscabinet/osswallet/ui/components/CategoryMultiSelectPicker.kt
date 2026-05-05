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
    val rootCategories = categories.filter { it.parentId == null }.sortedBy { it.name }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rootCategories.forEach { root ->
            RecursiveMultiSelectNode(
                category = root,
                allCategories = categories,
                selectedCategoryIds = selectedCategoryIds,
                onCategoryToggle = onCategoryToggle,
                depth = 0
            )
        }
    }
}

@Composable
private fun RecursiveMultiSelectNode(
    category: Category,
    allCategories: List<Category>,
    selectedCategoryIds: Set<Long>,
    onCategoryToggle: (Long) -> Unit,
    depth: Int
) {
    val children = allCategories.filter { it.parentId == category.id }.sortedBy { it.name }
    var expanded by remember { 
        mutableStateOf(selectedCategoryIds.any { id -> 
            allCategories.find { it.id == id }?.parentId == category.id 
        }) 
    }
    
    val content = @Composable {
        Column {
            CategoryMultiSelectItem(
                category = category,
                depth = depth,
                isSelected = category.id in selectedCategoryIds,
                hasChildren = children.isNotEmpty(),
                isExpanded = expanded,
                onToggleExpand = { expanded = !expanded },
                onToggle = { onCategoryToggle(category.id) }
            )

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    children.forEach { child ->
                        RecursiveMultiSelectNode(
                            category = child,
                            allCategories = allCategories,
                            selectedCategoryIds = selectedCategoryIds,
                            onCategoryToggle = onCategoryToggle,
                            depth = depth + 1
                        )
                    }
                }
            }
        }
    }

    if (depth == 0) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ) {
            content()
        }
    } else {
        content()
    }
}

@Composable
private fun CategoryMultiSelectItem(
    category: Category,
    depth: Int,
    isSelected: Boolean,
    hasChildren: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onToggle: () -> Unit
) {
    val isRoot = depth == 0
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
            modifier = Modifier.size(if (isRoot) 40.dp else 32.dp),
            shape = CircleShape,
            color = Color(displayColor).copy(alpha = 0.2f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    AccountIconMap[category.iconName] ?: Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(if (isRoot) 20.dp else 16.dp),
                    tint = Color(displayColor)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            category.name,
            style = if (isRoot) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = if (isRoot) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        if (hasChildren) {
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
