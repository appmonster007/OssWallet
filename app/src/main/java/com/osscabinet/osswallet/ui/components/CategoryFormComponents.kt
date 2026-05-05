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
import com.osscabinet.ossWallet.ui.components.AeonFormHeader
import com.osscabinet.ossWallet.ui.components.AeonHeroHeader
import com.osscabinet.ossWallet.ui.components.VisualIdentitySection
import com.osscabinet.ossWallet.ui.components.EditorialInput
import com.osscabinet.ossWallet.ui.components.RecordFieldCard
import com.osscabinet.ossWallet.ui.components.AccountIconMap
import com.osscabinet.ossWallet.ui.components.CategoryParentSelector
import com.osscabinet.ossWallet.ui.components.AeonSectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditSheet(
    category: Category?,
    parentCategories: List<Category>,
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
                var showParentPicker by remember { mutableStateOf(false) }

                RecordFieldCard(
                    icon = Icons.Rounded.AccountTree,
                    label = "PARENT CATEGORY",
                    value = parentCategory?.name ?: "None",
                    onClick = { showParentPicker = true },
                    trailingIcon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    iconContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                )

                if (showParentPicker) {
                    ModalBottomSheet(
                        onDismissRequest = { showParentPicker = false },
                        containerColor = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .padding(bottom = 32.dp)
                        ) {
                            AeonSectionHeader(title = "Select Parent Category")
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Box(modifier = Modifier.weight(1f, fill = false)) {
                                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                    CategoryParentSelector(
                                        categories = parentCategories.filter { it.id != (category?.id ?: 0L) },
                                        selectedParentId = selectedParentId,
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
