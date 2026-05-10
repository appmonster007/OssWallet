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
import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

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
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
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
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                if (description != null) {
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "GRID COLUMNS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.primary
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
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp,
                        fontSize = 11.sp
                    ),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
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
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                breadcrumbs.forEachIndexed { index, crumb ->
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = crumb.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = if (index == breadcrumbs.size - 1) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (index == breadcrumbs.size - 1) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant,
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
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = CircleShape,
        modifier = Modifier.size(24.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Icon(Icons.Default.FilterAlt, null, modifier = Modifier.size(12.dp))
                Text(count.toString(), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold))
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
    precision: Int = 2,
    onClick: (() -> Unit)? = null
) {
    val color = if (type == CashFlowType.INCOME) incomeColor else expenseColor
    Surface(
        color = color.copy(alpha = 0.1f),
        contentColor = color,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, color.copy(alpha = 0.2f)),
        modifier = if (onClick != null) modifier.clickable { onClick() } else modifier
    ) {
        Text(
            text = formatCurrencyRight(amount, currency, precision, shorten = true).trim(),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                fontSize = 10.sp,
                fontFeatureSettings = "tnum"
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AeonSettingsTogglePreview() {
    OssWalletTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AeonSettingsToggle(
                label = "Notifications",
                checked = true,
                onCheckedChange = {},
                description = "Enable push notifications"
            )
            AeonSettingsToggle(
                label = "Dark Mode",
                checked = false,
                onCheckedChange = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AeonBaseConversionTogglePreview() {
    OssWalletTheme {
        AeonBaseConversionToggle(
            baseCurrency = "USD",
            showBaseConversion = true,
            onShowBaseConversionChange = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AeonLayoutTogglePreview() {
    OssWalletTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            AeonLayoutToggle(
                layout = "grid",
                onLayoutChange = {}
            )
            Spacer(modifier = Modifier.height(8.dp))
            AeonLayoutToggle(
                layout = "scroll",
                onLayoutChange = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AeonGridColumnsSettingsPreview() {
    OssWalletTheme {
        AeonGridColumnsSettings(
            gridColumns = 2,
            onGridColumnsChange = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AeonFlowSectionHeaderPreview() {
    OssWalletTheme {
        AeonFlowSectionHeader(
            title = "TRANSACTION HISTORY",
            modifier = Modifier.padding(16.dp),
            trailing = {
                Text(
                    "SEE ALL",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AeonFlowBadgePreview() {
    OssWalletTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AeonFlowBadge(
                    amount = BigDecimal("1500.00"),
                    type = CashFlowType.INCOME,
                    currency = "USD"
                )
                AeonFlowBadge(
                    amount = BigDecimal("450.50"),
                    type = CashFlowType.EXPENSE,
                    currency = "USD"
                )
            }
        }
    }
}
