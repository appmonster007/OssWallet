package com.osscabinet.ossWallet.ui.screens.dashboard.quickadd.steps

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.components.GlassSurface
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.GlassVariant
import com.osscabinet.ossWallet.ui.theme.GlassTokens
import com.osscabinet.ossWallet.ui.theme.copyStyle

@Composable
fun QuickAddTemplateStep(
    templates: List<Template>,
    categories: List<Category>,
    selectedTemplateId: Long?,
    selectedCategoryId: Long?,
    onTemplateSelected: (Template) -> Unit,
    onCategorySelected: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var categoryQuery by remember { mutableStateOf("") }
    var showCategories by rememberSaveable { mutableStateOf(false) }
    // Stack of category IDs representing the current drill-down path; empty = root
    var categoryNavStack by remember { mutableStateOf(emptyList<Long>()) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(showCategories) {
        if (!showCategories) focusRequester.requestFocus()
        else {
            categoryNavStack = emptyList()
            categoryQuery = ""
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Segmented toggle
        SegmentedToggle(showCategories = showCategories, onToggle = { showCategories = it })

        if (!showCategories) {
            // ── Template mode ──────────────────────────────────────────────────────
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search by name or note...", style = AppTextStyles.formFieldHint) },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = AppColors.contentSecondary)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.primary,
                    unfocusedBorderColor = AppColors.divider.copy(alpha = 0.3f),
                    focusedContainerColor = AppColors.recessedSurface,
                    unfocusedContainerColor = AppColors.recessedSurface
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() })
            )

            val filtered = remember(templates, query) {
                if (query.isBlank()) templates.sortedByDescending { it.isStarred }
                else templates.filter { t ->
                    t.name.contains(query, ignoreCase = true) ||
                            (t.note?.contains(query, ignoreCase = true) == true)
                }.sortedByDescending { it.isStarred }
            }

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No templates found",
                        style = AppTextStyles.listItemSubtitle,
                        color = AppColors.contentSecondary.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filtered, key = { it.id }) { template ->
                        val category = categories.find { it.id == template.categoryId }
                        TemplateGridItem(
                            template = template,
                            category = category,
                            isSelected = template.id == selectedTemplateId,
                            onClick = {
                                keyboardController?.hide()
                                onTemplateSelected(template)
                            }
                        )
                    }
                }
            }
        } else {
            // ── Category mode ──────────────────────────────────────────────────────
            val allUserCategories = remember(categories) {
                categories.filter { !Category.isSystemCategory(it.id) }
            }
            // Map parentId (null = root) → children for multi-level drill-down
            val childrenMap = remember(allUserCategories) {
                allUserCategories.groupBy { it.parentId }
            }
            val currentParentId = categoryNavStack.lastOrNull()
            val currentParent = remember(currentParentId, allUserCategories) {
                currentParentId?.let { pid -> allUserCategories.find { it.id == pid } }
            }

            // Search bar
            OutlinedTextField(
                value = categoryQuery,
                onValueChange = { categoryQuery = it },
                placeholder = { Text("Search categories...", style = AppTextStyles.formFieldHint) },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = AppColors.contentSecondary)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.primary,
                    unfocusedBorderColor = AppColors.divider.copy(alpha = 0.3f),
                    focusedContainerColor = AppColors.recessedSurface,
                    unfocusedContainerColor = AppColors.recessedSurface
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() })
            )

            if (categoryQuery.isNotBlank()) {
                // Flat search across all levels
                val searchResults = remember(allUserCategories, categoryQuery) {
                    allUserCategories.filter { it.name.contains(categoryQuery, ignoreCase = true) }
                        .sortedBy { it.name }
                }
                if (searchResults.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No categories found",
                            style = AppTextStyles.listItemSubtitle,
                            color = AppColors.contentSecondary.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(searchResults, key = { it.id }) { category ->
                            val hasChildren = childrenMap[category.id]?.isNotEmpty() == true
                            CategoryGridItem(
                                category = category,
                                isSelected = category.id == selectedCategoryId && selectedTemplateId == null,
                                hasChildren = hasChildren,
                                onTap = { onCategorySelected(category) },
                                onLongPress = if (hasChildren) ({
                                    categoryQuery = ""
                                    categoryNavStack = categoryNavStack + category.id
                                }) else null
                            )
                        }
                    }
                }
            } else {
                // Hierarchical drill-down
                AnimatedContent(
                    targetState = currentParentId,
                    transitionSpec = {
                        if (targetState != null) {
                            slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                        } else {
                            slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                        }
                    },
                    label = "categoryDrillDown"
                ) { parentId ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Back header when drilled in
                        if (parentId != null && currentParent != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { categoryNavStack = categoryNavStack.dropLast(1) }) {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = "Back to categories",
                                        tint = AppColors.contentSecondary
                                    )
                                }
                                Surface(
                                    onClick = { onCategorySelected(currentParent) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = AppColors.recessedSurface,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val parentColor = remember(currentParent.colorHex) {
                                            Color(currentParent.colorHex or (0xFF shl 24).toInt())
                                        }
                                        Icon(
                                            getIconByName(currentParent.iconName),
                                            contentDescription = null,
                                            tint = parentColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            currentParent.name,
                                            style = AppTextStyles.formSwitchLabel,
                                            color = AppColors.contentPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            "Select",
                                            style = AppTextStyles.chartAxisLabel,
                                            color = AppColors.contentSecondary.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }

                        val visibleItems = remember(parentId, childrenMap) {
                            childrenMap[parentId]?.sortedBy { it.name } ?: emptyList()
                        }

                        if (visibleItems.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(80.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No categories",
                                    style = AppTextStyles.listItemSubtitle,
                                    color = AppColors.contentSecondary.copy(alpha = 0.6f)
                                )
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 88.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(visibleItems, key = { it.id }) { category ->
                                    val hasChildren = childrenMap[category.id]?.isNotEmpty() == true
                                    CategoryGridItem(
                                        category = category,
                                        isSelected = category.id == selectedCategoryId && selectedTemplateId == null,
                                        hasChildren = hasChildren,
                                        onTap = { onCategorySelected(category) },
                                        onLongPress = if (hasChildren) ({
                                            categoryNavStack = categoryNavStack + category.id
                                        }) else null
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun SegmentedToggle(showCategories: Boolean, onToggle: (Boolean) -> Unit) {
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        glassVariant = GlassVariant.Item,
        fallbackColor = AppColors.recessedSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(false to "Templates", true to "Categories").forEach { (isCategories, label) ->
                val isSelected = showCategories == isCategories
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) AppColors.primary else Color.Transparent,
                    label = "segBg$label"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) AppColors.onPrimary else AppColors.contentSecondary,
                    label = "segText$label"
                )
                Surface(
                    onClick = { onToggle(isCategories) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = bgColor
                ) {
                    Text(
                        text = label,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        textAlign = TextAlign.Center,
                        style = AppTextStyles.formSwitchLabel,
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
private fun TemplateGridItem(
    template: Template,
    category: Category?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Always derive color from category
    val fallbackColor = AppColors.recessedSurface
    val bgColor = remember(category?.colorHex) {
        category?.colorHex
            ?.let { Color(it or (0xFF shl 24).toInt()) }
            ?: fallbackColor
    }
    val iconTint = remember(bgColor) { bgColor.darkenForIcon() }
    val primaryColor = AppColors.primary
    val onPrimaryColor = AppColors.onPrimary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        GlassSurface(
            modifier = Modifier.size(72.dp),
            shape = RoundedCornerShape(20.dp),
            glassVariant = GlassVariant.Item,
            hazeStyle = GlassTokens.itemStyle().copyStyle(bgColor = bgColor, alpha = 0.18f),
            fallbackColor = bgColor.copy(alpha = 0.18f),
            borderColor = if (isSelected) primaryColor else null,
            borderWidth = if (isSelected) 2.dp else 0.dp
        ) {
            Surface(
                onClick = onClick,
                modifier = Modifier.fillMaxSize(),
                color = Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = getIconByName(category?.iconName),
                        contentDescription = template.name,
                        tint = iconTint,
                        modifier = Modifier.size(28.dp)
                    )
                    if (isSelected) SelectedBadge(primaryColor, onPrimaryColor)
                }
            }
        }
        Text(
            text = template.name,
            style = AppTextStyles.chartAxisLabel,
            color = AppColors.contentPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryGridItem(
    category: Category,
    isSelected: Boolean,
    hasChildren: Boolean,
    onTap: () -> Unit,
    onLongPress: (() -> Unit)?
) {
    val bgColor = remember(category.colorHex) {
        Color(category.colorHex or (0xFF shl 24).toInt())
    }
    val iconTint = remember(bgColor) { bgColor.darkenForIcon() }
    val primaryColor = AppColors.primary
    val onPrimaryColor = AppColors.onPrimary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        GlassSurface(
            modifier = Modifier.size(72.dp),
            shape = RoundedCornerShape(20.dp),
            glassVariant = GlassVariant.Item,
            hazeStyle = GlassTokens.itemStyle().copyStyle(bgColor = bgColor, alpha = 0.18f),
            fallbackColor = bgColor.copy(alpha = 0.18f),
            borderColor = if (isSelected) primaryColor else null,
            borderWidth = if (isSelected) 2.dp else 0.dp
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .combinedClickable(
                        onClick = onTap,
                        onLongClick = onLongPress
                    ),
                color = Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = getIconByName(category.iconName),
                        contentDescription = category.name,
                        tint = iconTint,
                        modifier = Modifier.size(28.dp)
                    )
                    if (isSelected) SelectedBadge(primaryColor, onPrimaryColor)
                    // Long-press hint badge (only for parents with children)
                    if (hasChildren && !isSelected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AppColors.contentSecondary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.KeyboardArrowRight,
                                contentDescription = "Long press to see sub-categories",
                                tint = AppColors.contentSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
        Text(
            text = category.name,
            style = AppTextStyles.chartAxisLabel,
            color = AppColors.contentPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SelectedBadge(primaryColor: Color, onPrimaryColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(primaryColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = onPrimaryColor,
                modifier = Modifier.size(10.dp)
            )
        }
    }
}

/** Darkens a color for use as an icon tint against a light tinted background. */
private fun Color.darkenForIcon(): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(this.copy(alpha = 1f).hashCode(), hsv)
    // Parse properly
    val argb = android.graphics.Color.argb(255, (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt())
    android.graphics.Color.colorToHSV(argb, hsv)
    hsv[2] = if (hsv[2] > 0.5f) hsv[2] - 0.3f else hsv[2] + 0.4f
    return Color(android.graphics.Color.HSVToColor(hsv))
}
