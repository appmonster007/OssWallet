package com.osscabinet.ossWallet.ui.screens.plannedpayments.entry.components

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
import com.osscabinet.ossWallet.ui.components.AppIconMap
import com.osscabinet.ossWallet.ui.components.entry.*
import com.osscabinet.ossWallet.ui.screens.plannedpayments.entry.PlannedPaymentEntryEvent
import com.osscabinet.ossWallet.ui.screens.plannedpayments.entry.PlannedPaymentEntryState

@Composable
fun PlannedPaymentDetailSection(
    state: PlannedPaymentEntryState,
    onEvent: (PlannedPaymentEntryEvent) -> Unit
) {
    AeonDetailSection(
        note = state.noteUserPart,
        onNoteChange = { onEvent(PlannedPaymentEntryEvent.SetNote(it)) },
        payee = state.payee,
        onPayeeChange = { onEvent(PlannedPaymentEntryEvent.SetPayee(it)) },
        selectedTags = state.selectedTagNames,
        onTagClick = { onEvent(PlannedPaymentEntryEvent.ShowTagSheet(true)) },
        onRemoveTag = { tag -> onEvent(PlannedPaymentEntryEvent.SetTags(state.selectedTagNames - tag)) },
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
                    onClick = { onEvent(PlannedPaymentEntryEvent.ShowPaymentMethodSheet(true)) },
                    modifier = Modifier.weight(1f)
                )

                EntryActionChip(
                    icon = Icons.Rounded.Info,
                    label = state.status.name,
                    onClick = { onEvent(PlannedPaymentEntryEvent.ShowStatusSheet(true)) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Transaction Status (Cleared/Pending/Void)
            EntryActionChip(
                icon = Icons.Rounded.CheckCircle,
                label = "Auto-status: ${state.transactionStatus.name}",
                onClick = { onEvent(PlannedPaymentEntryEvent.ShowTransactionStatusSheet(true)) },
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
            )

            // End Date
            EntrySwitchRow(
                label = "Has End Date",
                checked = state.hasEndDate,
                onCheckedChange = { onEvent(PlannedPaymentEntryEvent.SetHasEndDate(it)) },
                icon = Icons.Rounded.EventBusy
            )

            if (state.hasEndDate) {
                EntryDateCard(
                    icon = Icons.Rounded.CalendarToday,
                    label = "END DATE",
                    dateMillis = state.endDateMillis,
                    onClick = { onEvent(PlannedPaymentEntryEvent.ShowEndDatePicker(true)) }
                )
            }

            // Auto Confirm
            EntrySwitchRow(
                label = "Auto-confirm Transaction",
                checked = state.autoConfirm,
                onCheckedChange = { onEvent(PlannedPaymentEntryEvent.SetAutoConfirm(it)) },
                icon = Icons.Rounded.AutoFixHigh
            )

            // Warranty
            EntrySwitchRow(
                label = "Warranty Protection",
                checked = state.isWarrantyEnabled,
                onCheckedChange = { onEvent(PlannedPaymentEntryEvent.SetWarrantyEnabled(it)) },
                icon = Icons.Rounded.Security
            )

            if (state.isWarrantyEnabled) {
                EntryDateCard(
                    icon = Icons.Rounded.CalendarMonth,
                    label = "EXPIRATION DATE",
                    dateMillis = state.warrantyExpirationMillis,
                    onClick = { onEvent(PlannedPaymentEntryEvent.ShowWarrantyDatePicker(true)) }
                )
            }

            // Location
            LocationRow(
                coords = if (state.latitude != null && state.longitude != null) state.latitude to state.longitude else null,
                onShowLocationSheet = { onEvent(PlannedPaymentEntryEvent.ShowLocationSheet(true)) }
            )
        }
    )
}

@Composable
private fun LocationRow(
    coords: Pair<Double, Double>?,
    onShowLocationSheet: () -> Unit
) {
    Surface(
        onClick = onShowLocationSheet,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.tertiary)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Payment Location", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)))
                Text(
                    text = coords?.let { "Coordinates: ${it.first.toString().take(6)}, ${it.second.toString().take(6)}" } ?: "Add Location",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        }
    }
}
