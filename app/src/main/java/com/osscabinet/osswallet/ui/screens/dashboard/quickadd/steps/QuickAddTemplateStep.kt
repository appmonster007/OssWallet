package com.osscabinet.ossWallet.ui.screens.dashboard.quickadd.steps

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import com.osscabinet.ossWallet.ui.components.aeonHairlineBorder
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.ui.components.AeonSearchField
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.toOpaqueColor

/**
 * Step 1 — "Atelier Grid". Editorial template/category picker.
 *
 * Layout (top→bottom): segmented Templates/Categories toggle, a gradient-hairline
 * glass panel (the only scrollable region), and the shared search field. The caller
 * passes [modifier] carrying a weight so the panel flexes between the pinned
 * sheet header and footer.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickAddTemplateStep(
    templates: List<Template>,
    categories: List<Category>,
    selectedTemplateId: Long?,
    selectedCategoryId: Long?,
    isSheetSettled: Boolean,
    onTemplateSelected: (Template) -> Unit,
    onCategorySelected: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    val hasTemplates = templates.isNotEmpty()
    // Default to templates; fall back to categories only when none are available.
    // NOTE: if hasTemplates oscillates (e.g., brief empty emission on profile switch),
    // this effect would switch to categories and leave it there — acceptable since such
    // oscillations are unlikely while the Quick Add sheet is open.
    var showCategories by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(hasTemplates) {
        if (!hasTemplates) showCategories = true
    }
    var categoryNavStack by remember { mutableStateOf(emptyList<Long>()) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(query) {
        if (query.isNotBlank()) categoryNavStack = emptyList()
    }

    LaunchedEffect(isSheetSettled) {
        if (isSheetSettled) focusRequester.requestFocus()
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val numColumns = if (maxWidth > 400.dp) 5 else 4

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val overline = when {
                query.isNotBlank() -> "Results"
                showCategories -> "Categories"
                else -> "Frequent"
            }

            AtelierPanel(
                overline = overline,
                modifier = Modifier.weight(1f, fill = false),
            ) {
                if (!showCategories && hasTemplates) {
                    TemplatesGrid(
                        templates = templates,
                        categories = categories,
                        query = query,
                        numColumns = numColumns,
                        selectedTemplateId = selectedTemplateId,
                        onTemplateSelected = onTemplateSelected,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                } else {
                    CategoriesGrid(
                        categories = categories,
                        query = query,
                        numColumns = numColumns,
                        selectedTemplateId = selectedTemplateId,
                        selectedCategoryId = selectedCategoryId,
                        categoryNavStack = categoryNavStack,
                        onCategorySelected = onCategorySelected,
                        onDrillInto = { id ->
                            query = ""
                            categoryNavStack = categoryNavStack + id
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        },
                        onDrillOut = { categoryNavStack = categoryNavStack.dropLast(1) },
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (hasTemplates) {
                    AtelierModeToggle(
                        showCategories = showCategories,
                        onClick = {
                            showCategories = !showCategories
                            query = ""
                            categoryNavStack = emptyList()
                        }
                    )
                }

                AeonSearchField(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = "Search",
                    modifier = Modifier.weight(1f),
                    focusRequester = focusRequester
                )
            }
        }
    }
}

@Composable
private fun AtelierModeToggle(
    showCategories: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val label = if (showCategories) "CATEGORIES" else "TEMPLATES"
    val icon = if (showCategories) Icons.Rounded.Category else Icons.Rounded.Dashboard

    Surface(
        onClick = onClick,
        shape = shape,
        color = AppColors.recessedSurface,
        modifier = Modifier
            .height(48.dp)
            .aeonHairlineBorder(shape)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppColors.accent,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = AppTextStyles.sectionOverline,
                color = AppColors.accent
            )
        }
    }
}

@Composable
private fun ColumnScope.TemplatesGrid(
    templates: List<Template>,
    categories: List<Category>,
    query: String,
    numColumns: Int,
    selectedTemplateId: Long?,
    onTemplateSelected: (Template) -> Unit,
    modifier: Modifier = Modifier
) {
    val filtered = remember(templates, query) {
        if (query.isBlank()) templates.sortedByDescending { it.isStarred }
        else templates.filter { t ->
            t.name.contains(query, ignoreCase = true) ||
                    (t.note?.contains(query, ignoreCase = true) == true)
        }.sortedByDescending { it.isStarred }
    }

    if (filtered.isEmpty()) {
        EmptyState("No templates found")
        return
    }
    AtelierGrid(numColumns, modifier) {
        items(filtered, key = { it.id }) { template ->
            val category = categories.find { it.id == template.categoryId }
            AtelierTile(
                label = template.name,
                icon = getIconByName(category?.iconName),
                iconColor = category?.colorHex?.toOpaqueColor()
                    ?: AppColors.contentSecondary,
                isSelected = template.id == selectedTemplateId,
                onTap = { onTemplateSelected(template) }
            )
        }
    }
}

@Composable
private fun ColumnScope.CategoriesGrid(
    categories: List<Category>,
    query: String,
    numColumns: Int,
    selectedTemplateId: Long?,
    selectedCategoryId: Long?,
    categoryNavStack: List<Long>,
    onCategorySelected: (Category) -> Unit,
    onDrillInto: (Long) -> Unit,
    onDrillOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allUserCategories = remember(categories) {
        categories.filter { !Category.isSystemCategory(it.id) }
    }
    val childrenMap = remember(allUserCategories) {
        allUserCategories.groupBy { it.parentId }
    }
    val currentParentId = categoryNavStack.lastOrNull()
    val currentParent = remember(currentParentId, allUserCategories) {
        currentParentId?.let { pid -> allUserCategories.find { it.id == pid } }
    }

    if (query.isNotBlank()) {
        val searchResults = remember(allUserCategories, query) {
            allUserCategories.filter { it.name.contains(query, ignoreCase = true) }
                .sortedBy { it.name }
        }
        if (searchResults.isEmpty()) {
            EmptyState("No categories found")
            return
        }
        AtelierGrid(numColumns, modifier) {
            items(searchResults, key = { it.id }) { category ->
                AtelierTile(
                    label = category.name,
                    icon = getIconByName(category.iconName),
                    iconColor = category.colorHex.toOpaqueColor(),
                    isSelected = category.id == selectedCategoryId && selectedTemplateId == null,
                    hasChildren = childrenMap[category.id]?.isNotEmpty() == true,
                    onTap = { onCategorySelected(category) },
                    onLongPress = if (childrenMap[category.id]?.isNotEmpty() == true)
                        ({ onDrillInto(category.id) }) else null
                )
            }
        }
        return
    }

    AnimatedContent(
        targetState = currentParentId,
        modifier = modifier,
        transitionSpec = {
            if (targetState != null) {
                slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
            } else {
                slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
            }
        },
        label = "categoryDrillDown"
    ) { parentId ->
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (parentId != null && currentParent != null) {
                DrillBackHeader(
                    parent = currentParent,
                    onBack = onDrillOut,
                    onSelectParent = { onCategorySelected(currentParent) }
                )
            }
            val visibleItems = remember(parentId, childrenMap) {
                childrenMap[parentId]?.sortedBy { it.name } ?: emptyList()
            }
            if (visibleItems.isEmpty()) {
                EmptyState("No categories")
            } else {
                AtelierGrid(numColumns, Modifier.weight(1f, fill = false)) {
                    items(visibleItems, key = { it.id }) { category ->
                        AtelierTile(
                            label = category.name,
                            icon = getIconByName(category.iconName),
                            iconColor = category.colorHex.toOpaqueColor(),
                            isSelected = category.id == selectedCategoryId && selectedTemplateId == null,
                            hasChildren = childrenMap[category.id]?.isNotEmpty() == true,
                            onTap = { onCategorySelected(category) },
                            onLongPress = if (childrenMap[category.id]?.isNotEmpty() == true)
                                ({ onDrillInto(category.id) }) else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DrillBackHeader(
    parent: Category,
    onBack: () -> Unit,
    onSelectParent: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(28.dp)) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back to categories",
                tint = AppColors.contentSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Surface(
            onClick = onSelectParent,
            shape = RoundedCornerShape(12.dp),
            color = AppColors.recessedSurface,
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    getIconByName(parent.iconName),
                    contentDescription = null,
                    tint = parent.colorHex.toOpaqueColor(),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    parent.name,
                    style = AppTextStyles.managementItemTitle,
                    color = AppColors.contentPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "SELECT",
                    style = AppTextStyles.metaLabel,
                    color = AppColors.contentSecondary.copy(alpha = 0.7f)
                )
            }
        }
    }
}
