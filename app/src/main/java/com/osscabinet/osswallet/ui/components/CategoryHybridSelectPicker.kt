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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Category

enum class CategorySelectionMode {
    SINGLE, MULTI, PARENT
}

/**
 * A unified category picker that supports single-select, multi-select, and parent-selection modes.
 * Implements a "layered" hierarchical design with left-aligned expansion controls.
 */
@Composable
fun CategoryHybridSelectPicker(
    categories: List<Category>,
    mode: CategorySelectionMode,
    selectedIds: Set<Long>,
    onSelectionChange: (Set<Long>) -> Unit,
    modifier: Modifier = Modifier,
    onParentSelected: (Long?) -> Unit = {}
) {
    val rootCategories = categories.filter { it.parentId == null }.sortedBy { it.name }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Special "None" option for Parent Selection mode
        if (mode == CategorySelectionMode.PARENT) {
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
                    Spacer(modifier = Modifier.width(32.dp)) // Alignment with icons
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Block, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        "None (Primary Category)",
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

        rootCategories.forEach { root ->
            RecursiveHybridNode(
                category = root,
                allCategories = categories,
                mode = mode,
                selectedIds = selectedIds,
                onSelectionChange = onSelectionChange,
                onParentSelected = onParentSelected,
                depth = 0
            )
        }
    }
}

@Composable
private fun RecursiveHybridNode(
    category: Category,
    allCategories: List<Category>,
    mode: CategorySelectionMode,
    selectedIds: Set<Long>,
    onSelectionChange: (Set<Long>) -> Unit,
    onParentSelected: (Long?) -> Unit,
    depth: Int
) {
    val children = allCategories.filter { it.parentId == category.id }.sortedBy { it.name }
    var expanded by remember { 
        mutableStateOf(selectedIds.contains(category.id) || 
            selectedIds.any { id -> allCategories.find { it.id == id }?.parentId == category.id }) 
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
                category = category,
                mode = mode,
                isSelected = selectedIds.contains(category.id),
                hasChildren = children.isNotEmpty(),
                isExpanded = expanded,
                onToggleExpand = { expanded = !expanded },
                onClick = {
                    when (mode) {
                        CategorySelectionMode.SINGLE -> onSelectionChange(setOf(category.id))
                        CategorySelectionMode.PARENT -> onParentSelected(category.id)
                        CategorySelectionMode.MULTI -> {
                            val newSelected = if (category.id in selectedIds) {
                                selectedIds - category.id
                            } else {
                                selectedIds + category.id
                            }
                            onSelectionChange(newSelected)
                        }
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
                            category = child,
                            allCategories = allCategories,
                            mode = mode,
                            selectedIds = selectedIds,
                            onSelectionChange = onSelectionChange,
                            onParentSelected = onParentSelected,
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
private fun HybridSelectItem(
    category: Category,
    mode: CategorySelectionMode,
    isSelected: Boolean,
    hasChildren: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
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

        // Category Icon
        val displayColor = if (category.colorHex == -1) 0xFF9E9E9E.toInt() else category.colorHex
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = Color(displayColor).copy(alpha = 0.2f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    AccountIconMap[category.iconName] ?: Icons.Rounded.Category,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color(displayColor)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Category Name
        Text(
            category.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        // Right side: Selection Indicator
        when (mode) {
            CategorySelectionMode.SINGLE, CategorySelectionMode.PARENT -> {
                if (isSelected) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            CategorySelectionMode.MULTI -> {
                Switch(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Multi Select Mode")
@Composable
fun HybridPickerMultiPreview() {
    val mockCategories = getMockCategories()
    var selectedIds by remember { mutableStateOf(setOf(1L, 2L, 6L)) }
    
    MaterialTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            CategoryHybridSelectPicker(
                categories = mockCategories,
                mode = CategorySelectionMode.MULTI,
                selectedIds = selectedIds,
                onSelectionChange = { selectedIds = it }
            )
        }
    }
}

@Preview(showBackground = true, name = "Parent Select Mode")
@Composable
fun HybridPickerParentPreview() {
    val mockCategories = getMockCategories()
    var selectedId by remember { mutableStateOf<Long?>(1L) }
    
    MaterialTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            CategoryHybridSelectPicker(
                categories = mockCategories,
                mode = CategorySelectionMode.PARENT,
                selectedIds = if (selectedId != null) setOf(selectedId!!) else emptySet(),
                onSelectionChange = { },
                onParentSelected = { selectedId = it }
            )
        }
    }
}

private fun getMockCategories() = listOf(
    Category(1, "Food", "Restaurant", 0xFFE91E63.toInt(), true, null),
    Category(2, "Groceries", "ShoppingBag", 0xFFE91E63.toInt(), false, 1),
    Category(3, "Dining Out", "Restaurant", 0xFFE91E63.toInt(), false, 1),
    Category(4, "Fast Food", "Fastfood", 0xFFE91E63.toInt(), false, 3),
    Category(5, "Transport", "DirectionsCar", 0xFF2196F3.toInt(), true, null),
    Category(6, "Fuel", "LocalGasStation", 0xFF2196F3.toInt(), false, 5),
    Category(7, "Public Transport", "DirectionsBus", 0xFF2196F3.toInt(), false, 5)
)
