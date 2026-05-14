package com.osscabinet.ossWallet.ui.screens.tags.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.ui.components.AeonConfirmDeleteDialog
import com.osscabinet.ossWallet.ui.components.entry.AeonTagSelectionContent
import com.osscabinet.ossWallet.ui.components.entry.TagPresetColors
import com.osscabinet.ossWallet.ui.screens.tags.TagViewModel

/**
 * Screen for managing tags in settings.
 * Reuses AeonTagSelectionContent for a consistent search-and-create experience.
 */
@Composable
fun TagListScreen(
    viewModel: TagViewModel,
    onBackClick: () -> Unit
) {
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    var tagToDelete by remember { mutableStateOf<Tag?>(null) }
    var tagToEdit by remember { mutableStateOf<Tag?>(null) }
    var transactionCount by remember { mutableIntStateOf(0) }

    // Fetch transaction count when tagToDelete changes
    LaunchedEffect(tagToDelete) {
        tagToDelete?.let {
            transactionCount = viewModel.getTransactionCountForTag(it.name)
        }
    }

    AeonConfirmDeleteDialog(
        show = tagToDelete != null,
        title = "Delete Tag",
        message = if (transactionCount > 0) {
            "This tag is used in $transactionCount transactions. Are you sure you want to delete it? The tag will be removed from those transactions."
        } else {
            "Are you sure you want to delete the tag \"${tagToDelete?.name}\"?"
        },
        onConfirm = {
            tagToDelete?.let { viewModel.deleteTag(it) }
            tagToDelete = null
        },
        onDismiss = { tagToDelete = null }
    )

    if (tagToEdit != null) {
        EditTagDialog(
            tag = tagToEdit!!,
            onDismiss = { tagToEdit = null },
            onConfirm = { newName, newColor ->
                viewModel.updateTag(tagToEdit!!.name, tagToEdit!!.copy(name = newName, colorHex = newColor))
                tagToEdit = null
            }
        )
    }

    // Reuse the AeonTagSelectionContent but without "Done" selection logic
    AeonTagSelectionContent(
        tags = tags,
        selectedTags = emptySet<String>(),
        onTagsSelected = { onBackClick() },
        onAddTag = { name, color -> 
            viewModel.addTag(name, color)
        },
        onEditTag = { tag ->
            tagToEdit = tag
        },
        onDeleteTag = { tag ->
            tagToDelete = tag
        },
        onDismiss = onBackClick,
        title = "Manage Tags",
        isSelectionModeEnabled = false
    )
}

@Composable
fun EditTagDialog(
    tag: Tag,
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit
) {
    var name by remember { mutableStateOf(tag.name) }
    var selectedColor by remember { mutableStateOf(Color(tag.colorHex)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Tag") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tag Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text("Tag Color", style = MaterialTheme.typography.labelLarge)
                
                TagPresetColors.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(color, CircleShape)
                                    .clickable { selectedColor = color }
                                    .border(
                                        width = if (selectedColor == color) 2.dp else 0.dp,
                                        color = if (selectedColor == color) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim(), selectedColor.toArgb()) },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
