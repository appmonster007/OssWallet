package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.ui.util.toColorInt
import com.osscabinet.ossWallet.ui.util.toHex
import com.osscabinet.ossWallet.ui.components.AeonHeroHeader
import com.osscabinet.ossWallet.ui.components.VisualIdentitySection
import com.osscabinet.ossWallet.ui.components.EditorialInput
import com.osscabinet.ossWallet.ui.components.RecordFieldCard
import com.osscabinet.ossWallet.ui.components.AccountIconMap
import com.osscabinet.ossWallet.ui.components.AeonHierarchicalHybridSelector
import com.osscabinet.ossWallet.ui.components.AeonSelectMode
import com.osscabinet.ossWallet.ui.components.AeonFullScreenDialog
import com.osscabinet.ossWallet.ui.components.AeonSelectionScreen

@Composable
fun CategoryEditSheet(
    category: Category?,
    parentCategories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (String, String, Int, Long?) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var selectedIcon by remember { mutableStateOf(category?.iconName ?: "Other") }
    var selectedColorHex by remember { mutableStateOf(category?.colorHex?.toHex() ?: "#9E9E9E") }
    var selectedParentId by remember { mutableStateOf(category?.parentId) }

    val isMandatory = category != null && category.id >= 1000L && category.id <= 1003L
    val isEditMode = category != null && category.id != 0L

    val displayColorHex = if (selectedParentId != null) {
        parentCategories.find { it.id == selectedParentId }?.colorHex?.toHex() ?: selectedColorHex
    } else {
        selectedColorHex
    }

    AeonFullScreenDialog(onDismissRequest = onDismiss) {
        AeonSelectionScreen(
            title = if (!isEditMode) "New Category" else "Edit Category",
            onBack = onDismiss,
            actions = {
                if (isEditMode && !isMandatory && onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
                
                TextButton(
                    onClick = { onSave(name, selectedIcon, displayColorHex.toColorInt(), selectedParentId) },
                    enabled = name.isNotBlank() && !isMandatory
                ) {
                    Text("SAVE", fontWeight = FontWeight.Bold)
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                if (isMandatory) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "This is a system category and cannot be modified or deleted.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

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
                        onValueChange = { if (!isMandatory) name = it },
                        label = "CATEGORY NAME",
                        icon = Icons.Rounded.Category,
                        readOnly = isMandatory
                    )

                    val parentCategory = parentCategories.find { it.id == selectedParentId }
                    var showParentPicker by remember { mutableStateOf(false) }

                    RecordFieldCard(
                        icon = Icons.Rounded.AccountTree,
                        label = "PARENT CATEGORY",
                        value = parentCategory?.name ?: "None",
                        onClick = { if (!isMandatory) showParentPicker = true },
                        trailingIcon = if (!isMandatory) Icons.AutoMirrored.Rounded.KeyboardArrowRight else null,
                        iconContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    )

                    if (showParentPicker && !isMandatory) {
                        AeonFullScreenDialog(onDismissRequest = { showParentPicker = false }) {
                            AeonSelectionScreen(
                                title = "Select Parent Category",
                                onBack = { showParentPicker = false }
                            ) { paddingPicker ->
                                Column(
                                    modifier = Modifier
                                        .padding(paddingPicker)
                                        .padding(24.dp)
                                ) {
                                    Box(modifier = Modifier.weight(1f, fill = false)) {
                                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                            AeonHierarchicalHybridSelector(
                                                items = parentCategories.filter { it.id != (category?.id ?: 0L) },
                                                mode = AeonSelectMode.PARENT,
                                                selectedIds = if (selectedParentId != null) setOf(selectedParentId!!) else emptySet(),
                                                onSelectionChange = { },
                                                idMapper = { it.id },
                                                labelMapper = { it.name },
                                                parentIdMapper = { it.parentId },
                                                iconMapper = { AccountIconMap[it.iconName] },
                                                colorMapper = { if (it.colorHex == -1) Color(0xFF9E9E9E) else Color(it.colorHex) },
                                                noneLabel = "None (Primary Category)",
                                                onParentSelected = {
                                                    selectedParentId = it
                                                    showParentPicker = false
                                                }
                                            )
                                        }
                                    }
                                }
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
                    onIconSelected = { if (!isMandatory) selectedIcon = it },
                    accentColorHex = displayColorHex,
                    onColorSelected = { 
                        if (selectedParentId == null && !isMandatory) {
                            selectedColorHex = it 
                        }
                    },
                    isColorLocked = selectedParentId != null || isMandatory
                )
            }
        }
    }
}
