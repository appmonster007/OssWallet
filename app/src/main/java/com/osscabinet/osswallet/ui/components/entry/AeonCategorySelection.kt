package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.ui.components.AeonSelectMode
import com.osscabinet.ossWallet.ui.components.getIconByName

@Composable
fun AeonCategorySelection(
    categories: List<Category>,
    selectedCategoryIds: Set<Long>,
    onSelectionChange: (Set<Long>) -> Unit,
    onDismiss: () -> Unit,
    mode: AeonSelectMode = AeonSelectMode.SINGLE,
    title: String = "Select Category"
) {
    AeonSelectionSheet(
        title = title,
        onDismiss = onDismiss,
        items = categories,
        selectedIds = selectedCategoryIds,
        onSelectionChange = onSelectionChange,
        idMapper = { it.id },
        labelMapper = { it.name },
        mode = mode,
        isHierarchical = true,
        parentIdMapper = { it.parentId },
        iconMapper = { getIconByName(it.iconName) },
        colorMapper = { if (it.colorHex == -1) Color(0xFF9E9E9E) else Color(it.colorHex) },
        searchPlaceholder = "Search categories..."
    )
}
