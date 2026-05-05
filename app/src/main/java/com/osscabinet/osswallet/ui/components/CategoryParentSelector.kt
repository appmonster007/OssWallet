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
import com.osscabinet.ossWallet.ui.components.AccountIconMap

@Composable
fun CategoryParentSelector(
    categories: List<Category>,
    selectedParentId: Long?,
    onParentSelected: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val rootCategories = categories.filter { it.parentId == null }.sortedBy { it.name }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Option for "None"
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (selectedParentId == null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.fillMaxWidth().clickable { onParentSelected(null) }
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
                if (selectedParentId == null) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        rootCategories.forEach { root ->
            RecursiveParentNode(
                category = root,
                allCategories = categories,
                selectedParentId = selectedParentId,
                onParentSelected = onParentSelected,
                depth = 0
            )
        }
    }
}

@Composable
private fun RecursiveParentNode(
    category: Category,
    allCategories: List<Category>,
    selectedParentId: Long?,
    onParentSelected: (Long?) -> Unit,
    depth: Int
) {
    val children = allCategories.filter { it.parentId == category.id }.sortedBy { it.name }
    var expanded by remember { 
        mutableStateOf(selectedParentId != null && 
            (category.id == selectedParentId || allCategories.any { it.parentId == category.id && it.id == selectedParentId })) 
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
            CategoryParentItem(
                category = category,
                isSelected = selectedParentId == category.id,
                hasChildren = children.isNotEmpty(),
                isExpanded = expanded,
                onToggleExpand = { expanded = !expanded },
                onClick = { onParentSelected(category.id) }
            )

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    children.forEach { child ->
                        RecursiveParentNode(
                            category = child,
                            allCategories = allCategories,
                            selectedParentId = selectedParentId,
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
private fun CategoryParentItem(
    category: Category,
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
        
        Text(
            category.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        if (isSelected) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CategoryParentSelectorPreview() {
    val mockCategories = listOf(
        Category(1, "Food", "Restaurant", 0xFFE91E63.toInt(), true, null),
        Category(2, "Groceries", "ShoppingBag", 0xFFE91E63.toInt(), false, 1),
        Category(3, "Dining Out", "Restaurant", 0xFFE91E63.toInt(), false, 1),
        Category(4, "Fast Food", "Fastfood", 0xFFE91E63.toInt(), false, 3),
        Category(5, "Transport", "DirectionsCar", 0xFF2196F3.toInt(), true, null),
        Category(6, "Fuel", "LocalGasStation", 0xFF2196F3.toInt(), false, 5),
        Category(7, "Public Transport", "DirectionsBus", 0xFF2196F3.toInt(), false, 5)
    )
    
    var selectedId by remember { mutableStateOf<Long?>(1L) }
    
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            CategoryParentSelector(
                categories = mockCategories,
                selectedParentId = selectedId,
                onParentSelected = { selectedId = it }
            )
        }
    }
}
