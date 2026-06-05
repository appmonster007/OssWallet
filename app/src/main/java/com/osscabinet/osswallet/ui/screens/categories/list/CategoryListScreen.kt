package com.osscabinet.ossWallet.ui.screens.categories.list

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.service.filter.TransactionFilterService
import com.osscabinet.ossWallet.ui.components.AeonHazeProvider
import com.osscabinet.ossWallet.ui.components.AeonHierarchicalHybridSelector
import com.osscabinet.ossWallet.ui.components.AeonSelectMode
import com.osscabinet.ossWallet.ui.components.AeonTopAppBar
import com.osscabinet.ossWallet.ui.components.AeonTopBarActionPill
import com.osscabinet.ossWallet.ui.components.AeonTopBarNavTitlePill
import com.osscabinet.ossWallet.ui.components.AeonTopBarSection
import com.osscabinet.ossWallet.ui.components.AppIconMap
import com.osscabinet.ossWallet.ui.components.AppBarIconButton
import com.osscabinet.ossWallet.ui.components.scrollBottomPadding
import com.osscabinet.ossWallet.ui.screens.categories.CategoryViewModel
import com.osscabinet.ossWallet.ui.screens.categories.list.components.CategoryEditSheet
import com.osscabinet.ossWallet.ui.theme.AeonSize
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.HazeLayer
import com.osscabinet.ossWallet.ui.theme.OutlineLight
import dev.chrisbanes.haze.hazeSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListScreen(
    viewModel: CategoryViewModel,
    actions: CategoryListActions
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

    AeonHazeProvider { hazeLayers ->
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = Color.Transparent
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppColors.screenBackground)
                        .hazeSource(hazeLayers.states[HazeLayer.Overlay])
                        .statusBarsPadding()
                        .padding(
                            top = AeonSize.D64.value + AeonTheme.dimensions.screenPadding,
                            bottom = innerPadding.calculateBottomPadding()
                        )
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

            AeonTopAppBar(
                left = AeonTopBarSection(
                    content = {
                        AeonTopBarNavTitlePill(
                            title = "Manage Categories",
                            onBackClick = { navigate(actions.onBackClick) }
                        )
                    }
                ),
                right = AeonTopBarSection(
                    content = {
                        AeonTopBarActionPill {
                            AppBarIconButton(
                                icon = Icons.Rounded.Add,
                                onClick = { showAddDialog = true }
                            )
                        }
                    }
                )
            )
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

data class CategoryListActions(
    val onBackClick: () -> Unit
)
