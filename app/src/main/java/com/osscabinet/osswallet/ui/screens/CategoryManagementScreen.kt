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
