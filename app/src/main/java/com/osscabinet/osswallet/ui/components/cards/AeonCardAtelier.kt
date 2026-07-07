package com.osscabinet.ossWallet.ui.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.card.getAtelierTab
import com.osscabinet.ossWallet.domain.model.settings.ScreenContext
import com.osscabinet.ossWallet.ui.components.AeonGlassSheet
import com.osscabinet.ossWallet.ui.components.aeonHairlineBorder
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

/**
 * Redesigned Card Studio (Atelier) for adding new cards.
 * Provides a high-performance, intent-based UI that eliminates scroll conflicts.
 */
@Composable
fun AeonCardAtelier(
    screenContext: ScreenContext,
    onDismiss: () -> Unit,
    onCardTypeSelected: (CardType) -> Unit,
    modifier: Modifier = Modifier,
    additionalFilter: (CardType) -> Boolean = { true }
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf("ALL") }

    val tabs = listOf("ALL", "STATUS", "WALLET", "TRACK", "TRENDS")

    val allOptions = remember(screenContext) {
        CardType.entries.filter { it.isUserAddableOn(screenContext) && additionalFilter(it) }
    }

    val filteredOptions = remember(searchQuery, selectedTab, allOptions) {
        allOptions.filter { type ->
            val matchesSearch = type.displayName.contains(searchQuery, ignoreCase = true)
            val matchesTab = selectedTab == "ALL" || type.getAtelierTab() == selectedTab
            matchesSearch && matchesTab
        }
    }

    val content = @Composable {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // 1. Header
            AtelierHeader(onDismiss = onDismiss)

            // 2. Search Bar
            AtelierSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it }
            )

            // 3. Granular Tabs
            AtelierTabs(
                tabs = tabs,
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            // 4. Grid Content
            if (filteredOptions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No widgets found",
                        style = AppTextStyles.listItemSubtitle,
                        color = AppColors.contentSecondary.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredOptions, key = { it.serialName }) { type ->
                        AtelierCardTile(
                            type = type,
                            onTap = {
                                onCardTypeSelected(type)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }

    if (LocalInspectionMode.current) {
        // In Preview, render directly to avoid Popup issues which often raise exceptions
        Surface(
            color = AppColors.sheetSurface,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            modifier = modifier
        ) {
            content()
        }
    } else {
        AeonGlassSheet(onDismiss = onDismiss) {
            content()
        }
    }
}

@Composable
private fun AtelierHeader(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 16.dp, top = 20.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "CARD STUDIO",
                style = AppTextStyles.sectionOverline.copy(
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Black
                ),
                color = AppColors.accent
            )
            Text(
                text = "Personalize your workspace",
                style = AppTextStyles.caption.copy(fontSize = 11.sp),
                color = AppColors.contentSecondary.copy(alpha = 0.8f)
            )
        }

        Box(
            modifier = Modifier
                .aeonHairlineBorder(CircleShape)
                .clip(CircleShape)
                .background(AppColors.accent.copy(alpha = 0.08f))
                .clickable(onClick = onDismiss)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "DONE",
                style = AppTextStyles.sectionOverline.copy(fontSize = 10.sp),
                color = AppColors.accent
            )
        }
    }
}

@Composable
private fun AtelierSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .height(44.dp)
            .aeonHairlineBorder(shape)
            .clip(shape)
            .background(AppColors.recessedSurface)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = AppColors.contentSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        "Search widgets...",
                        style = AppTextStyles.listItemSubtitle.copy(fontSize = 13.sp),
                        color = AppColors.contentSecondary.copy(alpha = 0.4f)
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = AppTextStyles.listItemSubtitle.copy(
                        color = AppColors.contentPrimary,
                        fontSize = 13.sp
                    ),
                    cursorBrush = SolidColor(AppColors.accent),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun AtelierTabs(
    tabs: List<String>,
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tabs) { tab ->
            val isSelected = tab == selectedTab
            val shape = RoundedCornerShape(10.dp)
            Box(
                modifier = Modifier
                    .aeonHairlineBorder(shape)
                    .clip(shape)
                    .background(if (isSelected) AppColors.accent else AppColors.recessedSurface)
                    .clickable { onTabSelected(tab) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab,
                    style = AppTextStyles.sectionOverline.copy(
                        fontSize = 8.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                    ),
                    color = if (isSelected) AppColors.onAccent else AppColors.contentSecondary
                )
            }
        }
    }
}

@Composable
private fun AtelierCardTile(
    type: CardType,
    onTap: () -> Unit
) {
    val tileShape = RoundedCornerShape(12.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .aeonHairlineBorder(tileShape)
            .clip(tileShape)
            .background(AppColors.recessedSurface)
            .clickable(onClick = onTap),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(AppColors.accent.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = type.toIcon(),
                    contentDescription = null,
                    tint = AppColors.accent,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = type.displayName,
                    style = AppTextStyles.listItemTitle.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = AppColors.contentPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = type.getIntentSubtitle(),
                    style = AppTextStyles.caption.copy(
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = AppColors.contentSecondary.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun CardType.getIntentSubtitle(): String = when (this) {
    CardType.NET_WORTH -> "Total Value"
    CardType.CASH_FLOW -> "In vs Out"
    CardType.RECENT_ACTIVITIES -> "Latest Logs"
    CardType.ACCOUNTS -> "All Wallets"
    CardType.CURRENCY_BALANCES -> "FX Summary"
    CardType.ACCOUNT_HERO -> "Main Focus"
    CardType.BUDGETS -> "Limit Status"
    CardType.PLANNED_PAYMENTS -> "Upcoming"
    CardType.LOANS -> "Debt Tracker"
    CardType.CATEGORY_PIE_CHART -> "Distribution"
    CardType.CATEGORY_TREEMAP -> "Weighting"
    CardType.CASHFLOW_COMPARISON -> "Period Growth"
    CardType.INCOME_EXPENSE_BAR -> "Net Trend"
    CardType.CATEGORY_SPENDING_TREND -> "Behavior"
    CardType.TAG_UPSET_PLOT -> "Intersections"
}

/**
 * Mapping from CardType to ImageVector icons for the UI.
 */
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

// --- Previews ---

@Preview(showBackground = true)
@Composable
fun PreviewAeonCardAtelier_Dashboard() {
    OssWalletTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Gray)
        ) {
            AeonCardAtelier(
                screenContext = ScreenContext.DASHBOARD,
                onDismiss = {},
                onCardTypeSelected = {},
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAtelierCardTile() {
    OssWalletTheme {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(modifier = Modifier.width(140.dp)) {
                AtelierCardTile(type = CardType.NET_WORTH, onTap = {})
            }
            Box(modifier = Modifier.width(140.dp)) {
                AtelierCardTile(type = CardType.CASH_FLOW, onTap = {})
            }
        }
    }
}
