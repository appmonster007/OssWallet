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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.service.TransactionFilterService
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.util.*
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
            AeonCategoryList(
                categories = categories,
                isManagementMode = true,
                onEditClick = { selectedCategoryForEdit = it },
                onAddSubCategoryClick = { parent ->
                    selectedCategoryForEdit = Category(
                        id = 0,
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
        val descendants: List<Long> = if (editingCategory != null && editingCategory.id != 0L) {
            TransactionFilterService.expandCategoryIds(listOf(editingCategory.id), categories)
        } else {
            emptyList()
        }
        val eligibleParents = categories.filter { it.id != (editingCategory?.id ?: 0L) && !descendants.contains(it.id) }
        
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
            }
        )
    }
}
