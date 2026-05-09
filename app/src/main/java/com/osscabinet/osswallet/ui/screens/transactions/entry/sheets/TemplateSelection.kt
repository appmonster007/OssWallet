package com.osscabinet.ossWallet.ui.screens.transactions.entry.sheets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.ui.components.AeonHybridSelector
import com.osscabinet.ossWallet.ui.components.AeonSelectMode
import com.osscabinet.ossWallet.ui.components.AeonFullScreenDialog
import com.osscabinet.ossWallet.ui.components.AeonSelectionScreen

/**
 * Full-screen selection for Transaction Templates.
 * Replaced TemplateSelectionSheet.
 */
@Composable
fun TemplateSelection(
    templates: List<Template>,
    categories: List<Category>,
    onTemplateSelected: (Template) -> Unit,
    onDismiss: () -> Unit
) {
    AeonFullScreenDialog(onDismissRequest = onDismiss) {
        AeonSelectionScreen(
            title = "Select Template",
            onBack = onDismiss
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 24.dp)
            ) {
                if (templates.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No templates available",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Box(modifier = Modifier.weight(1f, fill = false)) {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            AeonHybridSelector(
                                items = templates,
                                mode = AeonSelectMode.SINGLE,
                                selectedIds = emptySet(),
                                onSelectionChange = { ids ->
                                    ids.firstOrNull()?.let { id ->
                                        templates.find { it.id.toString() == id }?.let { 
                                            onTemplateSelected(it)
                                            onDismiss()
                                        }
                                    }
                                },
                                idMapper = { it.id.toString() },
                                labelMapper = { it.name },
                                detailMapper = { template ->
                                    val categoryName = categories.find { it.id == template.categoryId }?.name ?: "Unknown"
                                    "$categoryName • ${template.note ?: "No note"}"
                                },
                                iconMapper = { Icons.Rounded.Bookmark }
                            )
                        }
                    }
                }
            }
        }
    }
}
