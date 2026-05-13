package com.osscabinet.ossWallet.ui.screens.loans.entry.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.LoanType
import com.osscabinet.ossWallet.ui.components.entry.*

@Composable
fun LoanDetailSection(
    counterparty: String,
    onCounterpartyChange: (String) -> Unit,
    loanType: LoanType,
    note: String,
    onNoteChange: (String) -> Unit,
    hasSettlementDate: Boolean,
    onHasSettlementDateChange: (Boolean) -> Unit,
    selectedTags: Set<String>,
    onShowTagSheet: () -> Unit,
    onRemoveTag: (String) -> Unit,
    counterpartySuggestions: List<String> = emptyList(),
    noteSuggestions: List<String> = emptyList()
) {
    EntrySection(
        title = "ADDITIONAL DETAILS",
        icon = Icons.Rounded.MoreHoriz
    ) {
        EntryEditorialInput(
            value = counterparty,
            onValueChange = onCounterpartyChange,
            label = if (loanType == LoanType.LENT) "LENT TO" else "BORROWED FROM",
            icon = Icons.Rounded.Person,
            suggestions = counterpartySuggestions
        )

        EntryEditorialInput(
            value = note,
            onValueChange = onNoteChange,
            label = "EXTRA NOTES",
            icon = Icons.AutoMirrored.Rounded.Notes,
            suggestions = noteSuggestions
        )

        EntryTagSection(
            selectedTags = selectedTags,
            onShowTagSheet = onShowTagSheet,
            onRemoveTag = onRemoveTag
        )

        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        )

        // Settlement toggle
        EntrySwitchRow(
            label = "Expected settlement date",
            checked = hasSettlementDate,
            onCheckedChange = onHasSettlementDateChange,
            icon = Icons.Rounded.EventAvailable
        )
    }
}
