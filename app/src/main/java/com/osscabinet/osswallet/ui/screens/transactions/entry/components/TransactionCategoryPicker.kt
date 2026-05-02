package com.osscabinet.ossWallet.ui.screens.transactions.entry.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.ui.components.categoryIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionCategoryPicker(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategorySelected: (String, Long?) -> Unit,
    onDismiss: () -> Unit
) {
    // Group categories into a hierarchy
    val parentCategories = remember(categories) { 
        categories.filter { it.parentId == null }.sortedBy { it.name }
    }
    val childCategoriesMap = remember(categories) { 
        categories.filter { it.parentId != null }.groupBy { it.parentId } 
    }

    // Keep track of which parents are expanded
    // Initialize with parent of selected category expanded if any
    var expandedParents by remember { 
        val initial = mutableSetOf<Long>()
        selectedCategoryId?.let { id ->
            categories.find { it.id == id }?.parentId?.let { initial.add(it) }
        }
        mutableStateOf(initial.toSet()) 
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)) }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Select Category",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
            )
            Text(
                text = "Classify this transaction for better analytics",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                parentCategories.forEach { parent ->
                    val children = childCategoriesMap[parent.id]?.sortedBy { it.name } ?: emptyList()
                    val hasChildren = children.isNotEmpty()
                    val isExpanded = parent.id in expandedParents
                    val isSelected = parent.id == selectedCategoryId

                    item(key = "parent_${parent.id}") {
                        CategoryPickerItem(
                            category = parent,
                            isSelected = isSelected,
                            hasChildren = hasChildren,
                            isExpanded = isExpanded,
                            onExpandClick = {
                                expandedParents = if (isExpanded) {
                                    expandedParents - parent.id
                                } else {
                                    expandedParents + parent.id
                                }
                            },
                            onSelect = { onCategorySelected(parent.name, parent.id) }
                        )
                    }

                    if (hasChildren && isExpanded) {
                        items(children, key = { "child_${it.id}" }) { child ->
                            val isChildSelected = child.id == selectedCategoryId
                            CategoryPickerItem(
                                category = child,
                                isSelected = isChildSelected,
                                isChild = true,
                                onSelect = { onCategorySelected(child.name, child.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryPickerItem(
    category: Category,
    isSelected: Boolean,
    isChild: Boolean = false,
    hasChildren: Boolean = false,
    isExpanded: Boolean = false,
    onExpandClick: (() -> Unit)? = null,
    onSelect: () -> Unit
) {
    val categoryColor = Color(category.colorHex)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (isChild) 32.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            onClick = onSelect,
            shape = RoundedCornerShape(20.dp),
            color = if (isSelected) {
                categoryColor.copy(alpha = 0.08f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLowest
            },
            border = BorderStroke(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                color = if (isSelected) categoryColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
            ),
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = categoryColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(if (isChild) 32.dp else 40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val categoryIcon = categoryIcons[category.iconName] ?: categoryIcons[category.name] ?: Icons.Rounded.Category
                        Icon(
                            categoryIcon,
                            contentDescription = null,
                            tint = categoryColor,
                            modifier = Modifier.size(if (isChild) 16.dp else 20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = category.name,
                    style = if (isChild) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                    ),
                    modifier = Modifier.weight(1f)
                )
                if (isSelected) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = "Selected",
                        tint = categoryColor,
                        modifier = Modifier.size(if (isChild) 20.dp else 24.dp)
                    )
                }
            }
        }

        if (hasChildren && onExpandClick != null) {
            Surface(
                onClick = onExpandClick,
                shape = CircleShape,
                color = if (isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
