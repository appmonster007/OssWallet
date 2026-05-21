package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.ui.components.AeonSelectMode
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.OutlineLight

@Composable
fun AeonCategorySelection(
    categories: List<Category>,
    selectedCategoryIds: Set<Long>,
    onSelectionChange: (Set<Long>) -> Unit,
    onDismiss: () -> Unit,
    mode: AeonSelectMode = AeonSelectMode.SINGLE,
    title: String = "Select Category",
    startExpanded: Boolean = true
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
        colorMapper = { if (it.colorHex == -1) OutlineLight else Color(it.colorHex) },
        searchPlaceholder = "Search categories...",
        startExpanded = startExpanded
    )
}

@Preview(showBackground = true)
@Composable
fun AeonCategorySelectionPreview() {
    OssWalletTheme {
        Surface {
            AeonCategorySelection(
                categories = listOf(
                    Category(id = 1, name = "Food", iconName = "Restaurant", colorHex = 0xFF4CAF50.toInt(), isDefault = false, parentId = null),
                    Category(id = 2, name = "Dining Out", iconName = "Fastfood", colorHex = 0xFF8BC34A.toInt(), isDefault = false, parentId = 1),
                    Category(id = 3, name = "Groceries", iconName = "ShoppingBasket", colorHex = 0xFFCDDC39.toInt(), isDefault = false, parentId = 1),
                    Category(id = 4, name = "Transport", iconName = "DirectionsBus", colorHex = 0xFF2196F3.toInt(), isDefault = false, parentId = null),
                    Category(id = 5, name = "Fuel", iconName = "LocalGasStation", colorHex = 0xFF03A9F4.toInt(), isDefault = false, parentId = 4)
                ),
                selectedCategoryIds = setOf(2),
                onSelectionChange = {},
                onDismiss = {}
            )
        }
    }
}
