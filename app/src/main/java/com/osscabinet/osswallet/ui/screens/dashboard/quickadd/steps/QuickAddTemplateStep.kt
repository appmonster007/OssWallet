package com.osscabinet.ossWallet.ui.screens.dashboard.quickadd.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.ui.components.AeonSearchField
import com.osscabinet.ossWallet.ui.components.aeonHairlineBorder
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.screens.dashboard.quickadd.QuickAddResult
import com.osscabinet.ossWallet.ui.screens.dashboard.quickadd.QuickAddSection
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.toOpaqueColor
import java.math.BigDecimal

enum class QuickAddMode(val label: String, val icon: ImageVector) {
    ALL("ALL", Icons.Rounded.AllInclusive),
    TEMPLATES("TEMPLATES", Icons.Rounded.Dashboard),
    CATEGORIES("CATEGORIES", Icons.Rounded.Category),
    SUGGESTIONS("SUGGESTIONS", Icons.Rounded.History)
}

/**
 * Step 1 — "Atelier Grid". Editorial unified template/suggestion/category picker.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickAddTemplateStep(
    sections: List<QuickAddSection>,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    selectedTemplateId: Long?,
    selectedCategoryId: Long?,
    isSheetSettled: Boolean,
    onTemplateSelected: (Template) -> Unit,
    onSuggestionSelected: (QuickAddResult.SuggestionItem) -> Unit,
    onCategorySelected: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    var activeMode by rememberSaveable { mutableStateOf(QuickAddMode.ALL) }
    var categoryNavStack by rememberSaveable { mutableStateOf(emptyList<Long>()) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) categoryNavStack = emptyList()
    }

    LaunchedEffect(isSheetSettled) {
        if (isSheetSettled) focusRequester.requestFocus()
    }

    val allCategories = remember(sections) {
        sections.flatMap { it.items }
            .filterIsInstance<QuickAddResult.CategoryItem>()
            .map { it.category }
    }
    val childrenMap = remember(allCategories) {
        allCategories.groupBy { it.parentId }
    }

    val filteredSections = remember(sections, activeMode) {
        when (activeMode) {
            QuickAddMode.ALL -> sections
            QuickAddMode.TEMPLATES -> sections.filter { it.title == "TEMPLATES" }
            QuickAddMode.CATEGORIES -> sections.filter { it.title == "CATEGORIES" }
            QuickAddMode.SUGGESTIONS -> sections.filter { it.title == "SUGGESTIONS" }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val numColumns = if (maxWidth > 400.dp) 5 else 4

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val overline = when {
                searchQuery.isNotBlank() -> "Results"
                activeMode != QuickAddMode.ALL -> activeMode.label
                else -> "Atelier"
            }

            AtelierPanel(
                header = {
                    if (searchQuery.isNotEmpty()) {
                        PanelOverline(overline)
                    }
                },
                modifier = Modifier.weight(1f, fill = false),
            ) {
                if (filteredSections.isEmpty()) {
                    val emptyMsg = when (activeMode) {
                        QuickAddMode.TEMPLATES -> "No templates found"
                        QuickAddMode.CATEGORIES -> "No categories found"
                        QuickAddMode.SUGGESTIONS -> if (searchQuery.isBlank()) "Search to see suggestions" else "No matches found"
                        else -> if (searchQuery.isBlank()) "No items found" else "No matches found"
                    }
                    EmptyState(emptyMsg)
                } else {
                    AtelierGrid(numColumns, Modifier.weight(1f, fill = false)) {
                        filteredSections.forEach { section ->
                            val isCategorySection = section.title == "CATEGORIES"

                            // Section Header (Only show in ALL mode)
                            if (activeMode == QuickAddMode.ALL) {
                                item(span = { GridItemSpan(numColumns) }) {
                                    PanelOverline(section.title)
                                }
                            }

                            if (isCategorySection && searchQuery.isBlank() && categoryNavStack.isNotEmpty()) {
                                val currentParentId = categoryNavStack.last()
                                val currentParent = allCategories.find { it.id == currentParentId }
                                if (currentParent != null) {
                                    item(span = { GridItemSpan(numColumns) }) {
                                        DrillBackHeader(
                                            parent = currentParent,
                                            onBack = { categoryNavStack = categoryNavStack.dropLast(1) },
                                            onSelectParent = { onCategorySelected(currentParent) }
                                        )
                                    }
                                }
                            }

                            val itemsToDisplay = if (isCategorySection && searchQuery.isBlank()) {
                                val currentParentId = categoryNavStack.lastOrNull()
                                section.items.filter { item ->
                                    item is QuickAddResult.CategoryItem && item.category.parentId == currentParentId
                                }
                            } else {
                                section.items
                            }

                            items(
                                items = itemsToDisplay,
                                span = { GridItemSpan(1) }
                            ) { item ->
                                when (item) {
                                    is QuickAddResult.TemplateItem -> {
                                        AtelierTile(
                                            label = item.template.name,
                                            icon = getIconByName(item.iconName),
                                            iconColor = item.colorHex.toOpaqueColor(),
                                            isSelected = item.template.id == selectedTemplateId,
                                            onTap = { onTemplateSelected(item.template) }
                                        )
                                    }

                                    is QuickAddResult.SuggestionItem -> {
                                        AtelierTile(
                                            label = item.label,
                                            icon = getIconByName(item.iconName),
                                            iconColor = item.colorHex.toOpaqueColor(),
                                            isSelected = false,
                                            onTap = { onSuggestionSelected(item) }
                                        )
                                    }

                                    is QuickAddResult.CategoryItem -> {
                                        val hasChildren = searchQuery.isBlank() && 
                                                childrenMap[item.category.id]?.isNotEmpty() == true
                                        AtelierTile(
                                            label = item.category.name,
                                            icon = getIconByName(item.category.iconName),
                                            iconColor = item.category.colorHex.toOpaqueColor(),
                                            isSelected = item.category.id == selectedCategoryId && selectedTemplateId == null,
                                            hasChildren = hasChildren,
                                            onTap = { 
                                                if (hasChildren && !Category.isSystemCategory(item.category.id)) {
                                                    categoryNavStack = categoryNavStack + item.category.id
                                                    keyboardController?.hide()
                                                    focusManager.clearFocus()
                                                } else {
                                                    onCategorySelected(item.category)
                                                }
                                            },
                                            onLongPress = { onCategorySelected(item.category) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AtelierModeToggle(
                    mode = activeMode,
                    onClick = {
                        val nextIndex = (activeMode.ordinal + 1) % QuickAddMode.entries.size
                        activeMode = QuickAddMode.entries[nextIndex]
                    }
                )

                AeonSearchField(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChanged,
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
    mode: QuickAddMode,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
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
                imageVector = mode.icon,
                contentDescription = null,
                tint = AppColors.accent,
                modifier = Modifier.size(16.dp)
            )
//            Text(
//                text = mode.label,
//                style = AppTextStyles.sectionOverline,
//                color = AppColors.accent
//            )
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
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

@Preview(showBackground = true, widthDp = 360)
@Composable
fun PreviewQuickAddTemplateStep() {
    val mockTemplates = listOf(
        Template(id = 1, name = "Starbucks", categoryId = 1, amount = BigDecimal("5.50"), entryMode = EntryMode.EXPENSE, currency = "USD", accountId = "1"),
        Template(id = 2, name = "Rent", categoryId = 2, amount = BigDecimal("1200.00"), entryMode = EntryMode.EXPENSE, currency = "USD", accountId = "2", isStarred = true)
    )

    val mockCategories = listOf(
        Category(id = 1, name = "Coffee", iconName = "Coffee", colorHex = -123456, isDefault = false, parentId = null),
        Category(id = 2, name = "Housing", iconName = "Home", colorHex = -654321, isDefault = false, parentId = null),
        Category(id = 3, name = "Food", iconName = "Restaurant", colorHex = -987654, isDefault = false, parentId = null)
    )

    val mockSections = listOf(
        QuickAddSection(
            title = "TEMPLATES",
            items = mockTemplates.map { 
                QuickAddResult.TemplateItem(it, "Coffee", -123456)
            }
        ),
        QuickAddSection(
            title = "SUGGESTIONS",
            items = listOf(
                QuickAddResult.SuggestionItem(
                    label = "Lunch: Chipotle",
                    categoryId = 3,
                    iconName = "Restaurant",
                    colorHex = -987654,
                    accountId = "1",
                    amount = BigDecimal("15.20"),
                    currency = "USD",
                    entryMode = EntryMode.EXPENSE,
                    note = "Lunch",
                    payee = "Chipotle",
                    tags = emptySet()
                )
            )
        ),
        QuickAddSection(
            title = "CATEGORIES",
            items = mockCategories.map { QuickAddResult.CategoryItem(it) }
        )
    )

    OssWalletTheme {
        Box(modifier = Modifier.background(AppColors.background).padding(vertical = 20.dp)) {
            QuickAddTemplateStep(
                sections = mockSections,
                searchQuery = "",
                onSearchQueryChanged = {},
                selectedTemplateId = null,
                selectedCategoryId = null,
                isSheetSettled = true,
                onTemplateSelected = {},
                onSuggestionSelected = {},
                onCategorySelected = {}
            )
        }
    }
}
