package com.osscabinet.ossWallet.ui.screens.budgets.entry.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.BudgetPeriod
import com.osscabinet.ossWallet.domain.model.BudgetTrackType
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.ui.components.getAccountIconByName
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.components.entry.*

@Composable
fun BudgetActionCards(
    period: BudgetPeriod,
    onPeriodClick: () -> Unit,
    selectedAccountIds: Set<String>,
    accounts: List<Account>,
    onAccountClick: () -> Unit,
    selectedTrackTypes: Set<BudgetTrackType>,
    onTrackTypeClick: () -> Unit,
    selectedCategoryIds: Set<Long>,
    categories: List<Category>,
    onCategoryClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Row 1: Period (Full Width as per MD)
        EntryActionCard(
            icon = Icons.Rounded.DateRange,
            label = "PERIOD",
            value = period.name.lowercase().replaceFirstChar { it.uppercase() },
            onClick = onPeriodClick,
            modifier = Modifier.fillMaxWidth(),
            iconContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
            iconTint = MaterialTheme.colorScheme.secondary
        )

        // Row 2: Account and Tracks (Side by Side)
        EntryActionRow {
            val accountObj = if (selectedAccountIds.size == 1) accounts.find { it.id == selectedAccountIds.first() } else null
            EntryActionCard(
                icon = getAccountIconByName(accountObj?.icon),
                label = "ACCOUNT",
                value = when {
                    selectedAccountIds.isEmpty() -> "All Accounts"
                    accountObj != null -> accountObj.name
                    else -> "${selectedAccountIds.size} Accounts"
                },
                onClick = onAccountClick,
                modifier = Modifier.weight(1f),
                iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                iconTint = MaterialTheme.colorScheme.primary
            )

            EntryActionCard(
                icon = Icons.Rounded.ShowChart,
                label = "TRACKS",
                value = if (selectedTrackTypes.size == BudgetTrackType.entries.size) "All Types"
                        else selectedTrackTypes.joinToString { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                onClick = onTrackTypeClick,
                modifier = Modifier.weight(1f),
                iconContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                iconTint = MaterialTheme.colorScheme.tertiary
            )
        }

        // Row 3: Filters / Categories (Full Width)
        val categoryObj = if (selectedCategoryIds.size == 1) categories.find { it.id == selectedCategoryIds.first() } else null
        EntryActionCard(
            icon = categoryObj?.let { getIconByName(it.iconName) } ?: Icons.Rounded.FilterList,
            label = "FILTERS",
            value = when {
                selectedCategoryIds.isEmpty() -> "All Categories"
                categoryObj != null -> categoryObj.name
                else -> "${selectedCategoryIds.size} Categories"
            },
            onClick = onCategoryClick,
            modifier = Modifier.fillMaxWidth(),
            iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            iconTint = MaterialTheme.colorScheme.primary
        )
    }
}
