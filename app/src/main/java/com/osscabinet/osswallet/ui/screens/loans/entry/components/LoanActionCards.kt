package com.osscabinet.ossWallet.ui.screens.loans.entry.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.ui.components.entry.EntryActionCard
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun LoanActionCards(
    startDateMillis: Long,
    onStartDateClick: () -> Unit,
    interestRate: String,
    onInterestClick: () -> Unit,
    linkedAccountId: String,
    accounts: List<Account>,
    onAccountClick: () -> Unit,
    hasSettlementDate: Boolean,
    settlementDateMillis: Long,
    onSettlementClick: () -> Unit,
    selectedCategoryIds: Set<Long>,
    categories: List<Category>,
    onCategoryClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Row 1: Start Date and Interest Rate
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EntryActionCard(
                icon = Icons.Rounded.CalendarToday,
                label = "START DATE",
                value = formatLoanDate(startDateMillis),
                onClick = onStartDateClick,
                modifier = Modifier.weight(1f),
                iconContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                iconTint = MaterialTheme.colorScheme.secondary
            )

            EntryActionCard(
                icon = Icons.Rounded.Percent,
                label = "INT. RATE %",
                value = if (interestRate.isEmpty()) "0.00 %" else "$interestRate %",
                onClick = onInterestClick,
                modifier = Modifier.weight(1f),
                iconContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                iconTint = MaterialTheme.colorScheme.tertiary
            )
        }

        // Row 2: Account and Settlement
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EntryActionCard(
                icon = Icons.Rounded.AccountBalance,
                label = "ACCOUNT",
                value = accounts.find { it.id == linkedAccountId }?.name ?: "Select",
                onClick = { onAccountClick() },
                modifier = Modifier.weight(1f),
                iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                iconTint = MaterialTheme.colorScheme.primary
            )

            EntryActionCard(
                icon = Icons.Rounded.EventAvailable,
                label = "SETTLEMENT",
                value = if (hasSettlementDate) formatLoanDate(settlementDateMillis) else "Not set",
                onClick = { if (hasSettlementDate) onSettlementClick() },
                modifier = Modifier.weight(1f),
                iconContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                iconTint = MaterialTheme.colorScheme.secondary
            )
        }

        // Row 3: Filters / Categories (Full Width)
        EntryActionCard(
            icon = Icons.Rounded.FilterList,
            label = "FILTERS",
            value = when {
                selectedCategoryIds.isEmpty() -> "All Categories"
                selectedCategoryIds.size == 1 -> categories.find { it.id == selectedCategoryIds.first() }?.name ?: "1 Category"
                else -> "${selectedCategoryIds.size} Categories"
            },
            onClick = onCategoryClick,
            modifier = Modifier.fillMaxWidth(),
            iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            iconTint = MaterialTheme.colorScheme.primary
        )
    }
}

private fun formatLoanDate(millis: Long): String {
    return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(java.util.Date(millis))
}
