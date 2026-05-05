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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Category

@Composable
fun CategoryListItem(
    category: Category,
    isSelected: Boolean = false,
    hasChildren: Boolean = false,
    isExpanded: Boolean = false,
    isManagementMode: Boolean = false,
    onToggleExpand: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
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
        
        if (isManagementMode) {
            IconButton(onClick = onEditClick, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Rounded.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
            }
        } else if (isSelected) {
            Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun AeonCategoryList(
    categories: List<Category>,
    selectedCategoryId: Long? = null,
    onCategorySelected: (Category) -> Unit,
    isManagementMode: Boolean = false,
    onEditClick: (Category) -> Unit = {},
    onAddSubCategoryClick: (Category) -> Unit = {}
) {
    val rootCategories = categories.filter { it.parentId == null }.sortedBy { it.name }
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rootCategories.forEach { root ->
            RecursiveCategoryNode(
                category = root,
                allCategories = categories,
                selectedCategoryId = selectedCategoryId,
                onCategorySelected = onCategorySelected,
                isManagementMode = isManagementMode,
                onEditClick = onEditClick,
                onAddSubCategoryClick = onAddSubCategoryClick,
                depth = 0
            )
        }
    }
}

@Composable
private fun RecursiveCategoryNode(
    category: Category,
    allCategories: List<Category>,
    selectedCategoryId: Long?,
    onCategorySelected: (Category) -> Unit,
    isManagementMode: Boolean,
    onEditClick: (Category) -> Unit,
    onAddSubCategoryClick: (Category) -> Unit,
    depth: Int
) {
    val children = allCategories.filter { it.parentId == category.id }.sortedBy { it.name }
    var expanded by remember { 
        mutableStateOf(
            selectedCategoryId != null && 
            (category.id == selectedCategoryId || allCategories.any { it.parentId == category.id && it.id == selectedCategoryId })
        ) 
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
            CategoryListItem(
                category = category,
                isSelected = selectedCategoryId == category.id,
                hasChildren = children.isNotEmpty() || isManagementMode,
                isExpanded = expanded,
                isManagementMode = isManagementMode,
                onToggleExpand = { expanded = !expanded },
                onEditClick = { onEditClick(category) },
                onClick = {
                    if (isManagementMode) onEditClick(category) else onCategorySelected(category)
                }
            )
            
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    children.forEach { child ->
                        RecursiveCategoryNode(
                            category = child,
                            allCategories = allCategories,
                            selectedCategoryId = selectedCategoryId,
                            onCategorySelected = onCategorySelected,
                            isManagementMode = isManagementMode,
                            onEditClick = onEditClick,
                            onAddSubCategoryClick = onAddSubCategoryClick,
                            depth = depth + 1
                        )
                    }
                    
                    if (isManagementMode) {
                        // Add Sub-category button
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onAddSubCategoryClick(category) }
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            color = Color.Transparent
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "Add sub-category",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    if (children.isNotEmpty() || isManagementMode) {
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
