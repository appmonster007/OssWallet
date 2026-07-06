package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.remember
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

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
    notePlaceholder: String = "Tap to enter...",
    payeeLabel: String = "PAYEE",
    showNote: Boolean = true,
    showPayee: Boolean = true,
    showTags: Boolean = true,
    selectedNames: Set<String> = emptySet(),
    allAvailableTags: List<Tag> = emptyList(),
    onTagClick: () -> Unit = {},
    onRemoveTag: (String) -> Unit = {},
    onTagsSelected: (Set<String>) -> Unit = {},
    onAddTag: (String, Int) -> Unit = { _, _ -> },
    noteSuggestions: List<String> = emptyList(),
    payeeSuggestions: List<String> = emptyList(),
    additionalContent: @Composable (ColumnScope.() -> Unit)? = null
) {
    val sectionRequester = remember { BringIntoViewRequester() }

    EntrySection(
        title = "ADDITIONAL DETAILS",
        icon = Icons.Rounded.MoreHoriz,
        modifier = Modifier.bringIntoViewRequester(sectionRequester)
    ) {
        if (showNote) {
            EntryEditorialInput(
                value = note,
                onValueChange = onNoteChange,
                label = noteLabel,
                icon = Icons.AutoMirrored.Rounded.Notes,
                suggestions = noteSuggestions,
                placeholder = notePlaceholder,
                isSelected = note.isNotEmpty(),
                isFlush = false,
                scrollRequester = sectionRequester
            )
        }

        if (showPayee) {
            EntryEditorialInput(
                value = payee,
                onValueChange = onPayeeChange,
                label = payeeLabel,
                icon = Icons.Rounded.Person,
                suggestions = payeeSuggestions,
                isSelected = payee.isNotEmpty(),
                isFlush = false,
                scrollRequester = sectionRequester
            )
        }

        if (showTags) {
            EntryTagSection(
                selectedNames = selectedNames,
                allAvailableTags = allAvailableTags,
                onShowTagSheet = onTagClick,
                onRemoveTag = onRemoveTag,
                onTagsSelected = onTagsSelected,
                onAddTag = onAddTag
            )
        }

        if (additionalContent != null) {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = AppColors.divider.copy(alpha = 0.5f)
            )
            additionalContent()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AeonDetailSectionPreview() {
    OssWalletTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            AeonDetailSection(
                note = "Lunch with team",
                onNoteChange = {},
                payee = "Starbucks",
                onPayeeChange = {},
                selectedNames = setOf("Business", "Food"),
                allAvailableTags = listOf(
                    Tag(name = "Business", colorHex = 0xFF2196F3.toInt()),
                    Tag(name = "Food", colorHex = 0xFF4CAF50.toInt()),
                    Tag(name = "Personal", colorHex = 0xFFFFC107.toInt())
                )
            )
        }
    }
}
