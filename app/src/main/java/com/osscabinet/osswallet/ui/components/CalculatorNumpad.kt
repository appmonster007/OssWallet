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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material.icons.rounded.Remove
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.scaledDp
import com.osscabinet.ossWallet.ui.theme.scaledSp
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
    val uiScale = AeonTheme.uiScale
    val keyHeight = scaledDp(68f, uiScale, min = 44f, max = 76f)
    val displayScale = uiScale.coerceIn(0.75f, 1.12f)

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
        modifier = modifier.padding(horizontal = 24.dp),
        display = {
            if (header != null) {
                header(state, onSaveInternal, { showHistory = true })
            } else {
                CalculatorDefaults.StandardDisplay(
                    state = state,
                    onSave = onSaveInternal,
                    onHistory = { showHistory = true },
                    displayScale = displayScale
                )
            }
        },
        keys = {
            CalculatorDefaults.StandardKeys(onAction = onAction, keyHeight = keyHeight)
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
        verticalArrangement = Arrangement.spacedBy(24.dp)
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
        modifier: Modifier = Modifier,
        displayScale: Float = 1f
    ) {
        val cornerRadius = scaledDp(28f, displayScale, min = 20f, max = 28f)
        val hPad = scaledDp(16f, displayScale, min = 12f, max = 16f)
        val vPad = scaledDp(20f, displayScale, min = 14f, max = 20f)
        val historyContainerSize = scaledDp(28f, displayScale, min = 20f, max = 28f)
        val historyIconSize = scaledDp(18f, displayScale, min = 13f, max = 18f)

        Surface(
            shape = RoundedCornerShape(cornerRadius),
            color = AppColors.recessedSurface,
            border = BorderStroke(0.5.dp, AppColors.divider.copy(alpha = 0.1f)),
            modifier = modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = hPad, vertical = vPad)) {
                CalculatorDisplay(
                    expression = state.displayExpression,
                    result = state.display,
                    ghostResult = state.ghostResult,
                    isError = state.isError,
                    onSave = onSave,
                    displayScale = displayScale
                )

                IconButton(
                    onClick = onHistory,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(historyContainerSize)
                ) {
                    Icon(
                        Icons.Rounded.History,
                        contentDescription = "Show History",
                        tint = AppColors.contentSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(historyIconSize)
                    )
                }
            }
        }
    }

    @Composable
    fun StandardKeys(
        onAction: (CalculatorAction) -> Unit,
        modifier: Modifier = Modifier,
        keyHeight: Dp = 68.dp
    ) {
        CalculatorKeys(
            onNumberPress = { onAction(CalculatorAction.AppendNumber(it)) },
            onOperatorPress = { onAction(CalculatorAction.AppendOperator(it)) },
            onParenthesisToggle = { onAction(CalculatorAction.AppendParenthesis(' ')) },
            onClearPress = { onAction(CalculatorAction.Clear) },
            onBackspacePress = { onAction(CalculatorAction.Backspace) },
            onEqualsPress = { onAction(CalculatorAction.Equals) },
            onPercentPress = { onAction(CalculatorAction.Percent) },
            modifier = modifier,
            keyHeight = keyHeight
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
    modifier: Modifier = Modifier,
    displayScale: Float = 1f
) {
    val displayResult = result.replace("-", "−")
    val resultFontSize = scaledSp(44f, displayScale, min = 33f, max = 49f)
    val expressionFontSize = scaledSp(14f, displayScale, min = 11f, max = 16f)
    val confirmButtonSize = scaledDp(52f, displayScale, min = 40f, max = 58f)
    val confirmIconSize = scaledDp(24f, displayScale, min = 18f, max = 27f)
    val spacerWidth = scaledDp(20f, displayScale, min = 12f, max = 22f)

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
                        fontSize = expressionFontSize
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
                    fontSize = resultFontSize
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

        Spacer(modifier = Modifier.width(spacerWidth))

        Surface(
            onClick = onSave,
            shape = CircleShape,
            color = AppColors.primary,
            modifier = Modifier.size(confirmButtonSize)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.Done,
                    contentDescription = "Confirm amount",
                    tint = ColorOnDark,
                    modifier = Modifier.size(confirmIconSize)
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
    modifier: Modifier = Modifier,
    keyHeight: Dp = 68.dp
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CalculatorKey("C", onClearPress, Modifier.weight(1f), isSpecial = true, keyHeight = keyHeight)
            CalculatorKey("( )", onParenthesisToggle, Modifier.weight(1f), isSpecial = true, keyHeight = keyHeight)
            CalculatorKey("%", onPercentPress, Modifier.weight(1f), isSpecial = true, keyHeight = keyHeight)
            CalculatorKey("÷", { onOperatorPress('/') }, Modifier.weight(1f), isOperator = true, keyHeight = keyHeight)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CalculatorKey("7", { onNumberPress("7") }, Modifier.weight(1f), keyHeight = keyHeight)
            CalculatorKey("8", { onNumberPress("8") }, Modifier.weight(1f), keyHeight = keyHeight)
            CalculatorKey("9", { onNumberPress("9") }, Modifier.weight(1f), keyHeight = keyHeight)
            CalculatorKey("×", { onOperatorPress('*') }, Modifier.weight(1f), isOperator = true, keyHeight = keyHeight)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CalculatorKey("4", { onNumberPress("4") }, Modifier.weight(1f), keyHeight = keyHeight)
            CalculatorKey("5", { onNumberPress("5") }, Modifier.weight(1f), keyHeight = keyHeight)
            CalculatorKey("6", { onNumberPress("6") }, Modifier.weight(1f), keyHeight = keyHeight)
            CalculatorKey("−", { onOperatorPress('-') }, Modifier.weight(1f), isOperator = true, keyHeight = keyHeight)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CalculatorKey("1", { onNumberPress("1") }, Modifier.weight(1f), keyHeight = keyHeight)
            CalculatorKey("2", { onNumberPress("2") }, Modifier.weight(1f), keyHeight = keyHeight)
            CalculatorKey("3", { onNumberPress("3") }, Modifier.weight(1f), keyHeight = keyHeight)
            CalculatorKey("+", { onOperatorPress('+') }, Modifier.weight(1f), isOperator = true, keyHeight = keyHeight)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CalculatorKey("0", { onNumberPress("0") }, Modifier.weight(1f), keyHeight = keyHeight)
            CalculatorKey(".", { onNumberPress(".") }, Modifier.weight(1f), keyHeight = keyHeight)
            CalculatorKey("⌫", onBackspacePress, Modifier.weight(1f), isSpecial = true, keyHeight = keyHeight)
            CalculatorKey("=", onEqualsPress, Modifier.weight(1f), isAccent = true, keyHeight = keyHeight)
        }
    }
}

private val DivideIcon: ImageVector by lazy {
    ImageVector.Builder("Divide", 24.dp, 24.dp, 24f, 24f).apply {
        // top dot
        path(fill = SolidColor(Color.Black)) {
            moveTo(13.75f, 5.5f)
            arcTo(1.75f, 1.75f, 0f, false, true, 12f, 7.25f)
            arcTo(1.75f, 1.75f, 0f, false, true, 10.25f, 5.5f)
            arcTo(1.75f, 1.75f, 0f, false, true, 12f, 3.75f)
            arcTo(1.75f, 1.75f, 0f, false, true, 13.75f, 5.5f)
            close()
        }
        // horizontal bar
        path(fill = SolidColor(Color.Black)) {
            moveTo(4f, 11f)
            lineTo(20f, 11f)
            lineTo(20f, 13f)
            lineTo(4f, 13f)
            close()
        }
        // bottom dot
        path(fill = SolidColor(Color.Black)) {
            moveTo(13.75f, 18.5f)
            arcTo(1.75f, 1.75f, 0f, false, true, 12f, 20.25f)
            arcTo(1.75f, 1.75f, 0f, false, true, 10.25f, 18.5f)
            arcTo(1.75f, 1.75f, 0f, false, true, 12f, 16.75f)
            arcTo(1.75f, 1.75f, 0f, false, true, 13.75f, 18.5f)
            close()
        }
    }.build()
}

private val EqualsIcon: ImageVector by lazy {
    ImageVector.Builder("Equals", 24.dp, 24.dp, 24f, 24f).apply {
        // top bar
        path(fill = SolidColor(Color.Black)) {
            moveTo(4f, 8.5f)
            lineTo(20f, 8.5f)
            lineTo(20f, 11f)
            lineTo(4f, 11f)
            close()
        }
        // bottom bar
        path(fill = SolidColor(Color.Black)) {
            moveTo(4f, 13f)
            lineTo(20f, 13f)
            lineTo(20f, 15.5f)
            lineTo(4f, 15.5f)
            close()
        }
    }.build()
}

@Composable
fun CalculatorKey(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isOperator: Boolean = false,
    isSpecial: Boolean = false,
    isAccent: Boolean = false,
    keyHeight: Dp = 68.dp
) {
    val scaleFactor = (keyHeight.value / 68f).coerceIn(0f, 1.5f)
    val cornerRadius = scaledDp(24f, scaleFactor, min = 12f, max = 27f)
    val iconSize = scaledDp(22f, scaleFactor, min = 16f, max = 25f)
    val labelFontSize = if (label.length > 1)
        scaledSp(16f, scaleFactor, min = 11f, max = 18f)
    else
        scaledSp(24f, scaleFactor, min = 14f, max = 27f)

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
        modifier = modifier.height(keyHeight),
        shape = RoundedCornerShape(cornerRadius),
        color = backgroundColor,
        border = if (!isAccent) BorderStroke(0.5.dp, AppColors.divider.copy(alpha = 0.1f)) else null,
        shadowElevation = 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            val iconVector: ImageVector? = when (label) {
                "⌫" -> Icons.AutoMirrored.Rounded.Backspace
                "÷" -> DivideIcon
                "×" -> Icons.Rounded.Close
                "−" -> Icons.Rounded.Remove
                "+" -> Icons.Rounded.Add
                "=" -> EqualsIcon
                "%" -> Icons.Rounded.Percent
                else -> null
            }
            if (iconVector != null) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = label,
                    modifier = Modifier.size(iconSize),
                    tint = textColor
                )
            } else {
                Text(
                    text = label,
                    style = AppTextStyles.screenTitle.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = if (label.length > 1) 0.sp else 0.5.sp,
                        fontSize = labelFontSize
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
