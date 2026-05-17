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
import androidx.compose.ui.window.Dialog
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

    var showIdentityDialog by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(28.dp),
            color = AppColors.panelSurface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Header with Title and Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (!isEditMode) "New Category" else "Edit Category",
                        style = AppTextStyles.listItemTitle.copy(fontWeight = FontWeight.Bold)
                    )
                    
                    if (isEditMode && !isMandatory && onDelete != null) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = AppColors.error)
                        }
                    }
                }

                if (isMandatory) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = AppColors.errorContainer.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = AppColors.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "System category locked.",
                                style = AppTextStyles.transactionSwipeAction.copy(fontSize = 12.sp),
                                color = AppColors.onErrorContainer
                            )
                        }
                    }
                }

                // Interactive Hero Header
                AeonHeroHeader(
                    title = name,
                    subtitle = if (selectedParentId != null) {
                        "Sub-category of ${parentCategories.find { it.id == selectedParentId }?.name}"
                    } else {
                        "Primary Category"
                    },
                    iconName = selectedIcon,
                    accentColorHex = displayColorHex,
                    isLeftAligned = true,
                    onTitleChange = { if (!isMandatory) name = it },
                    onIconClick = { if (!isMandatory) showIdentityDialog = true }
                )

                // Structure Section
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    val parentCategory = parentCategories.find { it.id == selectedParentId }
                    var showParentPicker by remember { mutableStateOf(false) }

                    EntryActionCard(
                        icon = Icons.Rounded.AccountTree,
                        label = "PARENT CATEGORY",
                        value = parentCategory?.name ?: "None (Primary)",
                        onClick = { if (!isMandatory) showParentPicker = true },
                        iconContainerColor = AppColors.formIconContainerSecondary.copy(alpha = 0.1f),
                        iconTint = AppColors.secondary
                    )

                    if (showParentPicker && !isMandatory) {
                        Dialog(onDismissRequest = { showParentPicker = false }) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth(0.95f)
                                    .fillMaxHeight(0.7f),
                                shape = RoundedCornerShape(28.dp),
                                color = AppColors.panelSurface,
                                tonalElevation = 8.dp
                            ) {
                                Column(modifier = Modifier.padding(24.dp)) {
                                    Text(
                                        "Select Parent",
                                        style = AppTextStyles.listItemTitle.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    )
                                    
                                    Box(modifier = Modifier.weight(1f)) {
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

                    if (selectedParentId != null) {
                        Text(
                            "Inheriting color from parent category.",
                            style = AppTextStyles.transactionSwipeAction.copy(
                                color = AppColors.contentSecondary.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            ),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                // Final Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = AppColors.contentSecondary)
                    }
                    
                    Button(
                        onClick = { onSave(name, selectedIcon, displayColorHex.toColorInt(), selectedParentId) },
                        enabled = name.isNotBlank() && !isMandatory,
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.primary,
                            disabledContainerColor = AppColors.divider.copy(alpha = 0.1f)
                        )
                    ) {
                        Text("Save Category")
                    }
                }
            }
        }
    }

    // Nested Visual Identity Dialog
    if (showIdentityDialog) {
        Dialog(onDismissRequest = { showIdentityDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .wrapContentHeight()
                    .padding(vertical = 24.dp),
                shape = RoundedCornerShape(28.dp),
                color = AppColors.panelSurface,
                tonalElevation = 12.dp // Higher elevation than parent
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Visual Identity",
                            style = AppTextStyles.listItemTitle.copy(fontWeight = FontWeight.Bold)
                        )
                        IconButton(onClick = { showIdentityDialog = false }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close")
                        }
                    }

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
    }
}
