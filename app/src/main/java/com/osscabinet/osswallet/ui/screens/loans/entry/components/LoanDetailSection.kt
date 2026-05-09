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
    onRemoveTag: (String) -> Unit
) {
    EntrySection(
        title = "ADDITIONAL DETAILS",
        icon = Icons.Rounded.MoreHoriz
    ) {
        EntryEditorialInput(
            value = counterparty,
            onValueChange = onCounterpartyChange,
            label = if (loanType == LoanType.LENT) "LENT TO" else "BORROWED FROM",
            icon = Icons.Rounded.Person
        )

        EntryEditorialInput(
            value = note,
            onValueChange = onNoteChange,
            label = "EXTRA NOTES",
            icon = Icons.AutoMirrored.Rounded.Notes
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.EventAvailable,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            Text(
                text = "Expected settlement date",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = hasSettlementDate,
                onCheckedChange = onHasSettlementDateChange,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = Color(0xFF004D40)
                )
            )
        }
    }
}
