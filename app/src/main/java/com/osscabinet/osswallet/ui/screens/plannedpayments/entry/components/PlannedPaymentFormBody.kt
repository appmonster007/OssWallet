package com.osscabinet.ossWallet.ui.screens.plannedpayments.entry.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.ui.components.getAccountIconByName
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.components.entry.*

@Composable
fun PlannedPaymentFormBody(
    title: String,
    onTitleChange: (String) -> Unit,
    transactionType: String,
    onTypeSelected: (Boolean) -> Unit,
    amountText: String,
    currency: String,
    onCurrencyClick: () -> Unit,
    onAmountClick: () -> Unit,
    scheduleSummary: String,
    onScheduleClick: () -> Unit,
    selectedAccount: Account?,
    onAccountClick: () -> Unit,
    categoryObj: Category?,
    onCategoryClick: () -> Unit,
    payee: String,
    onPayeeChange: (String) -> Unit,
    uniquePayees: List<String>,
    noteUserPart: String,
    onNoteUserPartChange: (String) -> Unit,
    uniqueNotes: List<String>,
    selectedTagNames: List<String>,
    onShowTagSheet: () -> Unit,
    onRemoveTag: (String) -> Unit,
    paymentMethod: String,
    onPaymentMethodClick: () -> Unit,
    status: PlannedPaymentStatus,
    onStatusClick: () -> Unit,
    hasEndDate: Boolean,
    onHasEndDateChange: (Boolean) -> Unit,
    endDateMillis: Long,
    onEndDateClick: () -> Unit,
    autoConfirm: Boolean,
    onAutoConfirmChange: (Boolean) -> Unit,
    allCurrencies: List<String>,
    modifier: Modifier = Modifier
) {
    val amountColor = if (transactionType == "income") Color(0xFF004D40) else Color(0xFFB3261E)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header card: type selector + amount
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)),
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                EntryEditorialInput(
                    value = title,
                    onValueChange = onTitleChange,
                    label = "PAYMENT NAME",
                    icon = Icons.Rounded.Title
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                )
                EntryTypeSelector(
                    selectedType = transactionType == "income",
                    onTypeSelected = onTypeSelected,
                    types = listOf(
                        Triple(Icons.Rounded.Remove, "Expense", false),
                        Triple(Icons.Rounded.Add, "Income", true)
                    ),
                    selectedColor = amountColor,
                    itemWidth = 120.dp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Amount row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(modifier = Modifier.clickable { onCurrencyClick() }) {
                        Text(
                            text = currency,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onAmountClick() }
                    ) {
                        Text(
                            text = if (amountText.isEmpty()) "0.00" else amountText,
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-1).sp
                            ),
                            color = if (amountText.isEmpty()) amountColor.copy(alpha = 0.3f) else amountColor
                        )
                    }
                }
            }
        }

        // Row 1: SCHEDULE card (full width)
        EntryActionCard(
            icon = Icons.Rounded.DateRange,
            label = "SCHEDULE",
            value = scheduleSummary,
            onClick = onScheduleClick,
            modifier = Modifier.fillMaxWidth(),
            iconContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
            iconTint = MaterialTheme.colorScheme.secondary
        )

        // Row 2: Account + Category
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EntryActionCard(
                icon = getAccountIconByName(selectedAccount?.icon),
                label = "ACCOUNT",
                value = selectedAccount?.name ?: "Select",
                onClick = onAccountClick,
                modifier = Modifier.weight(1f),
                iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                iconTint = MaterialTheme.colorScheme.primary
            )

            EntryActionCard(
                icon = getIconByName(categoryObj?.iconName),
                label = "CATEGORY",
                value = categoryObj?.name ?: "None",
                onClick = onCategoryClick,
                modifier = Modifier.weight(1f),
                iconContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                iconTint = MaterialTheme.colorScheme.tertiary
            )
        }

        // Details card
        EntrySection(
            title = "PAYMENT DETAILS",
            icon = Icons.Rounded.MoreHoriz
        ) {
            EntryEditorialInput(
                value = payee,
                onValueChange = onPayeeChange,
                label = "PAYEE",
                icon = Icons.Rounded.Person,
                suggestions = uniquePayees
            )
            EntryEditorialInput(
                value = noteUserPart,
                onValueChange = onNoteUserPartChange,
                label = "NOTES",
                icon = Icons.AutoMirrored.Rounded.Notes,
                suggestions = uniqueNotes,
                maxLines = 4
            )

            EntryTagSection(
                selectedTags = selectedTagNames,
                onShowTagSheet = onShowTagSheet,
                onRemoveTag = onRemoveTag
            )

            // Row for Payment Method and Status
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val methods = listOf(
                    "Cash" to Icons.Rounded.Payments,
                    "Credit Card" to Icons.Rounded.CreditCard,
                    "Debit Card" to Icons.Rounded.CreditCard,
                    "Bank Transfer" to Icons.Rounded.AccountBalance,
                    "Digital Wallet" to Icons.Rounded.Wallet,
                    "Mobile Payment" to Icons.Rounded.Contactless,
                    "Check" to Icons.Rounded.Description,
                    "Cryptocurrency" to Icons.Rounded.CurrencyBitcoin,
                    "Other" to Icons.Rounded.MoreHoriz
                )
                val paymentIcon = methods.find { it.first == paymentMethod }?.second ?: Icons.Rounded.Contactless

                EntryActionChip(
                    icon = paymentIcon,
                    label = paymentMethod,
                    onClick = onPaymentMethodClick,
                    modifier = Modifier.weight(1f)
                )

                val (statusIcon, statusColor) = when (status) {
                    PlannedPaymentStatus.ACTIVE -> Icons.Rounded.CheckCircle to Color(0xFF4CAF50)
                    PlannedPaymentStatus.PAUSED -> Icons.Rounded.PauseCircle to Color(0xFFFFC107)
                    PlannedPaymentStatus.COMPLETED -> Icons.Rounded.CheckCircle to Color(0xFF2196F3)
                    PlannedPaymentStatus.CANCELLED -> Icons.Rounded.Cancel to Color(0xFFF44336)
                }

                EntryActionChip(
                    icon = statusIcon,
                    label = status.name.lowercase().replaceFirstChar { it.uppercase() },
                    onClick = onStatusClick,
                    modifier = Modifier.weight(1f),
                    iconContainerColor = statusColor.copy(alpha = 0.1f),
                    iconTint = statusColor
                )
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
            )

            // End date toggle row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.EventBusy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                Text(
                    text = "Set end date",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = hasEndDate,
                    onCheckedChange = onHasEndDateChange,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = Color(0xFF004D40)
                    )
                )
            }

            if (hasEndDate) {
                EntryDateCard(
                    icon = Icons.Rounded.CalendarMonth,
                    label = "END DATE",
                    dateMillis = endDateMillis,
                    onClick = onEndDateClick
                )
            }

            // Auto-confirm toggle row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.AutoMode,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto-confirm",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "Automatically log when due",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                Switch(
                    checked = autoConfirm,
                    onCheckedChange = onAutoConfirmChange,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = Color(0xFF004D40)
                    )
                )
            }
        }
    }
}
