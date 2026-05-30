package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.ColorOnDark
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

/**
 * Animated type selector for entry screens.
 * Following the "Architect Slate" design specification.
 */
@Composable
fun <T> EntryTypeSelector(
    selectedType: T,
    onTypeSelected: (T) -> Unit,
    types: List<Triple<ImageVector, String, T>>,
    selectedColor: Color = AppColors.primary,
    itemWidth: androidx.compose.ui.unit.Dp = 80.dp
) {
    val selectedIndex = types.indexOfFirst { it.third == selectedType }.coerceAtLeast(0)
    val haptic = LocalHapticFeedback.current

    val indicatorColor by animateColorAsState(
        targetValue = selectedColor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "indicatorColor"
    )

    val indicatorOffset by animateDpAsState(
        targetValue = itemWidth * selectedIndex,
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
            .background(AppColors.recessedSurface)
            .border(0.5.dp, AppColors.divider.copy(alpha = 0.15f), RoundedCornerShape(32.dp))
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .offset { androidx.compose.ui.unit.IntOffset(indicatorOffset.roundToPx(), 0) }
                .width(itemWidth)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(indicatorColor)
        )

        Row(
            modifier = Modifier.fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            types.forEach { (icon, label, type) ->
                val isSelected = selectedType == type
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.1f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "iconScale"
                )

                Box(
                    modifier = Modifier
                        .width(itemWidth)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTypeSelected(type)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) ColorOnDark else AppColors.contentSecondary.copy(
                            alpha = 0.6f
                        ),
                        modifier = Modifier
                            .size(26.dp)
                            .graphicsLayer(scaleX = scale, scaleY = scale)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EntryTypeSelectorPreview() {
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    val types = listOf(
        Triple(Icons.Rounded.Remove, "Expense", TransactionType.EXPENSE),
        Triple(Icons.Rounded.Add, "Income", TransactionType.INCOME),
        Triple(Icons.Rounded.SwapHoriz, "Transfer", TransactionType.TRANSFER)
    )

    OssWalletTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            EntryTypeSelector(
                selectedType = selectedType,
                onTypeSelected = { selectedType = it },
                types = types,
                selectedColor = if (selectedType == TransactionType.INCOME) AppColors.primary else AppColors.error
            )
        }
    }
}
