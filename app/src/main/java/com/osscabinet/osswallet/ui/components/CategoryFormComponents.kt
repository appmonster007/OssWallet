package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.osscabinet.ossWallet.data.local.entity.CategoryEntity
import com.osscabinet.ossWallet.ui.util.toColorInt
import com.osscabinet.ossWallet.ui.util.toHex
import com.osscabinet.ossWallet.ui.components.AeonFormHeader
import com.osscabinet.ossWallet.ui.components.AeonHeroHeader
import com.osscabinet.ossWallet.ui.components.VisualIdentitySection
import com.osscabinet.ossWallet.ui.components.EditorialInput
import com.osscabinet.ossWallet.ui.components.RecordFieldCard
import com.osscabinet.ossWallet.ui.components.AccountIconMap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditSheet(
    category: CategoryEntity?,
    parentCategories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (String, String, Int, Long?) -> Unit
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var selectedIcon by remember { mutableStateOf(category?.iconName ?: "Other") }
    var selectedColorHex by remember { mutableStateOf(category?.colorHex?.toHex() ?: "#9E9E9E") }
    var selectedParentId by remember { mutableStateOf(category?.parentId) }

    val displayColorHex = if (selectedParentId != null) {
        parentCategories.find { it.id == selectedParentId }?.colorHex?.toHex() ?: selectedColorHex
    } else {
        selectedColorHex
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            AeonFormHeader(
                title = if (category == null || category.id == 0L) "New Category" else "Edit Category",
                onSaveClick = { onSave(name, selectedIcon, displayColorHex.toColorInt(), selectedParentId) },
                saveButtonEnabled = name.isNotBlank(),
                accentColorHex = displayColorHex
            )

            // Preview Header
            AeonHeroHeader(
                title = name,
                subtitle = if (selectedParentId != null) {
                    "Sub-category of ${parentCategories.find { it.id == selectedParentId }?.name}"
                } else {
                    "Primary Category"
                },
                iconName = selectedIcon,
                accentColorHex = displayColorHex
            )

            // Identity Section
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "IDENTITY",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.outline,
                    letterSpacing = 1.sp
                )
                
                EditorialInput(
                    value = name,
                    onValueChange = { name = it },
                    label = "CATEGORY NAME",
                    icon = Icons.Rounded.Category
                )

                val parentCategory = parentCategories.find { it.id == selectedParentId }
                var showParentDropdown by remember { mutableStateOf(false) }

                Box(modifier = Modifier.fillMaxWidth()) {
                    RecordFieldCard(
                        icon = Icons.Rounded.AccountTree,
                        label = "PARENT CATEGORY",
                        value = parentCategory?.name ?: "None",
                        onClick = { showParentDropdown = true },
                        trailingIcon = if (showParentDropdown) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        iconContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    )

                    DropdownMenu(
                        expanded = showParentDropdown,
                        onDismissRequest = { showParentDropdown = false },
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .background(MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text("None", fontWeight = FontWeight.Medium) },
                            onClick = {
                                selectedParentId = null
                                showParentDropdown = false
                            },
                            leadingIcon = {
                                Surface(
                                    modifier = Modifier.size(32.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Rounded.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        )
                        
                        if (parentCategories.isNotEmpty()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }

                        parentCategories.filter { it.id != (category?.id ?: 0L) }.forEach { parent ->
                            val isSelected = selectedParentId == parent.id
                            DropdownMenuItem(
                                text = { Text(parent.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                                onClick = {
                                    selectedParentId = parent.id
                                    showParentDropdown = false
                                },
                                leadingIcon = {
                                    Surface(
                                        modifier = Modifier.size(32.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(parent.colorHex).copy(alpha = 0.1f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                AccountIconMap[parent.iconName] ?: Icons.Rounded.Category,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = Color(parent.colorHex)
                                            )
                                        }
                                    }
                                },
                                trailingIcon = if (isSelected) {
                                    { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary) }
                                } else null,
                                colors = MenuDefaults.itemColors(
                                    textColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            if (selectedParentId != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Info,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Inheriting color from parent category.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // Visual Identity Section
            VisualIdentitySection(
                selectedIconName = selectedIcon,
                onIconSelected = { selectedIcon = it },
                accentColorHex = displayColorHex,
                onColorSelected = { 
                    if (selectedParentId == null) {
                        selectedColorHex = it 
                    }
                },
                isColorLocked = selectedParentId != null
            )
        }
    }
}
