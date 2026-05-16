package com.osscabinet.ossWallet.ui.screens.categories.list.components

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
import com.osscabinet.ossWallet.ui.components.entry.*
import com.osscabinet.ossWallet.ui.components.AeonHeroHeader
import com.osscabinet.ossWallet.ui.components.VisualIdentitySection
import com.osscabinet.ossWallet.ui.components.AppIconMap
import com.osscabinet.ossWallet.ui.components.AeonHierarchicalHybridSelector
import com.osscabinet.ossWallet.ui.components.AeonSelectMode
import com.osscabinet.ossWallet.ui.components.AeonFullScreenDialog
import com.osscabinet.ossWallet.ui.components.AeonSelectionScreen
import com.osscabinet.ossWallet.ui.theme.OutlineLight
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

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
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = AppColors.error)
                    }
                }
                
                IconButton(
                    onClick = { onSave(name, selectedIcon, displayColorHex.toColorInt(), selectedParentId) },
                    enabled = name.isNotBlank() && !isMandatory
                ) {
                    Icon(
                        Icons.Rounded.Done,
                        contentDescription = "Save",
                        tint = if (name.isNotBlank() && !isMandatory) AppColors.primary else AppColors.contentSecondary.copy(alpha = 0.38f)
                    )
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
                        color = AppColors.errorContainer.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = AppColors.error
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "This is a system category and cannot be modified or deleted.",
                                style = AppTextStyles.transactionSwipeAction,
                                color = AppColors.onErrorContainer
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
                EntrySection(title = "IDENTITY", icon = Icons.Rounded.Category) {
                    EntryEditorialInput(
                        value = name,
                        onValueChange = { if (!isMandatory) name = it },
                        label = "CATEGORY NAME",
                        icon = Icons.Rounded.Category
                    )

                    val parentCategory = parentCategories.find { it.id == selectedParentId }
                    var showParentPicker by remember { mutableStateOf(false) }

                    EntryActionCard(
                        icon = Icons.Rounded.AccountTree,
                        label = "PARENT CATEGORY",
                        value = parentCategory?.name ?: "None",
                        onClick = { if (!isMandatory) showParentPicker = true },
                        iconContainerColor = AppColors.formIconContainerSecondary.copy(alpha = 0.1f),
                        iconTint = AppColors.secondary
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
                                                iconMapper = { AppIconMap[it.iconName] },
                                                colorMapper = { if (it.colorHex == -1) OutlineLight else Color(it.colorHex) },
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
                        color = AppColors.secondaryContainer.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.Info,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = AppColors.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Inheriting color from parent category.",
                                style = AppTextStyles.transactionSwipeAction,
                                color = AppColors.onSecondaryContainer
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
