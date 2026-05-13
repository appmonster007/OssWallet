package com.osscabinet.ossWallet.ui.screens.templates.entry.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.osscabinet.ossWallet.ui.components.entry.*
import com.osscabinet.ossWallet.ui.screens.templates.entry.TemplateEntryEvent
import com.osscabinet.ossWallet.ui.screens.templates.entry.TemplateEntryState

@Composable
fun TemplateDetailSection(
    state: TemplateEntryState,
    onEvent: (TemplateEntryEvent) -> Unit
) {
    AeonDetailSection(
        note = state.note,
        onNoteChange = { onEvent(TemplateEntryEvent.SetNote(it)) },
        payee = state.payee,
        onPayeeChange = { onEvent(TemplateEntryEvent.SetPayee(it)) },
        selectedTags = state.tags,
        onTagClick = { onEvent(TemplateEntryEvent.ShowTagSheet(true)) },
        onRemoveTag = { tag -> onEvent(TemplateEntryEvent.SetTags(state.tags - tag)) },
        noteSuggestions = state.uniqueNotes,
        payeeSuggestions = state.uniquePayees,
        additionalContent = {
            // Payment Method and Status row
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val methods = listOf(
                    "Cash" to Icons.Rounded.Payments,
                    "Credit Card" to Icons.Rounded.CreditCard,
                    "Debit Card" to Icons.Rounded.CreditCard,
                    "Bank Transfer" to Icons.Rounded.AccountBalance,
                    "Digital Wallet" to Icons.Rounded.Wallet,
                    "Mobile Payment" to Icons.Rounded.Contactless,
                    "Check" to Icons.Rounded.Description,
                    "Cryptocurrency" to Icons.Rounded.CurrencyBitcoin,
                    "Other" to Icons.Rounded.MoreHoriz
                )
                val paymentIcon = methods.find { it.first == state.paymentMethod }?.second ?: Icons.Rounded.Contactless

                EntryActionChip(
                    icon = paymentIcon,
                    label = state.paymentMethod,
                    onClick = { onEvent(TemplateEntryEvent.ShowPaymentMethodSheet(true)) },
                    modifier = Modifier.weight(1f)
                )

                EntryActionChip(
                    icon = Icons.Rounded.CheckCircle,
                    label = "Auto-status: ${state.transactionStatus.name}",
                    onClick = { onEvent(TemplateEntryEvent.ShowStatusSheet(true)) },
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
            )

            // Warranty
            EntrySwitchRow(
                label = "Warranty Protection",
                checked = state.isWarrantyEnabled,
                onCheckedChange = { onEvent(TemplateEntryEvent.SetWarrantyEnabled(it)) },
                icon = Icons.Rounded.Security
            )

            if (state.isWarrantyEnabled) {
                EntryDateCard(
                    icon = Icons.Rounded.CalendarMonth,
                    label = "EXPIRATION DATE",
                    dateMillis = state.warrantyExpirationMillis,
                    onClick = { onEvent(TemplateEntryEvent.ShowWarrantyDatePicker(true)) }
                )
            }

            // Location
            EntryLocationRow(
                coords = if (state.latitude != null && state.longitude != null) state.latitude to state.longitude else null,
                onShowLocationSheet = { onEvent(TemplateEntryEvent.ShowLocationSheet(true)) },
                label = "Template Location"
            )
        }
    )
}
