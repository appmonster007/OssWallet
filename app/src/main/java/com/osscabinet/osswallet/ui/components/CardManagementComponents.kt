package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.model.settings.ScreenContext
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

fun CardType.toIcon(): ImageVector = when (this) {
    CardType.NET_WORTH -> Icons.Default.AccountBalance
    CardType.CASH_FLOW -> Icons.AutoMirrored.Filled.TrendingUp
    CardType.CURRENCY_BALANCES -> Icons.Default.AttachMoney
    CardType.ACCOUNTS -> Icons.Default.AccountBox
    CardType.BUDGETS -> Icons.Default.Savings
    CardType.CATEGORY_PIE_CHART -> Icons.Default.PieChart
    CardType.CATEGORY_TREEMAP -> Icons.Default.GridView
    CardType.ACCOUNT_HERO -> Icons.Default.AccountCircle
    CardType.PLANNED_PAYMENTS -> Icons.Default.Schedule
    CardType.LOANS -> Icons.Default.CreditCard
    CardType.RECENT_ACTIVITIES -> Icons.Default.History
    CardType.CASHFLOW_COMPARISON -> Icons.AutoMirrored.Filled.ShowChart
    CardType.INCOME_EXPENSE_BAR -> Icons.Default.BarChart
    CardType.CATEGORY_SPENDING_TREND -> Icons.Default.BarChart
}

/**
 * Unified card type selection dialog used across Dashboard, Analytics, and AccountDetail screens.
 *
 * Filters available card types by [screenContext] and applies [additionalFilter] for any
 * screen-specific exclusions (e.g. hiding unimplemented card types).
 */
@Composable
fun CardTypeSelectionDialog(
    screenContext: ScreenContext,
    onDismiss: () -> Unit,
    onCardTypeSelected: (CardType) -> Unit,
    additionalFilter: (CardType) -> Boolean = { true }
) {
    GenericAddCardDialog(
        onDismiss = onDismiss,
        cardOptions = CardType.entries
            .filter { it.isUserAddableOn(screenContext) && additionalFilter(it) }
            .map { type ->
                CardTypeOptionData(
                    title = type.displayName,
                    description = type.description,
                    icon = type.toIcon(),
                    onClick = { onCardTypeSelected(type) }
                )
            }
    )
}

/**
 * Reusable card type option for add card dialogs.
 */
@Composable
fun CardTypeOption(
    title: String,
    description: String,
    onClick: () -> Unit,
    icon: ImageVector? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = AppColors.insetSurface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AppColors.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = AppTextStyles.screenSubTitle
                )
                Text(
                    text = description,
                    style = AppTextStyles.caption,
                    color = AppColors.contentSecondary
                )
            }
        }
    }
}

/**
 * Data class representing a card type option for the add card dialog.
 */
data class CardTypeOptionData(
    val title: String,
    val description: String,
    val onClick: () -> Unit,
    val icon: ImageVector? = null
)

/**
 * Generic add card dialog that can be used across all screens.
 * Takes a list of card type options and displays them.
 */
@Composable
fun GenericAddCardDialog(
    onDismiss: () -> Unit,
    cardOptions: List<CardTypeOptionData>
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Card") },
        text = {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Select a card type to add:")
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(cardOptions) { option ->
                        CardTypeOption(
                            title = option.title,
                            description = option.description,
                            onClick = option.onClick,
                            icon = option.icon
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .fillMaxHeight(0.85f)
    )
}
