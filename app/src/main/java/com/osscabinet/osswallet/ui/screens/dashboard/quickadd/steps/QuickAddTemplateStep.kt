package com.osscabinet.ossWallet.ui.screens.dashboard.quickadd.steps

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Category
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.components.GlassSurface
import com.osscabinet.ossWallet.ui.components.AeonSearchField
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.GlassVariant
import com.osscabinet.ossWallet.ui.theme.GlassTokens
import com.osscabinet.ossWallet.ui.theme.copyStyle

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
    var showCategories by rememberSaveable { mutableStateOf(false) }
    // Stack of category IDs representing the current drill-down path; empty = root
    var categoryNavStack by remember { mutableStateOf(emptyList<Long>()) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // Clear drill-down stack when user starts typing (flat search takes over)
    LaunchedEffect(query) {
        if (query.isNotBlank()) categoryNavStack = emptyList()
    }

    LaunchedEffect(isSheetSettled) {
        if (isSheetSettled) focusRequester.requestFocus()
    }

    val columnSpacing  = 7.dp
    val toggleHeight   = 40.dp
    val tileSize       = 56.dp
    val tileCorner     = 14.dp
    val tileIconSize   = 20.dp
    val gridSpacing    = 6.dp
    val itemSpacing    = 4.dp
    val emptyHeightSm  = 60.dp
    val badgeSize      = 12.dp
    val badgeIconSize  = 8.dp
    val childBadgeSize = 12.dp
    val childArrowSize = 9.dp

    // 2-row max; 1 row while the keyboard is open so the sheet fits above it
    val imeVisible = WindowInsets.isImeVisible
    val rowHeight    = tileSize + itemSpacing + 20.dp
    val maxGridHeight = if (imeVisible) rowHeight else (rowHeight * 2) + gridSpacing

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val numColumns = if (maxWidth > 400.dp) 5 else 4

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(columnSpacing)
        ) {
        // ── Grid content (above toggle) ────────────────────────────────────────
        if (!showCategories) {
            val filtered = remember(templates, query) {
                if (query.isBlank()) templates.sortedByDescending { it.isStarred }
                else templates.filter { t ->
                    t.name.contains(query, ignoreCase = true) ||
                            (t.note?.contains(query, ignoreCase = true) == true)
                }.sortedByDescending { it.isStarred }
            }

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
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
                    columns = GridCells.Fixed(numColumns),
                    verticalArrangement = Arrangement.spacedBy(gridSpacing),
                    horizontalArrangement = Arrangement.spacedBy(gridSpacing),
                    contentPadding = PaddingValues(bottom = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxGridHeight)
                ) {
                    items(filtered, key = { it.id }) { template ->
                        val category = categories.find { it.id == template.categoryId }
                        TemplateGridItem(
                            template = template,
                            category = category,
                            isSelected = template.id == selectedTemplateId,
                            onClick = { onTemplateSelected(template) },
                            tileSize = tileSize,
                            tileCorner = tileCorner,
                            tileIconSize = tileIconSize,
                            itemSpacing = itemSpacing,
                            badgeSize = badgeSize,
                            badgeIconSize = badgeIconSize
                        )
                    }
                }
            }
        } else {
            // ── Category mode ──────────────────────────────────────────────────
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
                // Flat search across all levels
                val searchResults = remember(allUserCategories, query) {
                    allUserCategories.filter { it.name.contains(query, ignoreCase = true) }
                        .sortedBy { it.name }
                }
                if (searchResults.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
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
                        columns = GridCells.Fixed(numColumns),
                        verticalArrangement = Arrangement.spacedBy(gridSpacing),
                        horizontalArrangement = Arrangement.spacedBy(gridSpacing),
                        contentPadding = PaddingValues(bottom = 4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = maxGridHeight)
                    ) {
                        items(searchResults, key = { it.id }) { category ->
                            val hasChildren = childrenMap[category.id]?.isNotEmpty() == true
                            CategoryGridItem(
                                category = category,
                                isSelected = category.id == selectedCategoryId && selectedTemplateId == null,
                                hasChildren = hasChildren,
                                onTap = { onCategorySelected(category) },
                                onLongPress = if (hasChildren) ({
                                    query = ""
                                    categoryNavStack = categoryNavStack + category.id
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                }) else null,
                                tileSize = tileSize,
                                tileCorner = tileCorner,
                                tileIconSize = tileIconSize,
                                itemSpacing = itemSpacing,
                                badgeSize = badgeSize,
                                badgeIconSize = badgeIconSize,
                                childBadgeSize = childBadgeSize,
                                childArrowSize = childArrowSize
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
                    Column(verticalArrangement = Arrangement.spacedBy(columnSpacing)) {
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
                                            Color(currentParent.colorHex or (0xFF shl 24))
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
                                modifier = Modifier.fillMaxWidth().height(emptyHeightSm),
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
                                columns = GridCells.Fixed(numColumns),
                                verticalArrangement = Arrangement.spacedBy(gridSpacing),
                                horizontalArrangement = Arrangement.spacedBy(gridSpacing),
                                contentPadding = PaddingValues(bottom = 4.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = maxGridHeight)
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
                                            keyboardController?.hide()
                                            focusManager.clearFocus()
                                        }) else null,
                                        tileSize = tileSize,
                                        tileCorner = tileCorner,
                                        tileIconSize = tileIconSize,
                                        itemSpacing = itemSpacing,
                                        badgeSize = badgeSize,
                                        badgeIconSize = badgeIconSize,
                                        childBadgeSize = childBadgeSize,
                                        childArrowSize = childArrowSize
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Segmented toggle (below grid, above search bar) ────────────────────
        SegmentedToggle(showCategories = showCategories, onToggle = { showCategories = it }, height = toggleHeight)

        // ── Single shared search bar anchored to keyboard top ──────────────────
        AeonSearchField(
            query = query,
            onQueryChange = { query = it },
            placeholder = "Search ...",
            modifier = Modifier.fillMaxWidth(),
            focusRequester = focusRequester
        )
    }
    }
}

@Composable
private fun SegmentedToggle(showCategories: Boolean, onToggle: (Boolean) -> Unit, height: androidx.compose.ui.unit.Dp = 52.dp) {
    val haptics = LocalHapticFeedback.current
    val dims = AeonTheme.dimensions

    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        shape = RoundedCornerShape(dims.componentCornerRadius),
        glassVariant = GlassVariant.Item,
        fallbackColor = AppColors.recessedSurface
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(dims.spacingXs)
        ) {
            val maxWidth = maxWidth
            val itemWidth = maxWidth / 2

            // Sliding Indicator
            Box(
                modifier = Modifier
                    .padding(dims.spacingXs / 2)
                    .width(itemWidth)
                    .fillMaxHeight()
                    .graphicsLayer {
                        translationX = if (showCategories) itemWidth.toPx() else 0f
                    }
                    .background(AppColors.primary, RoundedCornerShape(dims.componentCornerRadius - 4.dp))
            )

            Row(modifier = Modifier.fillMaxSize()) {
                // Templates Tab
                SegmentedTab(
                    label = "Templates",
                    icon = Icons.Rounded.Dashboard,
                    isSelected = !showCategories,
                    onClick = {
                        if (showCategories) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggle(false)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                // Categories Tab
                SegmentedTab(
                    label = "Categories",
                    icon = Icons.Rounded.Category,
                    isSelected = showCategories,
                    onClick = {
                        if (!showCategories) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggle(true)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SegmentedTab(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor by animateColorAsState(
        targetValue = if (isSelected) AppColors.onPrimary else AppColors.contentSecondary,
        label = "textColor"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius - 4.dp))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm)
            )
            Text(
                text = label,
                style = AppTextStyles.formSwitchLabel,
                color = textColor
            )
        }
    }
}

@Composable
private fun TemplateGridItem(
    template: Template,
    category: Category?,
    isSelected: Boolean,
    onClick: () -> Unit,
    tileSize: androidx.compose.ui.unit.Dp = 72.dp,
    tileCorner: androidx.compose.ui.unit.Dp = 20.dp,
    tileIconSize: androidx.compose.ui.unit.Dp = 28.dp,
    itemSpacing: androidx.compose.ui.unit.Dp = 6.dp,
    badgeSize: androidx.compose.ui.unit.Dp = 16.dp,
    badgeIconSize: androidx.compose.ui.unit.Dp = 10.dp
) {
    // Always derive color from category
    val fallbackColor = AppColors.recessedSurface
    val bgColor = remember(category?.colorHex) {
        category?.colorHex
            ?.let { Color(it or (0xFF shl 24)) }
            ?: fallbackColor
    }
    val iconTint = remember(bgColor) { bgColor.darkenForIcon() }
    val primaryColor = AppColors.primary
    val onPrimaryColor = AppColors.onPrimary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(itemSpacing)
    ) {
        GlassSurface(
            modifier = Modifier.size(tileSize),
            shape = RoundedCornerShape(tileCorner),
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
                shape = RoundedCornerShape(tileCorner)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = getIconByName(category?.iconName),
                        contentDescription = template.name,
                        tint = iconTint,
                        modifier = Modifier.size(tileIconSize)
                    )
                    if (isSelected) SelectedBadge(primaryColor, onPrimaryColor, badgeSize, badgeIconSize)
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
    onLongPress: (() -> Unit)?,
    tileSize: androidx.compose.ui.unit.Dp = 72.dp,
    tileCorner: androidx.compose.ui.unit.Dp = 20.dp,
    tileIconSize: androidx.compose.ui.unit.Dp = 28.dp,
    itemSpacing: androidx.compose.ui.unit.Dp = 6.dp,
    badgeSize: androidx.compose.ui.unit.Dp = 16.dp,
    badgeIconSize: androidx.compose.ui.unit.Dp = 10.dp,
    childBadgeSize: androidx.compose.ui.unit.Dp = 16.dp,
    childArrowSize: androidx.compose.ui.unit.Dp = 12.dp
) {
    val bgColor = remember(category.colorHex) {
        Color(category.colorHex or (0xFF shl 24))
    }
    val iconTint = remember(bgColor) { bgColor.darkenForIcon() }
    val primaryColor = AppColors.primary
    val onPrimaryColor = AppColors.onPrimary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(itemSpacing)
    ) {
        GlassSurface(
            modifier = Modifier.size(tileSize),
            shape = RoundedCornerShape(tileCorner),
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
                shape = RoundedCornerShape(tileCorner)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = getIconByName(category.iconName),
                        contentDescription = category.name,
                        tint = iconTint,
                        modifier = Modifier.size(tileIconSize)
                    )
                    if (isSelected) SelectedBadge(primaryColor, onPrimaryColor, badgeSize, badgeIconSize)
                    if (hasChildren && !isSelected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(childBadgeSize)
                                .clip(RoundedCornerShape(childBadgeSize / 2))
                                .background(AppColors.contentSecondary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = "Long press to see sub-categories",
                                tint = AppColors.contentSecondary,
                                modifier = Modifier.size(childArrowSize)
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
private fun SelectedBadge(
    primaryColor: Color,
    onPrimaryColor: Color,
    badgeSize: androidx.compose.ui.unit.Dp = 10.dp,
    iconSize: androidx.compose.ui.unit.Dp = 8.dp
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        Box(
            modifier = Modifier
                .size(badgeSize)
                .clip(RoundedCornerShape(badgeSize / 2))
                .background(primaryColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = onPrimaryColor,
                modifier = Modifier.size(iconSize)
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

@Preview(showBackground = true)
@Composable
fun PreviewQuickAddTemplateStep() {
    val mockCategories = listOf(
        Category(1, "Food", "Restaurant", -16737025, false, null),
        Category(2, "Transport", "DirectionsBus", -16737025, false, null),
        Category(3, "Health", "Favorite", -16737025, false, null)
    )
    val mockTemplates = listOf(
        Template(1, "Lunch", 1, java.math.BigDecimal("15.00"), com.osscabinet.ossWallet.domain.model.EntryMode.EXPENSE, currency = "USD", accountId = "1"),
        Template(2, "Uber", 2, java.math.BigDecimal("25.00"), com.osscabinet.ossWallet.domain.model.EntryMode.EXPENSE, currency = "USD", accountId = "1"),
        Template(3, "Gym", 3, java.math.BigDecimal("50.00"), com.osscabinet.ossWallet.domain.model.EntryMode.EXPENSE, currency = "USD", accountId = "1")
    )

    OssWalletTheme {
        Box(modifier = Modifier.background(AppColors.background)) {
            QuickAddTemplateStep(
                templates = mockTemplates,
                categories = mockCategories,
                selectedTemplateId = 1L,
                selectedCategoryId = null,
                isSheetSettled = true,
                onTemplateSelected = {},
                onCategorySelected = {}
            )
        }
    }
}


