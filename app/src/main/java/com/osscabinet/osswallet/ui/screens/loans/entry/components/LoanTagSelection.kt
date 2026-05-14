package com.osscabinet.ossWallet.ui.screens.loans.entry.components

import androidx.compose.runtime.Composable
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.ui.components.entry.AeonTagSelection

/**
 * Full-screen selection for Tags in Loan Entry.
 */
@Composable
fun LoanTagSelection(
    availableTags: List<String>,
    selectedTags: Set<String>,
    onSelectionChanged: (Set<String>) -> Unit,
    onAddTag: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // Adapt String list to Tag list for the common component
    val tagList = availableTags.mapIndexed { index, name ->
        Tag(index.toLong(), name, 0xFF6200EE.toInt())
    }

    AeonTagSelection(
        tags = tagList,
        selectedTags = selectedTags.toList(),
        onTagsSelected = { onSelectionChanged(it.toSet()) },
        onAddTag = { name, _ -> onAddTag(name) },
        onEditTag = { },
        onDeleteTag = { },
        onDismiss = onDismiss
    )
}
