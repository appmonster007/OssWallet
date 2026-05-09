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
import com.osscabinet.ossWallet.ui.components.AeonFullScreenDialog
import com.osscabinet.ossWallet.ui.components.AeonSelectionScreen

@Composable
fun TransactionCategorySelection(
    categories: List<Category>,
    selectedCategoryId: Long,
    onCategorySelected: (String, Long) -> Unit,
    onDismiss: () -> Unit
) {
    AeonFullScreenDialog(onDismissRequest = onDismiss) {
        AeonSelectionScreen(
            title = "Select Category",
            onBack = onDismiss
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 24.dp)
            ) {
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
                            selectedIds = setOf(selectedCategoryId),
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
}
