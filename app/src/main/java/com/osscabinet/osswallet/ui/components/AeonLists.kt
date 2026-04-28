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
import com.osscabinet.ossWallet.ui.components.AccountIconMap

@Composable
fun CategoryListItem(
    category: Category,
    isParent: Boolean,
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
        val displayColor = if (category.colorHex == -1) 0xFF9E9E9E.toInt() else category.colorHex
        Surface(
            modifier = Modifier.size(if (isParent) 40.dp else 32.dp),
            shape = CircleShape,
            color = Color(displayColor).copy(alpha = 0.2f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    AccountIconMap[category.iconName] ?: Icons.Rounded.Category,
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
        
        if (isManagementMode) {
            IconButton(onClick = onEditClick, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Rounded.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
            }
        } else if (isSelected) {
            Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        
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

@Composable
fun AeonCategoryList(
    categories: List<Category>,
    selectedCategoryId: Long? = null,
    onCategorySelected: (Category) -> Unit,
    isManagementMode: Boolean = false,
    onEditClick: (Category) -> Unit = {},
    onAddSubCategoryClick: (Category) -> Unit = {}
) {
    val parentCategories = categories.filter { it.parentId == null }.sortedBy { it.name }
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        parentCategories.forEach { parent ->
            var expanded by remember { mutableStateOf(categories.any { it.parentId == parent.id && it.id == selectedCategoryId }) }
            val children = categories.filter { it.parentId == parent.id }.sortedBy { it.name }
            
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Column {
                    CategoryListItem(
                        category = parent,
                        isParent = true,
                        isSelected = selectedCategoryId == parent.id,
                        hasChildren = children.isNotEmpty(),
                        isExpanded = expanded,
                        isManagementMode = isManagementMode,
                        onToggleExpand = { expanded = !expanded },
                        onEditClick = { onEditClick(parent) },
                        onClick = {
                            if (isManagementMode) onEditClick(parent) else onCategorySelected(parent)
                        }
                    )
                    
                    AnimatedVisibility(
                        visible = expanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, bottom = 8.dp)) {
                            children.forEach { child ->
                                CategoryListItem(
                                    category = child,
                                    isParent = false,
                                    isSelected = selectedCategoryId == child.id,
                                    isManagementMode = isManagementMode,
                                    onEditClick = { onEditClick(child) },
                                    onClick = {
                                        if (isManagementMode) onEditClick(child) else onCategorySelected(child)
                                    }
                                )
                            }
                            
                            if (isManagementMode) {
                                // Add Sub-category button
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { onAddSubCategoryClick(parent) }
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
                        }
                    }
                }
            }
        }
    }
}
