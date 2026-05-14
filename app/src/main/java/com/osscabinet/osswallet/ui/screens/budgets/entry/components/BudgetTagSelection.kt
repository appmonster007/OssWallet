package com.osscabinet.ossWallet.ui.screens.budgets.entry.components

import androidx.compose.runtime.Composable
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.ui.components.entry.AeonTagSelection

/**
 * Full-screen selection for Tags in Budget Entry.
 * Now uses the improved AeonTagSelection component for consistency.
 */
@Composable
fun BudgetTagSelection(
    availableTags: List<String>,
    selectedTags: Set<String>,
    onSelectionChanged: (Set<String>) -> Unit,
    onAddTag: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // Adapt String list to Tag list for the common component
    val tagList = availableTags.mapIndexed { index, name ->
        Tag(index.toLong(), name, 0xFF6200EE.toInt()) // Default color if not provided
    }

    AeonTagSelection(
        tags = tagList,
        selectedTags = selectedTags,
        onTagsSelected = { onSelectionChanged(it) },
        onAddTag = { name, _ -> onAddTag(name) },
        onEditTag = { },
        onDeleteTag = { }, // Deletion not supported in simple string-based tag selection yet
        onDismiss = onDismiss
    )
}
