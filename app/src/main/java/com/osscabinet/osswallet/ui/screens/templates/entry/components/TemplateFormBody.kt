package com.osscabinet.ossWallet.ui.screens.templates.entry.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.ui.components.getAccountIconByName
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.components.entry.*

@Composable
fun TemplateFormBody(
    name: String,
    onNameChange: (String) -> Unit,
    transactionType: Int,
    onTypeSelected: (Int) -> Unit,
    amount: String,
    currency: String,
    onAmountClick: () -> Unit,
    selectedAccount: Account?,
    onAccountClick: () -> Unit,
    targetAccount: Account? = null,
    onTargetAccountClick: () -> Unit = {},
    categoryName: String,
    categoryObj: Category?,
    onCategoryClick: () -> Unit,
    payee: String,
    onPayeeChange: (String) -> Unit,
    uniquePayees: List<String>,
    tags: List<String>,
    onShowTagSheet: () -> Unit,
    onRemoveTag: (String) -> Unit,
    paymentMethod: String,
    onPaymentMethodClick: () -> Unit,
    location: String,
    onLocationChange: (String) -> Unit,
    note: String,
    onNoteChange: (String) -> Unit,
    uniqueNotes: List<String>,
    transferRate: java.math.BigDecimal = java.math.BigDecimal.ONE,
    targetCurrency: String? = null,
    onTargetCurrencyClick: () -> Unit = {},
    onTransferRateClick: () -> Unit = {},
    onCurrencyClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val amountColor = when (transactionType) {
        0 -> Color(0xFFB3261E)
        1 -> Color(0xFF004D40)
        else -> MaterialTheme.colorScheme.primary
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Card: Type & Amount (Aeon Style)
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
                // Transaction Type Row
                EntryTypeSelector(
                    selectedType = transactionType,
                    onTypeSelected = onTypeSelected,
                    types = listOf(
                        Triple(Icons.Rounded.Remove, "Expense", 0),
                        Triple(Icons.Rounded.Add, "Income", 1),
                        Triple(Icons.Rounded.SwapHoriz, "Transfer", 2)
                    ),
                    selectedColor = amountColor,
                    itemWidth = 100.dp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Amount Area
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) { onAmountClick() }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(modifier = Modifier.clickable { onCurrencyClick() }) {
                            Text(
                                text = currency,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (amount.isEmpty()) "0.00" else amount,
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-1).sp
                            ),
                            color = if (amount.isEmpty()) amountColor.copy(alpha = 0.3f) else amountColor
                        )
                    }

                    // Transfer-specific secondary amount
                    if (transactionType == 2 && targetAccount != null) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            onClick = onTransferRateClick,
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.CurrencyExchange,
                                    contentDescription = "Edit Exchange Rate",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "1:${transferRate.setScale(3, java.math.RoundingMode.HALF_UP).toPlainString()}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            val amountValue = amount.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO
                            val convertedAmount = amountValue.multiply(transferRate)
                            val convertedText = convertedAmount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()

                            Box(modifier = Modifier.clickable { onTargetCurrencyClick() }) {
                                Text(
                                    text = targetCurrency ?: currency,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = convertedText,
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                )

                EntryEditorialInput(
                    value = name,
                    onValueChange = onNameChange,
                    label = "TEMPLATE NAME",
                    icon = Icons.Rounded.Bookmark
                )
            }
        }

        EntryActionRow {
            EntryActionCard(
                icon = getAccountIconByName(selectedAccount?.icon),
                label = if (transactionType == 2) "FROM" else "ACCOUNT",
                value = selectedAccount?.name ?: "Select",
                onClick = onAccountClick,
                modifier = Modifier.weight(1f),
                iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                iconTint = MaterialTheme.colorScheme.primary
            )

            if (transactionType == 2) {
                EntryActionCard(
                    icon = getAccountIconByName(targetAccount?.icon),
                    label = "TO",
                    value = targetAccount?.name ?: "Select",
                    onClick = onTargetAccountClick,
                    modifier = Modifier.weight(1f),
                    iconContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                    iconTint = MaterialTheme.colorScheme.tertiary
                )
            } else {
                EntryActionCard(
                    icon = getIconByName(categoryObj?.iconName),
                    label = "CATEGORY",
                    value = categoryName,
                    onClick = onCategoryClick,
                    modifier = Modifier.weight(1f),
                    iconContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                    iconTint = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        EntrySection(
            title = "TEMPLATE DETAILS",
            icon = Icons.Rounded.MoreHoriz
        ) {
            EntryEditorialInput(
                value = payee,
                onValueChange = onPayeeChange,
                label = "PAYEE",
                icon = Icons.Rounded.Person,
                suggestions = uniquePayees
            )

            EntryTagSection(
                selectedTags = tags,
                onShowTagSheet = onShowTagSheet,
                onRemoveTag = onRemoveTag
            )

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

                Spacer(modifier = Modifier.weight(1f))
            }

            EntryEditorialInput(
                value = location,
                onValueChange = onLocationChange,
                label = "LOCATION",
                icon = Icons.Rounded.LocationOn
            )

            EntryEditorialInput(
                value = note,
                onValueChange = onNoteChange,
                label = "NOTE",
                icon = Icons.AutoMirrored.Rounded.Notes,
                suggestions = uniqueNotes
            )
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}
