package com.osscabinet.ossWallet.ui.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.calculator.ExpressionEvaluator
import com.osscabinet.ossWallet.ui.calculator.*
import java.math.BigDecimal

/**
 * Enhanced Financial Expression Engine UI
 */
@Composable
fun CalculatorNumpad(
    amount: String,
    onAmountChange: (String, String) -> Unit,
    transactionType: String = "income",
    decimalPrecision: Int = 2,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    var state by remember { mutableStateOf(CalculatorState(transactionType = transactionType)) }
    var showHistory by remember { mutableStateOf(false) }
    
    val evaluator = remember(decimalPrecision) { ExpressionEvaluator(decimalPrecision) }
    
    // Initialize state with the correct transaction type and initial amount
    LaunchedEffect(transactionType, amount) {
        if (state.tokens.isEmpty() || (state.tokens.size == 1 && state.tokens.first() is Token.UnaryMinus)) {
            val initialTokens = mutableListOf<Token>()
            if (transactionType == "expense") initialTokens.add(Token.UnaryMinus)
            
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Display area with History Toggle
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
                modifier = Modifier.align(Alignment.TopStart).offset(y = (-8).dp)
            ) {
                Icon(
                    Icons.Rounded.History,
                    contentDescription = "Show History",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        // Keys - Google Calculator layout with reverted aesthetics
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
    transactionType: String,
    isError: Boolean,
    onSave: () -> Unit
) {
    val displayResult = if (transactionType == "expense" && result != "0" && !result.startsWith("-")) {
        "−$result"
    } else {
        result.replace("-", "−")
    }
    
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
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
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Result line
            Text(
                text = displayResult,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 36.sp
            )
            
            // Ghost Result
            if (ghostResult.isNotEmpty() && ghostResult != result) {
                Text(
                    text = if (transactionType == "expense") "−$ghostResult" else ghostResult,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        IconButton(
            onClick = onSave,
            modifier = Modifier.padding(start = 12.dp)
        ) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = "Confirm amount",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
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
    // Google Calculator Layout Refined:
    // Row 1: AC/C, ( ), %, ÷
    // Row 2: 7, 8, 9, ×
    // Row 3: 4, 5, 6, −
    // Row 4: 1, 2, 3, +
    // Row 5: 0, ., ⌫, =
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
        isAccent -> MaterialTheme.colorScheme.primaryContainer
        isOperator -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        isSpecial -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    }
    
    val textColor = when {
        isAccent -> MaterialTheme.colorScheme.onPrimaryContainer
        isOperator -> MaterialTheme.colorScheme.onPrimaryContainer
        isSpecial -> if (label == "C" || label == "⌫") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    Surface(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(16.dp), // Reverted to previous aesthetic
        color = backgroundColor,
        tonalElevation = 1.dp
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
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = if (isOperator || isAccent) FontWeight.Bold else FontWeight.Medium,
                    color = textColor,
                    fontSize = if (label.length > 1) 18.sp else 24.sp
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
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Calculation History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (history.isNotEmpty()) {
                    TextButton(onClick = onClearHistory) {
                        Text("Clear All", color = MaterialTheme.colorScheme.error)
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
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(history) { item ->
                        Surface(
                            onClick = { onItemSelect(item) },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    item.expression,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "= ${item.result}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
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
    MaterialTheme {
        CalculatorNumpad(
            amount = "100",
            onAmountChange = { _, _ -> },
            transactionType = "expense",
            onDone = {}
        )
    }
}
