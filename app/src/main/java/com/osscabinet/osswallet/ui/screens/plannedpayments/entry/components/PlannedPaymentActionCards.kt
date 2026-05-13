package com.osscabinet.ossWallet.ui.screens.plannedpayments.entry.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.RecurrenceRule
import com.osscabinet.ossWallet.domain.service.management.PlannedPaymentService
import com.osscabinet.ossWallet.ui.components.getAccountIconByName
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.components.entry.*
import com.osscabinet.ossWallet.ui.screens.plannedpayments.entry.PlannedPaymentEntryEvent
import com.osscabinet.ossWallet.ui.screens.plannedpayments.entry.PlannedPaymentEntryState

@Composable
fun PlannedPaymentActionCards(
    state: PlannedPaymentEntryState,
    onEvent: (PlannedPaymentEntryEvent) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // First Row: Template and Schedule
        AeonContextGrid {
            EntryActionCard(
                icon = Icons.Rounded.Bookmark,
                label = "TEMPLATE",
                value = state.linkedTemplate?.name ?: "None",
                onClick = { onEvent(PlannedPaymentEntryEvent.ShowTemplateSheet(true)) },
                modifier = Modifier.weight(1f),
                iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                iconTint = MaterialTheme.colorScheme.primary
            )

            val scheduleSummary = PlannedPaymentService.buildScheduleSummary(
                state.startDateMillis,
                RecurrenceRule(state.recurrenceType, state.recurrenceInterval, state.dayOfWeek, state.dayOfMonth, state.cronExpression)
            )

            EntryActionCard(
                icon = Icons.Rounded.CalendarToday,
                label = "SCHEDULE",
                value = scheduleSummary,
                onClick = { onEvent(PlannedPaymentEntryEvent.ShowScheduleBuilder(true)) },
                modifier = Modifier.weight(1f),
                iconContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                iconTint = MaterialTheme.colorScheme.secondary
            )
        }

        // Second Row: Account and Category/Target
        AeonContextGrid {
            val selectedAccount = state.accounts.find { it.id == state.selectedAccountId }
            val accountIcon = getAccountIconByName(selectedAccount?.icon)
            val accountDisplay = when {
                state.selectedAccountId == null -> "Outside Wallet"
                selectedAccount != null -> selectedAccount.name
                else -> "Select"
            }
            EntryActionCard(
                icon = accountIcon,
                label = if (state.transactionType == 2) "FROM" else "ACCOUNT",
                value = accountDisplay,
                onClick = { onEvent(PlannedPaymentEntryEvent.ShowAccountSheet(true)) },
                modifier = Modifier.weight(1f),
                iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                iconTint = MaterialTheme.colorScheme.primary
            )

            if (state.transactionType == 2) {
                val targetAccount = state.accounts.find { it.id == state.targetAccountId }
                val targetDisplay = targetAccount?.name ?: "Select"
                val targetIcon = getAccountIconByName(targetAccount?.icon)
                EntryActionCard(
                    icon = targetIcon,
                    label = "TO",
                    value = targetDisplay,
                    onClick = { onEvent(PlannedPaymentEntryEvent.ShowTargetAccountSheet(true)) },
                    modifier = Modifier.weight(1f),
                    iconContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                    iconTint = MaterialTheme.colorScheme.tertiary
                )
            } else {
                val categoryObj = state.categories.find { it.id == state.selectedCategoryId }
                val categoryIcon = getIconByName(categoryObj?.iconName)
                EntryActionCard(
                    icon = categoryIcon,
                    label = "CATEGORY",
                    value = categoryObj?.name ?: "Unknown",
                    onClick = { onEvent(PlannedPaymentEntryEvent.ShowCategorySheet(true)) },
                    modifier = Modifier.weight(1f),
                    iconContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                    iconTint = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}
