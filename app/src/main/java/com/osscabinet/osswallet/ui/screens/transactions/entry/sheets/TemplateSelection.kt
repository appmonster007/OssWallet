package com.osscabinet.ossWallet.ui.screens.transactions.entry.sheets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.ui.components.AeonHybridSelector
import com.osscabinet.ossWallet.ui.components.AeonSelectMode
import com.osscabinet.ossWallet.ui.components.AeonFullScreenDialog
import com.osscabinet.ossWallet.ui.components.AeonSelectionScreen
import com.osscabinet.ossWallet.ui.components.AeonSearchField
import com.osscabinet.ossWallet.ui.components.getIconByName

@Composable
fun TemplateSelection(
    templates: List<Template>,
    categories: List<Category>,
    onTemplateSelected: (Template) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    val filtered = remember(query, templates) {
        if (query.isBlank()) templates
        else templates.filter { it.name.contains(query, ignoreCase = true) }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AeonFullScreenDialog(onDismissRequest = onDismiss) {
        AeonSelectionScreen(
            title = "Select Template",
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
                    placeholder = "Search templates…",
                    focusRequester = focusRequester
                )

                when {
                    templates.isEmpty() -> {
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
                    }
                    filtered.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No templates match \"$query\"",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    else -> {
                        Box(modifier = Modifier.weight(1f, fill = false)) {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                AeonHybridSelector(
                                    items = filtered,
                                    mode = AeonSelectMode.SINGLE,
                                    selectedIds = emptySet(),
                                    onSelectionChange = { ids ->
                                        ids.firstOrNull()?.let { id ->
                                            filtered.find { it.id.toString() == id }?.let {
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
                                    iconMapper = { template ->
                                        val iconName = categories.find { it.id == template.categoryId }?.iconName
                                        getIconByName(iconName)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
