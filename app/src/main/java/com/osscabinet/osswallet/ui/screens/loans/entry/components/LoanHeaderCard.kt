package com.osscabinet.ossWallet.ui.screens.loans.entry.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.LoanType
import com.osscabinet.ossWallet.ui.components.CurrencySelector
import com.osscabinet.ossWallet.ui.screens.transactions.entry.components.EditorialInput

@Composable
fun LoanHeaderCard(
    loanType: LoanType,
    onTypeSelected: (LoanType) -> Unit,
    name: String,
    onNameChange: (String) -> Unit,
    amount: String,
    currency: String,
    onCurrencyChange: (String) -> Unit,
    supportedCurrencies: List<String>,
    onAmountClick: () -> Unit
) {
    val tealColor = Color(0xFF004D40)
    val orangeColor = Color(0xFFE65100)
    val amountColor = if (loanType == LoanType.LENT) tealColor else orangeColor

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
            // Type Selector at top
            LoanTypeSelector(
                loanType = loanType,
                onTypeSelected = onTypeSelected
            )

            Spacer(modifier = Modifier.height(24.dp))

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
                        color = amountColor,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
            )

            // Entity Name at the bottom
            EditorialInput(
                value = name,
                onValueChange = onNameChange,
                label = "LOAN NAME",
                icon = Icons.Rounded.Title
            )
        }
    }
}

@Composable
private fun LoanTypeSelector(
    loanType: LoanType,
    onTypeSelected: (LoanType) -> Unit
) {
    val tealColor = Color(0xFF004D40)
    val orangeColor = Color(0xFFE65100)

    val indicatorColor by animateColorAsState(
        targetValue = if (loanType == LoanType.LENT) tealColor else orangeColor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "indicatorColor"
    )

    val indicatorOffset by animateDpAsState(
        targetValue = if (loanType == LoanType.LENT) 0.dp else 120.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "indicatorOffset"
    )

    Box(
        modifier = Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(
                0.5.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                RoundedCornerShape(32.dp)
            )
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(120.dp)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(indicatorColor)
        )

        Row(modifier = Modifier.fillMaxHeight()) {
            listOf(
                LoanType.LENT to (Icons.Rounded.ArrowUpward to "Lent"),
                LoanType.BORROWED to (Icons.Rounded.ArrowDownward to "Borrowed")
            ).forEach { (type, iconLabel) ->
                val (icon, label) = iconLabel
                val isSelected = loanType == type
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.1f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "scale"
                )
                Row(
                    modifier = Modifier
                        .width(120.dp)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTypeSelected(type) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer(scaleX = scale, scaleY = scale)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
                    )
                }
            }
        }
    }
}
