package com.osscabinet.ossWallet.ui.screens.categories.list

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.service.filter.TransactionFilterService
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.screens.categories.list.components.CategoryEditSheet
import com.osscabinet.ossWallet.ui.screens.categories.CategoryViewModel
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.OutlineLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListScreen(
    viewModel: CategoryViewModel,
    onBackClick: () -> Unit
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCategoryForEdit by remember { mutableStateOf<Category?>(null) }

    BackHandler(enabled = showAddDialog || selectedCategoryForEdit != null) {
        showAddDialog = false
        selectedCategoryForEdit = null
    }

    var navTriggered by remember { mutableStateOf(false) }
    val navigate: (() -> Unit) -> Unit = { action ->
        if (!navTriggered) {
            navTriggered = true
            action()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Manage Categories") },
                navigationIcon = {
                    IconButton(onClick = { navigate(onBackClick) }) {
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
                .padding(horizontal = AeonTheme.dimensions.screenPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg)
        ) {
            Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingLg))
            AeonHierarchicalHybridSelector(
                items = categories,
                mode = AeonSelectMode.MANAGEMENT,
                selectedIds = emptySet(),
                onSelectionChange = { },
                idMapper = { it.id },
                labelMapper = { it.name },
                parentIdMapper = { it.parentId },
                iconMapper = { AppIconMap[it.iconName] },
                colorMapper = { if (it.colorHex == -1) OutlineLight else Color(it.colorHex) },
                onEditClick = { selectedCategoryForEdit = it },
                onAddChildClick = { parent ->
                    selectedCategoryForEdit = Category(
                        id = 0,
                        name = "",
                        iconName = "Other",
                        colorHex = parent.colorHex,
                        isDefault = false,
                        parentId = parent.id
                    )
                }
            )
            Spacer(modifier = Modifier.height(scrollBottomPadding()))
        }
    }

    if (showAddDialog || selectedCategoryForEdit != null) {
        val editingCategory = selectedCategoryForEdit
        val descendants: Set<Long> = if (editingCategory != null && editingCategory.id != 0L) {
            TransactionFilterService.expandCategoryIds(setOf(editingCategory.id), categories)
        } else {
            emptySet()
        }
        val eligibleParents = categories.filter {
            it.id != (editingCategory?.id ?: 0L) && !descendants.contains(it.id)
        }

        // Sort eligible parents hierarchically for the dropdown
        val sortedEligibleParents = mutableListOf<Category>()
        fun addChildren(parentId: Long?) {
            eligibleParents.filter { it.parentId == parentId }.sortedBy { it.name }.forEach {
                sortedEligibleParents.add(it)
                addChildren(it.id)
            }
        }
        addChildren(null)
        eligibleParents.forEach { if (it !in sortedEligibleParents) sortedEligibleParents.add(it) }

        CategoryEditSheet(
            category = editingCategory,
            parentCategories = sortedEligibleParents,
            onDismiss = {
                showAddDialog = false
                selectedCategoryForEdit = null
            },
            onSave = { name, icon, color, parentId, nature ->
                viewModel.upsertCategory(
                    Category(
                        id = editingCategory?.id ?: 0,
                        name = name,
                        iconName = icon,
                        colorHex = color,
                        parentId = parentId,
                        isDefault = editingCategory?.isDefault ?: false,
                        nature = nature
                    )
                )
                showAddDialog = false
                selectedCategoryForEdit = null
            },
            onDelete = if (editingCategory != null && editingCategory.id != 0L) {
                {
                    viewModel.deleteCategory(editingCategory)
                    showAddDialog = false
                    selectedCategoryForEdit = null
                }
            } else null
        )
    }
}
