package com.osscabinet.ossWallet.ui.screens.transactions.entry.sheets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.ui.components.AeonHybridSelector
import com.osscabinet.ossWallet.ui.components.AeonSelectMode
import com.osscabinet.ossWallet.ui.components.AeonSectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateSelectionSheet(
    templates: List<Template>,
    onTemplateSelected: (Template) -> Unit,
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
            AeonSectionHeader(title = "Select Template")
            Spacer(modifier = Modifier.height(16.dp))

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
                            detailMapper = { "${it.category} • ${it.note ?: "No note"}" },
                            iconMapper = { Icons.Rounded.Bookmark }
                        )
                    }
                }
            }
        }
    }
}
