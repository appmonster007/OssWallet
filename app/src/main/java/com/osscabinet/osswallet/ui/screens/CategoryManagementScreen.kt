package com.osscabinet.ossWallet.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.data.local.entity.CategoryEntity
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.util.*
import com.osscabinet.ossWallet.ui.viewmodel.TransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(
    viewModel: TransactionViewModel,
    onBackClick: () -> Unit
) {
    val categories by viewModel.categories.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCategoryForEdit by remember { mutableStateOf<CategoryEntity?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Manage Categories") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add Category")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            AeonCategoryList(
                categories = categories,
                isManagementMode = true,
                onEditClick = { selectedCategoryForEdit = it },
                onAddSubCategoryClick = { parent ->
                    selectedCategoryForEdit = CategoryEntity(
                        name = "",
                        iconName = "Other",
                        colorHex = parent.colorHex,
                        isDefault = false,
                        parentId = parent.id
                    )
                },
                onCategorySelected = { /* Not used in management mode */ }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showAddDialog || selectedCategoryForEdit != null) {
        val editingCategory = selectedCategoryForEdit
        CategoryEditSheet(
            category = editingCategory,
            parentCategories = categories.filter { it.parentId == null },
            onDismiss = {
                showAddDialog = false
                selectedCategoryForEdit = null
            },
            onSave = { name, icon, color, parentId ->
                viewModel.upsertCategory(
                    id = editingCategory?.id ?: 0,
                    name = name,
                    iconName = icon,
                    colorHex = color,
                    parentId = parentId,
                    isDefault = editingCategory?.isDefault ?: false
                )
                showAddDialog = false
                selectedCategoryForEdit = null
            }
        )
    }
}

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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
                subtitle = if (selectedParentId != null) "Sub-category of ${parentCategories.find { it.id == selectedParentId }?.name}" else "Root Category",
                iconName = selectedIcon,
                accentColorHex = displayColorHex
            )

            // Identity Section
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("IDENTITY", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.outline, letterSpacing = 1.sp)
                
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
                    value = parentCategory?.name ?: "None (Root Level)",
                    onClick = { showParentPicker = true },
                    trailingIcon = Icons.Rounded.KeyboardArrowDown,
                    iconContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                )

                if (showParentPicker) {
                    ModalBottomSheet(
                        onDismissRequest = { showParentPicker = false },
                        containerColor = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
                            Text("SELECT PARENT", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(24.dp))
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                item {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth().clickable { selectedParentId = null; showParentPicker = false },
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (selectedParentId == null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                    ) {
                                        Text("None (Root Level)", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
                                    }
                                }
                                items(parentCategories.filter { it.id != (category?.id ?: 0L) }) { parent ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth().clickable { selectedParentId = parent.id; showParentPicker = false },
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (selectedParentId == parent.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                    ) {
                                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                AccountIconMap[parent.iconName] ?: Icons.Rounded.Category,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = Color(parent.colorHex)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(parent.name, style = MaterialTheme.typography.bodyLarge)
                                        }
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
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
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
                onColorSelected = { if (selectedParentId == null) selectedColorHex = it }
            )
        }
    }
}
