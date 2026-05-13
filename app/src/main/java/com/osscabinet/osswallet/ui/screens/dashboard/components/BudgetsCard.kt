package com.osscabinet.ossWallet.ui.screens.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.BudgetAlertLevel
import com.osscabinet.ossWallet.domain.model.BudgetProgress
import com.osscabinet.ossWallet.domain.model.settings.BudgetsCardConfig

import androidx.compose.foundation.shape.RoundedCornerShape
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import androidx.compose.ui.unit.sp

import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatAmount
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatCurrencyRight

@Composable
fun BudgetsCard(
    items: List<BudgetProgress>,
    config: BudgetsCardConfig,
    onUpdate: (BudgetsCardConfig) -> Unit,
    onBudgetClick: (Long) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    precision: Int = 2
) {
    var showMenu by remember { mutableStateOf(false) }

    AeonExpressiveCard(
        title = config.title,
        icon = Icons.Rounded.DonutLarge,
        modifier = modifier,
        headerModifier = headerModifier,
        headerTrailing = { _ ->
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Show archived") },
                        trailingIcon = {
                            Checkbox(checked = config.showArchived, onCheckedChange = null)
                        },
                        onClick = {
                            onUpdate(config.copy(showArchived = !config.showArchived))
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete card") },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        onClick = { showMenu = false; onDelete() }
                    )
                }
            }
        }
    ) {
        if (items.isEmpty()) {
            Text(
                text = "No budgets yet",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items.forEach { progress ->
                    BudgetProgressRow(
                        progress = progress,
                        onClick = { onBudgetClick(progress.budget.id) },
                        precision = precision
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetProgressRow(
    progress: BudgetProgress,
    onClick: () -> Unit,
    precision: Int = 2
) {
    val healthyColor = Color(0xFF00695C)
    val warningColor = Color(0xFFC62828)
    
    val trackColor = when (progress.alertLevel) {
        BudgetAlertLevel.EXCEEDED -> warningColor
        BudgetAlertLevel.WARNING -> warningColor.copy(alpha = 0.8f)
        BudgetAlertLevel.NONE -> healthyColor
    }

    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        if (progress.alertLevel == BudgetAlertLevel.EXCEEDED) Icons.Rounded.ErrorOutline else Icons.Rounded.Savings,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = trackColor.copy(alpha = 0.7f)
                    )
                    Text(
                        text = progress.budget.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.2).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "${(progress.percentage * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontFeatureSettings = "tnum"
                    ),
                    color = trackColor
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LinearProgressIndicator(
                progress = { progress.percentage.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(10.dp),
                color = trackColor,
                trackColor = trackColor.copy(alpha = 0.1f),
                strokeCap = StrokeCap.Round
            )
            
            Spacer(modifier = Modifier.height(14.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (progress.alertLevel == BudgetAlertLevel.EXCEEDED) "OVER BUDGET" else "REMAINING",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = trackColor.copy(alpha = 0.6f)
                    )
                    Text(
                        text = if (progress.alertLevel == BudgetAlertLevel.EXCEEDED) {
                            formatCurrencyRight(progress.spent.subtract(progress.limit), progress.budget.currency, precision, shorten = true)
                        } else {
                            formatCurrencyRight(progress.remaining, progress.budget.currency, precision, shorten = true)
                        },
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = if (progress.alertLevel == BudgetAlertLevel.EXCEEDED) warningColor else MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
                
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${formatAmount(progress.spent, precision)} / ${formatAmount(progress.limit, precision)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum"
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun BudgetsCardPreview() {
    val budget1 = com.osscabinet.ossWallet.domain.model.Budget(
        id = 1L, name = "Food", currency = "USD", limitAmount = java.math.BigDecimal("1000.00"),
        period = com.osscabinet.ossWallet.domain.model.BudgetPeriod.MONTHLY, profileId = 1
    )
    val budget2 = com.osscabinet.ossWallet.domain.model.Budget(
        id = 2L, name = "Rent", currency = "USD", limitAmount = java.math.BigDecimal("1200.00"),
        period = com.osscabinet.ossWallet.domain.model.BudgetPeriod.MONTHLY, profileId = 1
    )
    
    val budgets = listOf(
        com.osscabinet.ossWallet.domain.model.BudgetProgress(
            budget = budget1,
            spent = java.math.BigDecimal("450.00"),
            limit = java.math.BigDecimal("1000.00"),
            remaining = java.math.BigDecimal("550.00"),
            percentage = 0.45f,
            alertLevel = com.osscabinet.ossWallet.domain.model.BudgetAlertLevel.NONE
        ),
        com.osscabinet.ossWallet.domain.model.BudgetProgress(
            budget = budget2,
            spent = java.math.BigDecimal("1250.00"),
            limit = java.math.BigDecimal("1200.00"),
            remaining = java.math.BigDecimal("-50.00"),
            percentage = 1.04f,
            alertLevel = com.osscabinet.ossWallet.domain.model.BudgetAlertLevel.EXCEEDED
        )
    )

    com.osscabinet.ossWallet.ui.theme.OssWalletTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            BudgetsCard(
                items = budgets,
                config = com.osscabinet.ossWallet.domain.model.settings.BudgetsCardConfig(cardId = "preview", title = "Budgets"),
                onUpdate = {},
                onBudgetClick = {},
                onDelete = {}
            )
        }
    }
}
