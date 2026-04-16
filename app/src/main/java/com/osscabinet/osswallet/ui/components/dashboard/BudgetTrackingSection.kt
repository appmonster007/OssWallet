package com.osscabinet.ossWallet.ui.components.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.ui.components.AeonBudgetProgress
import com.osscabinet.ossWallet.ui.components.AeonFlowSectionHeader
import com.osscabinet.ossWallet.ui.components.AeonSettingsToggle
import kotlinx.serialization.Serializable

import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import java.math.BigDecimal

@Serializable
data class BudgetSettings(
    val showProgressLabels: Boolean = true
)

@Composable
fun BudgetTrackingSection(
    onManageClick: () -> Unit,
    budgets: List<Budget>,
    baseCurrency: String,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    precision: Int = 2,
    showProgressLabels: Boolean = true,
    onShowProgressLabelsChange: (Boolean) -> Unit = {}
) {
    var showSettings by remember { mutableStateOf(false) }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AeonFlowSectionHeader(
                title = "BUDGET TRACKING",
                modifier = headerModifier,
                trailing = {
                    IconButton(
                        onClick = { showSettings = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreHoriz,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )

            Column(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                budgets.forEach { budget ->
                    AeonBudgetProgress(
                        label = budget.category,
                        current = budget.spent,
                        total = budget.limit,
                        color = if (budget.spent > budget.limit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        baseCurrency = baseCurrency,
                        precision = precision,
                        showLabels = showProgressLabels
                    )
                }
            }
        }
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            confirmButton = {
                TextButton(onClick = { showSettings = false }) {
                    Text("Done")
                }
            },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Budget Settings",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(
                        onClick = {
                            onManageClick()
                            showSettings = false
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Manage Budgets"
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    AeonSettingsToggle(
                        label = "SHOW PROGRESS LABELS",
                        checked = showProgressLabels,
                        onCheckedChange = onShowProgressLabelsChange
                    )
                }
            },
            shape = RoundedCornerShape(32.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BudgetTrackingSectionPreview() {
    val sampleBudgets = listOf(
        Budget(
            category = "Food",
            spent = BigDecimal("450.00"),
            limit = BigDecimal("500.00"),
            currency = "USD"
        ),
        Budget(
            category = "Entertainment",
            spent = BigDecimal("120.00"),
            limit = BigDecimal("100.00"),
            currency = "USD"
        )
    )

    OssWalletTheme {
        Surface {
            BudgetTrackingSection(
                onManageClick = {},
                budgets = sampleBudgets,
                baseCurrency = "USD",
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
