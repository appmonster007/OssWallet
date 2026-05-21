package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.osscabinet.ossWallet.domain.card.CardCategory
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.model.settings.ScreenContext
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

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
    CardType.TAG_UPSET_PLOT -> Icons.Default.Analytics
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
                    category = type.category,
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
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(),
        color = AppColors.transparent,
        shape = RoundedCornerShape(AeonTheme.dimensions.cardCornerRadius)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AppColors.primaryContainer.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = AppColors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = AppTextStyles.listItemTitle.copy(fontWeight = FontWeight.SemiBold)
                )
                if (description.isNotEmpty()) {
                    Text(
                        text = description,
                        style = AppTextStyles.caption,
                        color = AppColors.contentSecondary,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CardTypeOptionPreview() {
    OssWalletTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            CardTypeOption(
                title = "Net Worth",
                description = "Track your total assets and liabilities over time.",
                onClick = {},
                icon = Icons.Default.AccountBalance
            )
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
    val icon: ImageVector? = null,
    val category: CardCategory = CardCategory.OTHER
)

/**
 * Generic add card dialog that can be used across all screens.
 * Takes a list of card type options and displays them.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GenericAddCardDialog(
    onDismiss: () -> Unit,
    cardOptions: List<CardTypeOptionData>
) {
    val groupedOptions = cardOptions
        .sortedWith(compareBy({ it.category.order }, { it.title }))
        .groupBy { it.category }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppColors.drawerSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = AppColors.border) },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "Add New Card",
                style = AppTextStyles.heroAmount,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            Text(
                text = "Select a card type to add to your screen",
                style = AppTextStyles.caption,
                color = AppColors.contentSecondary,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                groupedOptions.forEach { (category, options) ->
                    stickyHeader {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AppColors.drawerSurface)
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = category.displayName.uppercase(),
                                style = AppTextStyles.sectionOverline,
                                color = AppColors.primary
                            )
                        }
                    }

                    items(options) { option ->
                        CardTypeOption(
                            title = option.title,
                            description = option.description,
                            onClick = option.onClick,
                            icon = option.icon
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            color = AppColors.border.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GenericAddCardDialogPreview() {
    OssWalletTheme {
        GenericAddCardDialog(
            onDismiss = {},
            cardOptions = listOf(
                CardTypeOptionData(
                    title = "Net Worth",
                    description = "Track your total assets and liabilities over time.",
                    onClick = {},
                    icon = Icons.Default.AccountBalance,
                    category = CardCategory.SUMMARY
                ),
                CardTypeOptionData(
                    title = "Cash Flow",
                    description = "Monitor your income vs expenses.",
                    onClick = {},
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    category = CardCategory.ANALYTICS
                )
            )
        )
    }
}
