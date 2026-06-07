package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.calculator.ExpressionEvaluator
import com.osscabinet.ossWallet.ui.calculator.CalculationHistoryItem
import com.osscabinet.ossWallet.ui.calculator.CalculatorAction
import com.osscabinet.ossWallet.ui.calculator.CalculatorState
import com.osscabinet.ossWallet.ui.calculator.Token
import com.osscabinet.ossWallet.ui.calculator.calculatorReducer
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.ColorOnDark
import java.math.BigDecimal

/**
 * Stateful wrapper for the Financial Expression Engine.
 * Manages its own [CalculatorState] and [ExpressionEvaluator].
 */
@Composable
fun CalculatorNumpad(
    amount: String,
    onAmountChange: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    allowSigned: Boolean = false,
    header: @Composable ((state: CalculatorState, onSave: () -> Unit, onHistory: () -> Unit) -> Unit)? = null
) {
    var state by remember { mutableStateOf(CalculatorState()) }
    var showHistory by remember { mutableStateOf(false) }
    val evaluator = remember { ExpressionEvaluator() }

    LaunchedEffect(amount) {
        if (state.tokens.isEmpty()) {
            val amountNum = amount.toBigDecimalOrNull()
            val finalAmount = if (allowSigned) amountNum else amountNum?.abs()

            if (finalAmount != null && finalAmount != BigDecimal.ZERO) {
                val isNegative = finalAmount.signum() < 0
                val absValue = finalAmount.abs().stripTrailingZeros().toPlainString()
                val initialTokens = if (isNegative) {
                    listOf(Token.UnaryMinus, Token.Number(absValue))
                } else {
                    listOf(Token.Number(absValue))
                }
                state = state.copy(
                    tokens = initialTokens,
                    display = finalAmount.stripTrailingZeros().toPlainString()
                )
            }
        }
    }

    val onAction: (CalculatorAction) -> Unit = { action ->
        state = calculatorReducer(state, action, evaluator)
    }

    val onSaveInternal = {
        val finalState = if (!state.isCalculated) {
            calculatorReducer(state, CalculatorAction.Equals, evaluator)
        } else state

        state = finalState
        if (!finalState.isError) {
            onAmountChange(finalState.display)
            onDone()
        }
    }

    CalculatorScaffold(
        modifier = modifier.padding(horizontal = 20.dp),
        display = {
            if (header != null) {
                header(state, onSaveInternal, { showHistory = true })
            } else {
                CalculatorDefaults.StandardDisplay(
                    state = state,
                    onSave = onSaveInternal,
                    onHistory = { showHistory = true }
                )
            }
        },
        keys = {
            CalculatorDefaults.StandardKeys(onAction = onAction)
        },
        historyOverlay = {
            if (showHistory) {
                CalculatorHistorySheet(
                    history = state.history,
                    onItemSelect = {
                        onAction(CalculatorAction.LoadFromHistory(it))
                        showHistory = false
                    },
                    onClearHistory = { onAction(CalculatorAction.ClearHistory) },
                    onDismiss = { showHistory = false }
                )
            }
        }
    )
}

/**
 * High-level layout component for the Calculator.
 * Standardizes the spatial arrangement of slots.
 */
@Composable
fun CalculatorScaffold(
    display: @Composable () -> Unit,
    keys: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    topSlot: (@Composable () -> Unit)? = null,
    historyOverlay: @Composable () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Financial Calculator" },
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        topSlot?.invoke()
        display()
        keys()
    }
    historyOverlay()
}

/**
 * Standardized building blocks for the Calculator.
 */
object CalculatorDefaults {

    @Composable
    fun StandardDisplay(
        state: CalculatorState,
        onSave: () -> Unit,
        onHistory: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = AppColors.recessedSurface,
            border = BorderStroke(0.5.dp, AppColors.divider.copy(alpha = 0.1f)),
            modifier = modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp)) {
                CalculatorDisplay(
                    expression = state.displayExpression,
                    result = state.display,
                    ghostResult = state.ghostResult,
                    isError = state.isError,
                    onSave = onSave
                )

                IconButton(
                    onClick = onHistory,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(28.dp)
                ) {
                    Icon(
                        Icons.Rounded.History,
                        contentDescription = "Show History",
                        tint = AppColors.contentSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    @Composable
    fun StandardKeys(
        onAction: (CalculatorAction) -> Unit,
        modifier: Modifier = Modifier
    ) {
        CalculatorKeys(
            onNumberPress = { onAction(CalculatorAction.AppendNumber(it)) },
            onOperatorPress = { onAction(CalculatorAction.AppendOperator(it)) },
            onParenthesisToggle = { onAction(CalculatorAction.AppendParenthesis(' ')) },
            onClearPress = { onAction(CalculatorAction.Clear) },
            onBackspacePress = { onAction(CalculatorAction.Backspace) },
            onEqualsPress = { onAction(CalculatorAction.Equals) },
            onPercentPress = { onAction(CalculatorAction.Percent) },
            modifier = modifier
        )
    }
}

@Composable
fun CalculatorDisplay(
    expression: String,
    result: String,
    ghostResult: String,
    isError: Boolean,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayResult = result.replace("-", "−")

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End
        ) {
            if (expression.isNotEmpty()) {
                Text(
                    text = expression,
                    style = AppTextStyles.listItemTitle.copy(
                        letterSpacing = 0.5.sp,
                        fontSize = 14.sp
                    ),
                    color = AppColors.contentSecondary.copy(alpha = 0.5f),
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Text(
                text = displayResult,
                style = AppTextStyles.heroAmount.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1.5).sp,
                    fontSize = 44.sp
                ),
                color = if (isError) AppColors.error else AppColors.contentPrimary,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )

            if (ghostResult.isNotEmpty() && ghostResult != result) {
                Text(
                    text = ghostResult.replace("-", "−"),
                    style = AppTextStyles.chartTooltipValue,
                    color = AppColors.primary.copy(alpha = 0.4f),
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.width(20.dp))

        Surface(
            onClick = onSave,
            shape = CircleShape,
            color = AppColors.primary,
            modifier = Modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.Done,
                    contentDescription = "Confirm amount",
                    tint = ColorOnDark,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun CalculatorKeys(
    onNumberPress: (String) -> Unit,
    onOperatorPress: (Char) -> Unit,
    onParenthesisToggle: () -> Unit,
    onClearPress: () -> Unit,
    onBackspacePress: () -> Unit,
    onEqualsPress: () -> Unit,
    onPercentPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CalculatorKey("C", onClearPress, Modifier.weight(1f), isSpecial = true)
            CalculatorKey("( )", onParenthesisToggle, Modifier.weight(1f), isSpecial = true)
            CalculatorKey("%", onPercentPress, Modifier.weight(1f), isSpecial = true)
            CalculatorKey("÷", { onOperatorPress('/') }, Modifier.weight(1f), isOperator = true)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CalculatorKey("7", { onNumberPress("7") }, Modifier.weight(1f))
            CalculatorKey("8", { onNumberPress("8") }, Modifier.weight(1f))
            CalculatorKey("9", { onNumberPress("9") }, Modifier.weight(1f))
            CalculatorKey("×", { onOperatorPress('*') }, Modifier.weight(1f), isOperator = true)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CalculatorKey("4", { onNumberPress("4") }, Modifier.weight(1f))
            CalculatorKey("5", { onNumberPress("5") }, Modifier.weight(1f))
            CalculatorKey("6", { onNumberPress("6") }, Modifier.weight(1f))
            CalculatorKey("−", { onOperatorPress('-') }, Modifier.weight(1f), isOperator = true)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CalculatorKey("1", { onNumberPress("1") }, Modifier.weight(1f))
            CalculatorKey("2", { onNumberPress("2") }, Modifier.weight(1f))
            CalculatorKey("3", { onNumberPress("3") }, Modifier.weight(1f))
            CalculatorKey("+", { onOperatorPress('+') }, Modifier.weight(1f), isOperator = true)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CalculatorKey("0", { onNumberPress("0") }, Modifier.weight(1f))
            CalculatorKey(".", { onNumberPress(".") }, Modifier.weight(1f))
            CalculatorKey("⌫", onBackspacePress, Modifier.weight(1f), isSpecial = true)
            CalculatorKey("=", onEqualsPress, Modifier.weight(1f), isAccent = true)
        }
    }
}

@Composable
fun CalculatorKey(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isOperator: Boolean = false,
    isSpecial: Boolean = false,
    isAccent: Boolean = false
) {
    val backgroundColor = when {
        isAccent -> AppColors.primary
        isOperator -> AppColors.primary.copy(alpha = 0.08f)
        isSpecial -> AppColors.error.copy(alpha = 0.08f)
        else -> AppColors.recessedSurface
    }

    val textColor = when {
        isAccent -> ColorOnDark
        isOperator -> AppColors.primary
        isSpecial -> if (label == "C" || label == "⌫") AppColors.error else AppColors.contentSecondary
        else -> AppColors.contentPrimary
    }

    Surface(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(22.dp),
        color = backgroundColor,
        border = if (!isAccent) BorderStroke(0.5.dp, AppColors.divider.copy(alpha = 0.1f)) else null,
        shadowElevation = 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (label == "⌫") {
                Icon(
                    Icons.AutoMirrored.Rounded.Backspace,
                    contentDescription = "Backspace",
                    modifier = Modifier.size(22.dp),
                    tint = textColor
                )
            } else {
                Text(
                    text = label,
                    style = AppTextStyles.screenTitle.copy(
                        fontWeight = if (isOperator || isAccent) FontWeight.Bold else FontWeight.SemiBold,
                        letterSpacing = if (label.length > 1) 0.sp else 0.5.sp,
                        fontSize = if (label.length > 1) 16.sp else 24.sp
                    ),
                    color = textColor
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalculatorHistorySheet(
    history: List<CalculationHistoryItem>,
    onItemSelect: (CalculationHistoryItem) -> Unit,
    onClearHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppColors.cardSurface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = AppColors.divider.copy(alpha = 0.4f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("History", style = AppTextStyles.heroAmount)
                if (history.isNotEmpty()) {
                    TextButton(onClick = onClearHistory) {
                        Text(
                            "Clear All",
                            style = AppTextStyles.formSwitchLabel.copy(
                                fontWeight = FontWeight.Bold,
                                color = AppColors.error
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (history.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No calculations yet",
                        style = AppTextStyles.listItemTitle,
                        color = AppColors.contentSecondary.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.heightIn(max = 400.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(history) { item ->
                        Surface(
                            onClick = { onItemSelect(item) },
                            shape = RoundedCornerShape(20.dp),
                            color = AppColors.recessedSurface,
                            border = BorderStroke(0.5.dp, AppColors.divider.copy(alpha = 0.15f))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    item.expression,
                                    style = AppTextStyles.transactionTitle,
                                    color = AppColors.contentSecondary.copy(alpha = 0.6f)
                                )
                                Text(
                                    "= ${item.result}",
                                    style = AppTextStyles.screenTitle,
                                    color = AppColors.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
