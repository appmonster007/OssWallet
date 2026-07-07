package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import com.osscabinet.ossWallet.ui.components.aeonHairlineBorder
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.card.CardCategory
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.model.settings.ScreenContext
import com.osscabinet.ossWallet.ui.components.cards.AeonCardAtelier
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
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
 * Redesigned list option for card selection, matching the Atelier aesthetic.
 */
@Composable
fun AtelierListOption(
    title: String,
    description: String,
    onClick: () -> Unit,
    icon: ImageVector? = null
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .aeonHairlineBorder(shape)
            .clip(shape)
            .background(AppColors.recessedSurface)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AppColors.accent.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = AppColors.accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = AppTextStyles.listItemTitle.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = AppColors.contentPrimary
                )
                if (description.isNotEmpty()) {
                    Text(
                        text = description,
                        style = AppTextStyles.caption.copy(fontSize = 11.sp),
                        color = AppColors.contentSecondary.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
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
/**
 * Generic add card dialog rewritten with best-practice scrolling and inset management.
 * 
 * Uses a single LazyColumn as the root content to ensure stable nested scrolling
 * and prevents the "bouncing" glitch found in Material 3 BottomSheets.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GenericAddCardDialog(
    onDismiss: () -> Unit,
    cardOptions: List<CardTypeOptionData>
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    val groupedOptions = remember(cardOptions) {
        cardOptions
            .sortedWith(compareBy({ it.category.order }, { it.title }))
            .groupBy { it.category }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.drawerSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = AppColors.border) },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(), // Ensures content stays below status bar at all times
            contentPadding = PaddingValues(
                bottom = WindowInsets.navigationBars.asPaddingValues()
                    .calculateBottomPadding() + 24.dp
            )
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Add New Card",
                        style = AppTextStyles.heroAmount
                    )
                    Text(
                        text = "Select a card type to add to your screen",
                        style = AppTextStyles.caption,
                        color = AppColors.contentSecondary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                    )
                }
            }

            groupedOptions.forEach { (category, options) ->
                stickyHeader(key = category.name) {
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

                items(
                    items = options,
                    key = { it.title + category.name }
                ) { option ->
                    AtelierListOption(
                        title = option.title,
                        description = option.description,
                        onClick = option.onClick,
                        icon = option.icon
                    )
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
