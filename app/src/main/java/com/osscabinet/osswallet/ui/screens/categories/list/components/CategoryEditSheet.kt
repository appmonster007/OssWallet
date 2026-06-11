package com.osscabinet.ossWallet.ui.screens.categories.list.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.CategoryNature
import com.osscabinet.ossWallet.ui.components.AeonHeroHeader
import com.osscabinet.ossWallet.ui.components.AeonHierarchicalHybridSelector
import com.osscabinet.ossWallet.ui.components.AeonSearchField
import com.osscabinet.ossWallet.ui.components.AeonSelectMode
import com.osscabinet.ossWallet.ui.components.VisualIdentitySection
import com.osscabinet.ossWallet.ui.components.entry.EntryActionCard
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.util.toColorInt
import com.osscabinet.ossWallet.ui.util.toHex

@Composable
fun CategoryEditSheet(
    category: Category?,
    parentCategories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (String, String, Int, Long?, CategoryNature) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val categoryFallbackColor = AppColors.outline
    var name by remember { mutableStateOf(category?.name ?: "") }
    var selectedIcon by remember { mutableStateOf(category?.iconName ?: "Other") }
    var selectedColorHex by remember { mutableStateOf(category?.colorHex?.toHex() ?: "#9E9E9E") }
    var selectedParentId by remember { mutableStateOf(category?.parentId) }
    var selectedNature by remember { mutableStateOf(category?.nature ?: CategoryNature.WANT) }

    val isMandatory = category != null && Category.isSystemCategory(category.id)
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
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = "Delete",
                                tint = AppColors.error
                            )
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
                        "Sub-category"
                    } else {
                        "Primary Category"
                    },
                    iconName = selectedIcon,
                    accentColorHex = displayColorHex,
                    isLeftAligned = true,
                    onTitleChange = { if (!isMandatory) name = it },
                    onIconClick = { if (!isMandatory) showIdentityDialog = true }
                )

                // Classification Section
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "CLASSIFICATION",
                        style = AppTextStyles.formSectionLabel,
                        color = AppColors.contentSecondary
                    )

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

                    EntryActionCard(
                        icon = Icons.AutoMirrored.Rounded.Label,
                        label = "CATEGORY NATURE",
                        value = selectedNature.name.lowercase().replaceFirstChar { it.uppercase() },
                        onClick = {
                            if (!isMandatory) {
                                val entries = CategoryNature.entries
                                val nextIndex = (entries.indexOf(selectedNature) + 1) % entries.size
                                selectedNature = entries[nextIndex]
                            }
                        },
                        iconContainerColor = AppColors.formIconContainerPrimary.copy(alpha = 0.1f),
                        iconTint = AppColors.primary
                    )

                    if (showParentPicker && !isMandatory) {
                        var parentSearchQuery by remember { mutableStateOf("") }
                        val filteredParents =
                            remember(parentSearchQuery, parentCategories, category) {
                                val base = parentCategories.filter { it.id != (category?.id ?: 0L) }
                                if (parentSearchQuery.isBlank()) base
                                else base.filter {
                                    it.name.contains(
                                        parentSearchQuery,
                                        ignoreCase = true
                                    )
                                }
                            }
                        Dialog(
                            onDismissRequest = { showParentPicker = false },
                            properties = DialogProperties(usePlatformDefaultWidth = false)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .wrapContentHeight()
                                    .padding(vertical = 16.dp),
                                shape = RoundedCornerShape(32.dp),
                                color = AppColors.panelSurface,
                                tonalElevation = 12.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(24.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Select Parent",
                                            style = AppTextStyles.heroAmount.copy(fontSize = 20.sp)
                                        )
                                        IconButton(
                                            onClick = { showParentPicker = false },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.Close,
                                                contentDescription = "Close",
                                                modifier = Modifier.size(20.dp),
                                                tint = AppColors.contentSecondary
                                            )
                                        }
                                    }
                                    AeonSearchField(
                                        query = parentSearchQuery,
                                        onQueryChange = { parentSearchQuery = it },
                                        placeholder = "Search categories..."
                                    )
                                    AeonHierarchicalHybridSelector(
                                        items = filteredParents,
                                        mode = AeonSelectMode.PARENT,
                                        selectedIds = if (selectedParentId != null) setOf(
                                            selectedParentId!!
                                        ) else emptySet(),
                                        onSelectionChange = { },
                                        idMapper = { it.id },
                                        labelMapper = { it.name },
                                        parentIdMapper = if (parentSearchQuery.isBlank()) { it -> it.parentId } else { _ -> null },
                                        iconMapper = { getIconByName(it.iconName) },
                                        colorMapper = {
                                            if (it.colorHex == -1) categoryFallbackColor else androidx.compose.ui.graphics.Color(
                                                it.colorHex
                                            )
                                        },
                                        noneLabel = if (parentSearchQuery.isBlank()) "None (Primary)" else null,
                                        onParentSelected = { id ->
                                            selectedParentId = id
                                            showParentPicker = false
                                        }
                                    )
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
                        onClick = {
                            onSave(
                                name,
                                selectedIcon,
                                displayColorHex.toColorInt(),
                                selectedParentId,
                                selectedNature
                            )
                        },
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
        Dialog(
            onDismissRequest = { showIdentityDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(32.dp),
                color = AppColors.panelSurface,
                tonalElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Visual Identity",
                            style = AppTextStyles.heroAmount.copy(fontSize = 20.sp)
                        )
                        IconButton(
                            onClick = { showIdentityDialog = false },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Close",
                                modifier = Modifier.size(20.dp),
                                tint = AppColors.contentSecondary
                            )
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
