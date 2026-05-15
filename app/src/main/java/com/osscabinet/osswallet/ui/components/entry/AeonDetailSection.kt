package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Person
import androidx.compose.runtime.Composable

/**
 * Standardized Detail tier component for entry screens.
 * Encapsulates the Payee, Notes, and Tag selection rows.
 */
@Composable
fun AeonDetailSection(
    note: String,
    onNoteChange: (String) -> Unit,
    payee: String = "",
    onPayeeChange: (String) -> Unit = {},
    noteLabel: String = "NOTE",
    payeeLabel: String = "PAYEE",
    showNote: Boolean = true,
    showPayee: Boolean = true,
    showTags: Boolean = true,
    selectedTags: Collection<String> = emptyList(),
    onTagClick: () -> Unit = {},
    onRemoveTag: (String) -> Unit = {},
    noteSuggestions: List<String> = emptyList(),
    payeeSuggestions: List<String> = emptyList(),
    additionalContent: @Composable (ColumnScope.() -> Unit)? = null
) {
    EntrySection(
        title = "ADDITIONAL DETAILS",
        icon = Icons.Rounded.MoreHoriz
    ) {
        if (showNote) {
            EntryEditorialInput(
                value = note,
                onValueChange = onNoteChange,
                label = noteLabel,
                icon = Icons.AutoMirrored.Rounded.Notes,
                suggestions = noteSuggestions,
                placeholder = "Tap to enter..."
            )
        }

        if (showPayee) {
            EntryEditorialInput(
                value = payee,
                onValueChange = onPayeeChange,
                label = payeeLabel,
                icon = Icons.Rounded.Person,
                suggestions = payeeSuggestions
            )
        }

        if (showTags) {
            EntryTagSection(
                selectedTags = selectedTags.toList(),
                onShowTagSheet = onTagClick,
                onRemoveTag = onRemoveTag
            )
        }

        additionalContent?.invoke(this)
    }
}
