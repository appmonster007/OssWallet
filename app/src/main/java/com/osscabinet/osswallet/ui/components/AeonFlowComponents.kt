package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.osscabinet.ossWallet.domain.model.CashFlowType
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
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
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = MaterialTheme.colorScheme.primary
        )
        if (trailing != null) {
            trailing()
        }
    }
}

@Composable
fun AeonFlowBadge(
    amount: BigDecimal,
    type: CashFlowType,
    currency: String,
    modifier: Modifier = Modifier,
    incomeColor: Color = Color(0xFF4CAF50),
    expenseColor: Color = Color(0xFFF44336),
    precision: Int = 2
) {
    Surface(
        color = (if (type == CashFlowType.INCOME) incomeColor else expenseColor).copy(alpha = 0.15f),
        contentColor = if (type == CashFlowType.INCOME) incomeColor else expenseColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(
            text = (if (type == CashFlowType.INCOME) "+" else "-") + formatCurrencyRight(amount, currency, precision).trim(),
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
