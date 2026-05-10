package com.osscabinet.ossWallet.ui.screens.transactions.entry.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.osscabinet.ossWallet.ui.components.AeonHybridSelector
import com.osscabinet.ossWallet.ui.components.AeonSelectMode
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.components.AeonFullScreenDialog
import com.osscabinet.ossWallet.ui.components.AeonSelectionScreen
import com.osscabinet.ossWallet.ui.components.AeonSearchField

@Composable
fun TransactionCategorySelection(
    categories: List<Category>,
    selectedCategoryId: Long,
    onCategorySelected: (String, Long) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    
    val filteredCategories = remember(query, categories) {
        if (query.isBlank()) {
            categories
        } else {
            categories.filter { it.name.contains(query, ignoreCase = true) }
                .sortedBy { it.name }
        }
    }
    
    AeonFullScreenDialog(onDismissRequest = onDismiss) {
        AeonSelectionScreen(
            title = "Select Category",
            onBack = onDismiss
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AeonSearchField(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = "Search categories…"
                )
                
                Box(modifier = Modifier.weight(1f, fill = false)) {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (query.isBlank()) {
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
                                iconMapper = { getIconByName(it.iconName) },
                                colorMapper = { if (it.colorHex == -1) Color(0xFF9E9E9E) else Color(it.colorHex) }
                            )
                        } else {
                            if (filteredCategories.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = androidx.compose.ui.Alignment.Center
                                ) {
                                    Text(
                                        text = "No categories match \"$query\"",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                AeonHybridSelector(
                                    items = filteredCategories,
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
                                    iconMapper = { getIconByName(it.iconName) },
                                    colorMapper = { if (it.colorHex == -1) Color(0xFF9E9E9E) else Color(it.colorHex) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
