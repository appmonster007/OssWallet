package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
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
    payeeLabel: String = "PAYEE",
    showNote: Boolean = true,
    showPayee: Boolean = true,
    showTags: Boolean = true,
    selectedNames: Set<String> = emptySet(),
    allAvailableTags: List<Tag> = emptyList(),
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
                placeholder = "Tap to enter...",
                isFlush = true
            )
        }

        if (showPayee) {
            if (showNote) {
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = AppColors.divider.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = AeonTheme.dimensions.iconSizeXl + AeonTheme.dimensions.spacingLg)
                )
            }
            EntryEditorialInput(
                value = payee,
                onValueChange = onPayeeChange,
                label = payeeLabel,
                icon = Icons.Rounded.Person,
                suggestions = payeeSuggestions,
                isFlush = true
            )
        }

        if (showTags) {
            if (showNote || showPayee) {
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = AppColors.divider.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = AeonTheme.dimensions.iconSizeXl + AeonTheme.dimensions.spacingLg)
                )
            }
            EntryTagSection(
                selectedNames = selectedNames,
                allAvailableTags = allAvailableTags,
                onShowTagSheet = onTagClick,
                onRemoveTag = onRemoveTag
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
