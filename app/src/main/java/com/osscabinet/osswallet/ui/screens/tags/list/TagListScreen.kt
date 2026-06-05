package com.osscabinet.ossWallet.ui.screens.tags.list

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.R
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.ui.components.AeonConfirmDeleteDialog
import com.osscabinet.ossWallet.ui.components.entry.AeonTagSelectionContent
import com.osscabinet.ossWallet.ui.screens.tags.TagViewModel
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.TagPresetColors

/**
 * Screen for managing tags in settings.
 * Reuses AeonTagSelectionContent for a consistent search-and-create experience.
 */
@Composable
fun TagListScreen(
    viewModel: TagViewModel,
    actions: TagListActions
) {
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    var tagToDelete by remember { mutableStateOf<Tag?>(null) }
    var tagToEdit by remember { mutableStateOf<Tag?>(null) }
    var transactionCount by remember { mutableIntStateOf(0) }

    BackHandler(enabled = tagToDelete != null) { tagToDelete = null }
    BackHandler(enabled = tagToEdit != null) { tagToEdit = null }

    // Fetch transaction count when tagToDelete changes
    LaunchedEffect(tagToDelete) {
        tagToDelete?.let {
            transactionCount = viewModel.getTransactionCountForTag(it.name)
        }
    }

    AeonConfirmDeleteDialog(
        show = tagToDelete != null,
        title = stringResource(R.string.tag_delete_title),
        message = if (transactionCount > 0) {
            stringResource(R.string.tag_delete_confirm_with_count, transactionCount)
        } else {
            stringResource(R.string.tag_delete_confirm_simple, tagToDelete?.name ?: "")
        },
        onConfirm = {
            tagToDelete?.let { viewModel.deleteTag(it) }
            tagToDelete = null
        },
        onDismiss = { tagToDelete = null },
        confirmButtonText = stringResource(R.string.action_delete),
        dismissButtonText = stringResource(R.string.action_cancel)
    )

    if (tagToEdit != null) {
        EditTagDialog(
            tag = tagToEdit!!,
            onDismiss = { tagToEdit = null },
            onConfirm = { newName, newColor ->
                viewModel.updateTag(
                    tagToEdit!!.name,
                    tagToEdit!!.copy(name = newName, colorHex = newColor)
                )
                tagToEdit = null
            }
        )
    }

    // Reuse the AeonTagSelectionContent but without "Done" selection logic
    AeonTagSelectionContent(
        tags = tags,
        selectedTags = emptySet<String>(),
        onTagsSelected = { actions.onBackClick() },
        onAddTag = { name, color ->
            viewModel.addTag(name, color)
        },
        onEditTag = { tag ->
            tagToEdit = tag
        },
        onDeleteTag = { tag ->
            tagToDelete = tag
        },
        onDismiss = actions.onBackClick,
        title = stringResource(R.string.tag_manage_title),
        isSelectionModeEnabled = false
    )
}

data class TagListActions(
    val onBackClick: () -> Unit
)

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
        title = { Text(stringResource(R.string.tag_edit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.tag_edit_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    stringResource(R.string.tag_edit_color_label),
                    style = AppTextStyles.drawerItemLabel
                )

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
                                        color = if (selectedColor == color) AppColors.primary else AppColors.transparent,
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
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
