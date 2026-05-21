package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.calculator.ExpressionEvaluator
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.ui.calculator.*
import java.math.BigDecimal
import com.osscabinet.ossWallet.ui.theme.*
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

/**
 * Enhanced Financial Expression Engine UI.
 * Redesigned to follow the "Architect Slate" design specification.
 */
@Composable
fun CalculatorNumpad(
    amount: String,
    onAmountChange: (String, TransactionType) -> Unit,
    transactionType: TransactionType = TransactionType.INCOME,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    var state by remember { mutableStateOf(CalculatorState(transactionType = transactionType)) }
    var showHistory by remember { mutableStateOf(false) }
    
    val evaluator = remember { ExpressionEvaluator() }
    
    LaunchedEffect(transactionType, amount) {
        if (state.tokens.isEmpty() || (state.tokens.size == 1 && state.tokens.first() is Token.UnaryMinus)) {
            val initialTokens = mutableListOf<Token>()
            if (transactionType == TransactionType.EXPENSE) initialTokens.add(Token.UnaryMinus)
            
            val amountNum = amount.toBigDecimalOrNull()?.abs()
            if (amountNum != null && amountNum != BigDecimal.ZERO) {
                initialTokens.add(Token.Number(amountNum.stripTrailingZeros().toPlainString()))
            }
            
            state = state.copy(
                transactionType = transactionType,
                tokens = initialTokens,
                display = if (amountNum != null && amountNum != BigDecimal.ZERO) amountNum.stripTrailingZeros().toPlainString() else state.display
            )
        }
    }
    
    fun handleAction(action: CalculatorAction) {
        state = calculatorReducer(state, action, evaluator)
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Financial Calculator" },
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Display area
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = AppColors.recessedSurface,
            border = BorderStroke(0.5.dp, AppColors.divider.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                CalculatorDisplay(
                    expression = state.displayExpression,
                    result = state.display,
                    ghostResult = state.ghostResult,
                    transactionType = state.transactionType,
                    isError = state.isError,
                    onSave = {
                        val finalState = if (!state.isCalculated) {
                            calculatorReducer(state, CalculatorAction.Equals, evaluator)
                        } else state
                        
                        state = finalState
                        if (!finalState.isError) {
                            onAmountChange(finalState.display, finalState.transactionType)
                            onDone()
                        }
                    }
                )
                
                IconButton(
                    onClick = { showHistory = true },
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                ) {
                    Icon(
                        Icons.Rounded.History,
                        contentDescription = "Show History",
                        tint = AppColors.primary.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        
        // Keys
        CalculatorKeys(
            onNumberPress = { handleAction(CalculatorAction.AppendNumber(it)) },
            onOperatorPress = { handleAction(CalculatorAction.AppendOperator(it)) },
            onParenthesisToggle = { handleAction(CalculatorAction.AppendParenthesis(' ')) },
            onClearPress = { handleAction(CalculatorAction.Clear) },
            onBackspacePress = { handleAction(CalculatorAction.Backspace) },
            onEqualsPress = { 
                handleAction(CalculatorAction.Equals)
                if (!state.isError) {
                    onAmountChange(state.display, state.transactionType)
                }
            },
            onPercentPress = { handleAction(CalculatorAction.Percent) }
        )
    }

    if (showHistory) {
        CalculatorHistorySheet(
            history = state.history,
            onItemSelect = {
                handleAction(CalculatorAction.LoadFromHistory(it))
                showHistory = false
            },
            onClearHistory = { handleAction(CalculatorAction.ClearHistory) },
            onDismiss = { showHistory = false }
        )
    }
}

@Composable
private fun CalculatorDisplay(
    expression: String,
    result: String,
    ghostResult: String,
    transactionType: TransactionType,
    isError: Boolean,
    onSave: () -> Unit
) {
    val displayResult = if (transactionType == TransactionType.EXPENSE && result != "0" && !result.startsWith("-")) {
        "−$result"
    } else {
        result.replace("-", "−")
    }
    
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End
        ) {
            // Expression line
            if (expression.isNotEmpty()) {
                Text(
                    text = expression,
                    style = AppTextStyles.listItemTitle.copy(
                        letterSpacing = 0.5.sp
                    ),
                    color = AppColors.contentSecondary.copy(alpha = 0.6f),
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Result line
            Text(
                text = displayResult,
                style = AppTextStyles.heroAmount.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp,
                    fontSize = 42.sp
                ),
                color = if (isError) AppColors.error else AppColors.contentPrimary,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Ghost Result
            if (ghostResult.isNotEmpty() && ghostResult != result) {
                Text(
                    text = if (transactionType == TransactionType.EXPENSE) "−$ghostResult" else ghostResult,
                    style = AppTextStyles.chartTooltipValue,
                    color = AppColors.primary.copy(alpha = 0.4f),
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Surface(
            onClick = onSave,
            shape = CircleShape,
            color = AppColors.primary,
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.Done,
                    contentDescription = "Confirm amount",
                    tint = ColorOnDark,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun CalculatorKeys(
    onNumberPress: (String) -> Unit,
    onOperatorPress: (Char) -> Unit,
    onParenthesisToggle: () -> Unit,
    onClearPress: () -> Unit,
    onBackspacePress: () -> Unit,
    onEqualsPress: () -> Unit,
    onPercentPress: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CalculatorKey("C", onClearPress, Modifier.weight(1f), isSpecial = true)
            CalculatorKey("( )", onParenthesisToggle, Modifier.weight(1f), isSpecial = true)
            CalculatorKey("%", onPercentPress, Modifier.weight(1f), isSpecial = true)
            CalculatorKey("÷", { onOperatorPress('/') }, Modifier.weight(1f), isOperator = true)
        }
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CalculatorKey("7", { onNumberPress("7") }, Modifier.weight(1f))
            CalculatorKey("8", { onNumberPress("8") }, Modifier.weight(1f))
            CalculatorKey("9", { onNumberPress("9") }, Modifier.weight(1f))
            CalculatorKey("×", { onOperatorPress('*') }, Modifier.weight(1f), isOperator = true)
        }
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CalculatorKey("4", { onNumberPress("4") }, Modifier.weight(1f))
            CalculatorKey("5", { onNumberPress("5") }, Modifier.weight(1f))
            CalculatorKey("6", { onNumberPress("6") }, Modifier.weight(1f))
            CalculatorKey("−", { onOperatorPress('-') }, Modifier.weight(1f), isOperator = true)
        }
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CalculatorKey("1", { onNumberPress("1") }, Modifier.weight(1f))
            CalculatorKey("2", { onNumberPress("2") }, Modifier.weight(1f))
            CalculatorKey("3", { onNumberPress("3") }, Modifier.weight(1f))
            CalculatorKey("+", { onOperatorPress('+') }, Modifier.weight(1f), isOperator = true)
        }
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CalculatorKey("0", { onNumberPress("0") }, Modifier.weight(1f))
            CalculatorKey(".", { onNumberPress(".") }, Modifier.weight(1f))
            CalculatorKey("⌫", onBackspacePress, Modifier.weight(1f), isSpecial = true)
            CalculatorKey("=", onEqualsPress, Modifier.weight(1f), isAccent = true)
        }
    }
}

@Composable
private fun CalculatorKey(
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
        modifier = modifier.height(68.dp),
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        border = if (!isAccent) BorderStroke(0.5.dp, AppColors.divider.copy(alpha = 0.15f)) else null,
        shadowElevation = 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (label == "⌫") {
                Icon(
                    Icons.AutoMirrored.Rounded.Backspace,
                    contentDescription = "Backspace",
                    modifier = Modifier.size(24.dp),
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
                Text(
                    "History",
                    style = AppTextStyles.heroAmount
                )
                if (history.isNotEmpty()) {
                    TextButton(onClick = onClearHistory) {
                        Text("Clear All", style = AppTextStyles.formSwitchLabel.copy(fontWeight = FontWeight.Bold, color = AppColors.error))
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

@Preview(showBackground = true)
@Composable
fun CalculatorNumpadPreview() {
    OssWalletTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            CalculatorNumpad(
                amount = "125.50",
                onAmountChange = { _, _ -> },
                onDone = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CalculatorDisplayPreview() {
    OssWalletTheme {
        Surface(modifier = Modifier.padding(16.dp), color = AppColors.recessedSurface, shape = RoundedCornerShape(24.dp)) {
            CalculatorDisplay(
                expression = "50 + 75.50",
                result = "125.50",
                ghostResult = "125.50",
                transactionType = TransactionType.EXPENSE,
                isError = false,
                onSave = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CalculatorHistorySheetPreview() {
    OssWalletTheme {
        CalculatorHistorySheet(
            history = listOf(
                CalculationHistoryItem("100 + 25", "125"),
                CalculationHistoryItem("200 * 1.05", "210")
            ),
            onItemSelect = {},
            onClearHistory = {},
            onDismiss = {}
        )
    }
}
