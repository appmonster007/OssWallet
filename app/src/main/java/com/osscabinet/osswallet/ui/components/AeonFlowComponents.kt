package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.CashFlowType
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatAuto
import com.osscabinet.ossWallet.ui.components.transactions.countActiveFilters
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.NegativeAmountColor
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.PositiveAmountColor
import com.osscabinet.ossWallet.ui.util.isDark
import java.math.BigDecimal

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
        color = AppColors.surfaceContainerLow,
        shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, AppColors.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AeonTheme.dimensions.cardPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label.uppercase(),
                    style = AppTextStyles.sectionOverline,
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
                thumbContent = thumbContent,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AppColors.primary,
                    checkedTrackColor = AppColors.switchTrackOn,
                    checkedBorderColor = AppColors.primary,
                    uncheckedThumbColor = AppColors.switchThumbOff,
                    uncheckedTrackColor = AppColors.switchTrackOff,
                    uncheckedBorderColor = AppColors.outline
                )
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
        color = AppColors.surfaceContainerLow,
        shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, AppColors.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(AeonTheme.dimensions.cardPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "GRID COLUMNS",
                style = AppTextStyles.sectionOverline,
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
                        label = { Text(count.toString(), style = AppTextStyles.chipLabel) },
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = AppColors.primary,
                            activeContentColor = if (AppColors.primary.isDark()) Color.White else Color.Black,
                            inactiveContainerColor = AppColors.surfaceContainerHigh,
                            inactiveContentColor = AppColors.contentSecondary
                        )
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)
            ) {
                Text(
                    title.uppercase(),
                    style = AppTextStyles.sectionOverline,
                    color = AppColors.primary
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
                        tint = AppColors.outline
                    )
                    Text(
                        text = crumb.uppercase(),
                        style = if (index == breadcrumbs.size - 1) AppTextStyles.chipLabelBold else AppTextStyles.chipLabel,
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
        color = AppColors.primaryContainer,
        contentColor = AppColors.onPrimaryContainer,
        shape = CircleShape,
        modifier = Modifier.size(24.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.FilterAlt, null, modifier = Modifier.size(12.dp))
                Text(count.toString(), style = AppTextStyles.caption)
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
            text = formatAuto(amount, currency, shorten = true).trim(),
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
