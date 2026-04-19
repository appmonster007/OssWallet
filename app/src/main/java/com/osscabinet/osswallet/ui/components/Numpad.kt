package com.osscabinet.ossWallet.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import java.math.BigDecimal

@Composable
fun Numpad(
    amount: String,
    onAmountChange: (String) -> Unit,
    decimalPrecision: Int,
    onDone: () -> Unit,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    var firstOperand by remember { mutableStateOf<BigDecimal?>(null) }
    var operator by remember { mutableStateOf<String?>(null) }
    var isEnteringSecondOperand by remember { mutableStateOf(false) }

    // Swipe-to-dismiss logic
    var offsetY by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val threshold = with(density) { 100.dp.toPx() }

    LaunchedEffect(isVisible) {
        if (!isVisible) offsetY = 0f
    }

    fun calculateResult() {
        if (firstOperand != null && operator != null && amount.isNotEmpty()) {
            val secondOperand = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
            val rawResult = try {
                when (operator) {
                    "+" -> firstOperand!!.add(secondOperand)
                    "−" -> firstOperand!!.subtract(secondOperand)
                    "×" -> firstOperand!!.multiply(secondOperand)
                    "÷" -> if (secondOperand.signum() != 0) firstOperand!!.divide(
                        secondOperand,
                        decimalPrecision,
                        java.math.RoundingMode.HALF_UP
                    ) else BigDecimal.ZERO
                    else -> secondOperand
                }
            } catch (e: Exception) {
                BigDecimal.ZERO
            }

            val result = rawResult.abs().stripTrailingZeros()
            onAmountChange(result.toPlainString())
            firstOperand = null
            operator = null
            isEnteringSecondOperand = false
        }
    }

    fun handleNumberInput(input: String) {
        if (isEnteringSecondOperand) {
            onAmountChange(if (input == ".") "0." else input)
            isEnteringSecondOperand = false
        } else {
            if (input == ".") {
                if (!amount.contains(".")) {
                    onAmountChange(if (amount.isEmpty()) "0." else amount + ".")
                }
            } else if (input == "00") {
                if (amount.isNotEmpty() && amount != "0") {
                    if (amount.contains(".")) {
                        if (amount.substringAfter(".").length < decimalPrecision) {
                            onAmountChange(amount + "00")
                        }
                    } else {
                        onAmountChange(amount + "00")
                    }
                }
            } else {
                onAmountChange(if (amount == "0") input else amount + input)
            }
        }
    }

    fun handleOperatorInput(op: String) {
        if (amount.isNotEmpty()) {
            if (operator != null) calculateResult()
            firstOperand = amount.toBigDecimalOrNull()
            operator = op
            isEnteringSecondOperand = true
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioLowBouncy)
        ),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = spring(stiffness = Spring.StiffnessLow)
        ),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .offset { IntOffset(0, offsetY.toInt().coerceAtLeast(0)) }
                .draggable(
                    state = rememberDraggableState { delta ->
                        offsetY += delta
                    },
                    orientation = Orientation.Vertical,
                    onDragStopped = {
                        if (offsetY > threshold) {
                            onDone()
                        }
                        offsetY = 0f
                    }
                ),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Drag Handle
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .size(width = 40.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                Column(
                    modifier = Modifier.padding(horizontal = 16.dp).height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val operators = listOf("+", "−", "×", "÷")

                    @Composable
                    fun NumpadKey(
                        key: String,
                        onClick: () -> Unit,
                        modifier: Modifier = Modifier,
                        color: Color? = null,
                        contentColor: Color? = null
                    ) {
                        val backgroundColor = color ?: when (key) {
                            in operators -> MaterialTheme.colorScheme.primaryContainer
                            "C", "DEL" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        }
                        val textColor = contentColor ?: if (key in operators) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }

                        Surface(
                            onClick = onClick,
                            modifier = modifier.fillMaxHeight(),
                            shape = RoundedCornerShape(20.dp),
                            color = backgroundColor
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (key == "DEL") {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.Backspace,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = textColor
                                    )
                                } else if (key == "DONE") {
                                    Icon(
                                        Icons.Rounded.Done,
                                        contentDescription = "Done",
                                        modifier = Modifier.size(28.dp),
                                        tint = textColor
                                    )
                                } else {
                                    Text(
                                        text = key,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = textColor
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Row 1: C, ÷, ×, DEL
                    Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumpadKey("C", {
                            onAmountChange("")
                            firstOperand = null
                            operator = null
                            isEnteringSecondOperand = false
                        }, Modifier.weight(1f))
                        NumpadKey("÷", { handleOperatorInput("÷") }, Modifier.weight(1f))
                        NumpadKey("×", { handleOperatorInput("×") }, Modifier.weight(1f))
                        NumpadKey("DEL", { if (amount.isNotEmpty()) onAmountChange(amount.dropLast(1)) }, Modifier.weight(1f))
                    }
                    // Row 2: 7, 8, 9, −
                    Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumpadKey("7", { handleNumberInput("7") }, Modifier.weight(1f))
                        NumpadKey("8", { handleNumberInput("8") }, Modifier.weight(1f))
                        NumpadKey("9", { handleNumberInput("9") }, Modifier.weight(1f))
                        NumpadKey("−", { handleOperatorInput("−") }, Modifier.weight(1f))
                    }
                    // Row 3: 4, 5, 6, +
                    Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumpadKey("4", { handleNumberInput("4") }, Modifier.weight(1f))
                        NumpadKey("5", { handleNumberInput("5") }, Modifier.weight(1f))
                        NumpadKey("6", { handleNumberInput("6") }, Modifier.weight(1f))
                        NumpadKey("+", { handleOperatorInput("+") }, Modifier.weight(1f))
                    }
                    // Rows 4 & 5
                    Row(modifier = Modifier.weight(2f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(modifier = Modifier.weight(3f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                NumpadKey("1", { handleNumberInput("1") }, Modifier.weight(1f))
                                NumpadKey("2", { handleNumberInput("2") }, Modifier.weight(1f))
                                NumpadKey("3", { handleNumberInput("3") }, Modifier.weight(1f))
                            }
                            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                NumpadKey(".", { handleNumberInput(".") }, Modifier.weight(1f))
                                NumpadKey("0", { handleNumberInput("0") }, Modifier.weight(1f))
                                NumpadKey("00", { handleNumberInput("00") }, Modifier.weight(1f))
                            }
                        }
                        // Equal button spanning two rows
                        NumpadKey(
                            key = if (operator != null) "=" else "DONE",
                            onClick = {
                                if (operator != null) {
                                    calculateResult()
                                } else {
                                    onDone()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}
