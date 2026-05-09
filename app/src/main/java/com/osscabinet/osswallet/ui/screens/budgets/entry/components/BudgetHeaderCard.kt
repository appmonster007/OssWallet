package com.osscabinet.ossWallet.ui.screens.budgets.entry.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.components.CurrencySelector
import com.osscabinet.ossWallet.ui.components.entry.EntryEditorialInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Title

/**
 * Header card for Budget Entry following the Architect Slate hierarchy.
 * Amount is central with auto-resizing typography.
 */
@Composable
fun BudgetHeaderCard(
    name: String,
    onNameChange: (String) -> Unit,
    amount: String,
    currency: String,
    onCurrencyChange: (String) -> Unit,
    supportedCurrencies: List<String>,
    onAmountClick: () -> Unit
) {
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
            // Central Amount Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onAmountClick() }
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CurrencySelector(
                        selectedCurrency = currency,
                        onCurrencySelected = onCurrencyChange,
                        supportedCurrencies = supportedCurrencies
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    val amountDisplay = if (amount.isEmpty()) "0.00" else amount
                    var multiplier by remember(amountDisplay) { mutableStateOf(1f) }

                    Text(
                        text = amountDisplay,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-1).sp,
                            fontSize = (MaterialTheme.typography.displayMedium.fontSize.value * multiplier).sp
                        ),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                        onTextLayout = { textLayoutResult ->
                            if (textLayoutResult.didOverflowWidth && multiplier > 0.3f) {
                                multiplier *= 0.8f
                            }
                        },
                        color = Color(0xFF004D40), // Teal for budget healthy
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
            )

            // Entity Name at the bottom of the card
            EntryEditorialInput(
                value = name,
                onValueChange = onNameChange,
                label = "BUDGET NAME",
                icon = Icons.Rounded.Title
            )
        }
    }
}
