package com.osscabinet.ossWallet.ui.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.UpcomingPaymentItem
import com.osscabinet.ossWallet.domain.model.settings.PlannedPaymentsCardConfig
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

import androidx.compose.foundation.shape.RoundedCornerShape
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import androidx.compose.ui.unit.sp

import com.osscabinet.ossWallet.ui.util.formatAmount

@Composable
fun PlannedPaymentsCard(
    upcoming: List<UpcomingPaymentItem>,
    overdue: List<PlannedPayment>,
    config: PlannedPaymentsCardConfig,
    onUpdate: (PlannedPaymentsCardConfig) -> Unit,
    onItemClick: (Long) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    precision: Int = 2
) {
    var showMenu by remember { mutableStateOf(false) }

    AeonExpressiveCard(
        title = config.title,
        icon = Icons.Rounded.EventRepeat,
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
                        text = { Text("Show overdue") },
                        trailingIcon = {
                            Checkbox(
                                checked = config.showOverdue,
                                onCheckedChange = null
                            )
                        },
                        onClick = {
                            onUpdate(config.copy(showOverdue = !config.showOverdue))
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete card") },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (config.showOverdue && overdue.isNotEmpty()) {
                overdue.take(3).forEach { payment ->
                    PlannedPaymentRow(
                        title = payment.title,
                        amount = payment.amount,
                        currency = payment.currency,
                        dateLabel = "OVERDUE",
                        isOverdue = true,
                        onClick = { onItemClick(payment.id) },
                        precision = precision
                    )
                }
            }

            if (upcoming.isEmpty() && overdue.isEmpty()) {
                Text(
                    text = "No upcoming payments",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                upcoming.take(config.maxItems).forEach { item ->
                    PlannedPaymentRow(
                        title = item.plannedPayment.title,
                        amount = item.plannedPayment.amount,
                        currency = item.plannedPayment.currency,
                        dateLabel = formatDate(item.scheduledDate),
                        isOverdue = item.isOverdue,
                        onClick = { onItemClick(item.plannedPayment.id) },
                        precision = precision
                    )
                }
            }
        }
    }
}

@Composable
private fun PlannedPaymentRow(
    title: String,
    amount: BigDecimal,
    currency: String,
    dateLabel: String,
    isOverdue: Boolean,
    onClick: () -> Unit,
    precision: Int = 2
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = if (isOverdue) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.3f),
        border = BorderStroke(0.5.dp, if (isOverdue) MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                                     else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                )
                Text(
                    text = dateLabel.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        letterSpacing = 0.5.sp,
                        color = if (isOverdue) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )
            }
            Text(
                text = "${formatAmount(amount, precision)} $currency",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isOverdue) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

private fun formatDate(epochMilli: Long): String {
    val date = Instant.ofEpochMilli(epochMilli).atOffset(ZoneOffset.UTC).toLocalDate()
    val today = LocalDate.now(ZoneOffset.UTC)
    return when (date) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        else -> date.format(DateTimeFormatter.ofPattern("MMM d"))
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun PlannedPaymentsCardPreview() {
    val rule = com.osscabinet.ossWallet.domain.model.RecurrenceRule(
        type = com.osscabinet.ossWallet.domain.model.RecurrenceType.MONTHLY,
        interval = 1
    )
    val upcoming = listOf(
        UpcomingPaymentItem(
            plannedPayment = PlannedPayment(
                id = 1L, title = "Spotify", amount = java.math.BigDecimal("10.00"), currency = "USD",
                transactionType = "expense", accountId = "1", recurrenceRule = rule,
                startDate = System.currentTimeMillis(), nextDueDate = System.currentTimeMillis() + 86400000L
            ),
            scheduledDate = System.currentTimeMillis() + 86400000L,
            isOverdue = false
        ),
        UpcomingPaymentItem(
            plannedPayment = PlannedPayment(
                id = 2L, title = "Internet", amount = java.math.BigDecimal("60.00"), currency = "USD",
                transactionType = "expense", accountId = "1", recurrenceRule = rule,
                startDate = System.currentTimeMillis(), nextDueDate = System.currentTimeMillis() + 172800000L
            ),
            scheduledDate = System.currentTimeMillis() + 172800000L,
            isOverdue = false
        )
    )

    com.osscabinet.ossWallet.ui.theme.OssWalletTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            PlannedPaymentsCard(
                upcoming = upcoming,
                overdue = emptyList(),
                config = com.osscabinet.ossWallet.domain.model.settings.PlannedPaymentsCardConfig(cardId = "preview", title = "Upcoming"),
                onUpdate = {},
                onItemClick = {},
                onDelete = {}
            )
        }
    }
}
