package com.osscabinet.ossWallet.ui.screens.transactions.entry.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.ui.components.AeonHierarchicalHybridSelector
import com.osscabinet.ossWallet.ui.components.AeonSelectMode
import com.osscabinet.ossWallet.ui.components.AccountIconMap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionCategoryPicker(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategorySelected: (String, Long?) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)) }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Select Category",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
            )
            Text(
                text = "Classify this transaction for better analytics",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            Box(modifier = Modifier.weight(1f, fill = false)) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    AeonHierarchicalHybridSelector(
                        items = categories,
                        mode = AeonSelectMode.SINGLE,
                        selectedIds = if (selectedCategoryId != null) setOf(selectedCategoryId) else emptySet(),
                        onSelectionChange = { ids ->
                            val selectedId = ids.firstOrNull()
                            val category = categories.find { it.id == selectedId }
                            if (category != null) {
                                onCategorySelected(category.name, category.id)
                                onDismiss()
                            }
                        },
                        idMapper = { it.id },
                        labelMapper = { it.name },
                        parentIdMapper = { it.parentId },
                        iconMapper = { AccountIconMap[it.iconName] },
                        colorMapper = { if (it.colorHex == -1) Color(0xFF9E9E9E) else Color(it.colorHex) }
                    )
                }
            }
        }
    }
}
