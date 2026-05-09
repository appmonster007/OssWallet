package com.osscabinet.ossWallet.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.service.filter.TransactionFilterService
import com.osscabinet.ossWallet.ui.components.AccountIconMap
import com.osscabinet.ossWallet.ui.components.AeonHierarchicalHybridSelector
import com.osscabinet.ossWallet.ui.components.AeonSelectMode
import com.osscabinet.ossWallet.ui.components.CategoryEditSheet
import com.osscabinet.ossWallet.ui.viewmodel.TransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(
    viewModel: TransactionViewModel,
    onBackClick: () -> Unit
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCategoryForEdit by remember { mutableStateOf<Category?>(null) }

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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            AeonHierarchicalHybridSelector(
                items = categories,
                mode = AeonSelectMode.MANAGEMENT,
                selectedIds = emptySet(),
                onSelectionChange = { },
                idMapper = { it.id },
                labelMapper = { it.name },
                parentIdMapper = { it.parentId },
                iconMapper = { AccountIconMap[it.iconName] },
                colorMapper = { if (it.colorHex == -1) Color(0xFF9E9E9E) else Color(it.colorHex) },
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
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showAddDialog || selectedCategoryForEdit != null) {
        val editingCategory = selectedCategoryForEdit
        val descendants: List<Long> = if (editingCategory != null && editingCategory.id != 0L) {
            TransactionFilterService.expandCategoryIds(listOf(editingCategory.id), categories)
        } else {
            emptyList()
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
        // Add any that were missed (shouldn't happen with correct logic but good for safety)
        eligibleParents.forEach { if (it !in sortedEligibleParents) sortedEligibleParents.add(it) }

        CategoryEditSheet(
            category = editingCategory,
            parentCategories = sortedEligibleParents,
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
