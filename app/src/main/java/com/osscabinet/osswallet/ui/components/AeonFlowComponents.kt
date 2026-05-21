package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.osscabinet.ossWallet.domain.model.CashFlowType
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.ui.components.transactions.countActiveFilters
import com.osscabinet.ossWallet.ui.theme.NegativeAmountColor
import com.osscabinet.ossWallet.ui.theme.PositiveAmountColor
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatCurrencyRight
import java.math.BigDecimal
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

@Composable
fun AeonSettingsToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    thumbContent: (@Composable () -> Unit)? = null
) {
    Surface(
        color = AppColors.insetSurface.copy(alpha = 0.3f),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label.uppercase(),
                    style = AppTextStyles.transactionSwipeAction.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = AppColors.primary
                )
                if (description != null) {
                    Text(
                        description,
                        style = AppTextStyles.caption,
                        color = AppColors.contentSecondary
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                thumbContent = thumbContent
            )
        }
    }
}

@Composable
fun AeonBaseConversionToggle(
    baseCurrency: String,
    showBaseConversion: Boolean,
    onShowBaseConversionChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    AeonSettingsToggle(
        label = "BASE CONVERSION",
        description = "Show estimated value in $baseCurrency",
        checked = showBaseConversion,
        onCheckedChange = onShowBaseConversionChange,
        modifier = modifier
    )
}

@Composable
fun AeonLayoutToggle(
    layout: String,
    onLayoutChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "LAYOUT"
) {
    AeonSettingsToggle(
        label = label,
        description = if (layout == "grid") "Grid Layout" else "Horizontal Overflow",
        checked = layout == "grid",
        onCheckedChange = { onLayoutChange(if (it) "grid" else "scroll") },
        thumbContent = {
            if (layout == "grid") {
                Icon(
                    imageVector = Icons.Default.GridView,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.ViewCarousel,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize)
                )
            }
        },
        modifier = modifier
    )
}

@Composable
fun AeonGridColumnsSettings(
    gridColumns: Int,
    onGridColumnsChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    maxColumns: Int = 3
) {
    Surface(
        color = AppColors.insetSurface.copy(alpha = 0.3f),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "GRID COLUMNS",
                style = AppTextStyles.transactionSwipeAction.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = AppColors.primary
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                (1..maxColumns).forEachIndexed { index, count ->
                    SegmentedButton(
                        selected = gridColumns == count,
                        onClick = { onGridColumnsChange(count) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = maxColumns
                        ),
                        label = { Text(count.toString()) }
                    )
                }
            }
        }
    }
}

@Composable
fun AeonFlowSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    breadcrumbs: List<String> = emptyList(),
    onBreadcrumbClick: (Int) -> Unit = {},
    filter: TransactionFilter? = null
) {
    val haptic = LocalHapticFeedback.current
    val activeFilterCount = filter?.let { countActiveFilters(it) } ?: 0
    
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    title.uppercase(),
                    style = AppTextStyles.transactionSwipeAction.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                        fontSize = 11.sp
                    ),
                    color = AppColors.primary.copy(alpha = 0.8f)
                )

                if (activeFilterCount > 0) {
                    FilterActiveIndicator(count = activeFilterCount)
                }
            }
            if (trailing != null) {
                trailing()
            }
        }

        if (breadcrumbs.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home",
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onBreadcrumbClick(-1) 
                        },
                    tint = AppColors.contentSecondary
                )
                
                breadcrumbs.forEachIndexed { index, crumb ->
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = AppColors.contentSecondary.copy(alpha = 0.5f)
                    )
                    Text(
                        text = crumb.uppercase(),
                        style = AppTextStyles.transactionSwipeAction.copy(
                            fontSize = 11.sp,
                            fontWeight = if (index == breadcrumbs.size - 1) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (index == breadcrumbs.size - 1) 
                            AppColors.primary 
                        else 
                            AppColors.contentSecondary,
                        modifier = Modifier.clickable { 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onBreadcrumbClick(index) 
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun FilterActiveIndicator(count: Int) {
    Surface(
        color = AppColors.selectionContainer,
        contentColor = AppColors.onSelectionContainer,
        shape = CircleShape,
        modifier = Modifier.size(24.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Icon(Icons.Default.FilterAlt, null, modifier = Modifier.size(12.dp))
                Text(count.toString(), style = AppTextStyles.chipLabel)
            }
        }
    }
}

@Composable
fun AeonFlowBadge(
    amount: BigDecimal,
    type: CashFlowType,
    currency: String,
    modifier: Modifier = Modifier,
    incomeColor: Color = PositiveAmountColor,
    expenseColor: Color = NegativeAmountColor,
    onClick: (() -> Unit)? = null
) {
    val color = if (type == CashFlowType.INCOME) incomeColor else expenseColor
    Surface(
        color = color.copy(alpha = 0.15f),
        contentColor = color,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = if (onClick != null) modifier.clickable { onClick() } else modifier
    ) {
        Text(
            text = formatCurrencyRight(amount, currency, shorten = true).trim(),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = AppTextStyles.transactionSwipeAction.copy(
                fontWeight = FontWeight.Black,
                fontSize = 8.sp,
                fontFeatureSettings = "tnum"
            )
        )
    }
}

// ── PREVIEWS ─────────────────────────────────────────────────────────────

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun AeonFlowComponentsPreview() {
    OssWalletTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Settings Toggle", style = AppTextStyles.listSectionHeader)
            AeonSettingsToggle(
                label = "Notifications",
                description = "Enable push notifications for your account.",
                checked = true,
                onCheckedChange = {}
            )
            
            Text("Base Conversion Toggle", style = AppTextStyles.listSectionHeader)
            AeonBaseConversionToggle(
                baseCurrency = "USD",
                showBaseConversion = true,
                onShowBaseConversionChange = {}
            )
            
            Text("Layout Toggle", style = AppTextStyles.listSectionHeader)
            AeonLayoutToggle(
                layout = "grid",
                onLayoutChange = {}
            )
            
            Text("Grid Columns Settings", style = AppTextStyles.listSectionHeader)
            AeonGridColumnsSettings(
                gridColumns = 2,
                onGridColumnsChange = {}
            )
            
            Text("Flow Section Header", style = AppTextStyles.listSectionHeader)
            AeonFlowSectionHeader(
                title = "Transactions",
                breadcrumbs = listOf("Accounts", "Savings"),
                onBreadcrumbClick = {}
            )
            
            Text("Flow Badges", style = AppTextStyles.listSectionHeader)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AeonFlowBadge(
                    amount = BigDecimal("1500.00"),
                    type = CashFlowType.INCOME,
                    currency = "USD"
                )
                AeonFlowBadge(
                    amount = BigDecimal("850.00"),
                    type = CashFlowType.EXPENSE,
                    currency = "USD"
                )
            }
            
            Text("Filter Indicator", style = AppTextStyles.listSectionHeader)
            FilterActiveIndicator(count = 3)
        }
    }
}
