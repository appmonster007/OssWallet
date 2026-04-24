package com.osscabinet.ossWallet.ui.calculator

import com.osscabinet.ossWallet.domain.calculator.ExpressionEvaluator
import java.math.BigDecimal

/**
 * Calculator state management adapted from Calc-OS patterns
 * Uses Compose state for reactive UI updates
 */
data class CalculatorState(
    val display: String = "0",
    val expression: String = "",
    val memory: BigDecimal = BigDecimal.ZERO,
    val history: List<CalculationHistoryItem> = emptyList(),
    val isError: Boolean = false,
    val isCalculated: Boolean = false
) {
    companion object {
        const val MAX_HISTORY_SIZE = 100
    }
}

data class CalculationHistoryItem(
    val expression: String,
    val result: BigDecimal,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Calculator actions - MVI pattern for state updates
 */
sealed class CalculatorAction {
    data class AppendNumber(val number: String) : CalculatorAction()
    data class AppendOperator(val operator: Char) : CalculatorAction()
    data class AppendParenthesis(val parenthesis: Char) : CalculatorAction()
    object Clear : CalculatorAction()
    object Backspace : CalculatorAction()
    object Equals : CalculatorAction()
    data class MemoryAdd(val value: BigDecimal) : CalculatorAction()
    data class MemorySubtract(val value: BigDecimal) : CalculatorAction()
    object MemoryRecall : CalculatorAction()
    object MemoryClear : CalculatorAction()
}

/**
 * Calculator state reducer
 */
fun calculatorReducer(state: CalculatorState, action: CalculatorAction): CalculatorState {
    return when (action) {
        is CalculatorAction.AppendNumber -> {
            val newExpression = if (state.isCalculated) {
                action.number
            } else {
                state.expression + action.number
            }
            state.copy(
                expression = newExpression,
                display = evaluateExpression(newExpression),
                isCalculated = false,
                isError = false
            )
        }
        
        is CalculatorAction.AppendOperator -> {
            val newExpression = state.expression + action.operator
            state.copy(
                expression = newExpression,
                display = evaluateExpression(newExpression),
                isCalculated = false,
                isError = false
            )
        }
        
        is CalculatorAction.AppendParenthesis -> {
            val newExpression = state.expression + action.parenthesis
            state.copy(
                expression = newExpression,
                display = evaluateExpression(newExpression),
                isCalculated = false,
                isError = false
            )
        }
        
        CalculatorAction.Clear -> {
            state.copy(
                display = "0",
                expression = "",
                isError = false,
                isCalculated = false
            )
        }
        
        CalculatorAction.Backspace -> {
            val newExpression = if (state.expression.isNotEmpty()) {
                state.expression.dropLast(1)
            } else {
                ""
            }
            state.copy(
                expression = newExpression,
                display = if (newExpression.isNotEmpty()) evaluateExpression(newExpression) else "0",
                isCalculated = false
            )
        }
        
        CalculatorAction.Equals -> {
            val result = evaluateExpressionToBigDecimal(state.expression)
            val historyItem = CalculationHistoryItem(
                expression = state.expression,
                result = result
            )
            val newHistory = listOf(historyItem) + state.history
                .take(CalculatorState.MAX_HISTORY_SIZE)
            
            state.copy(
                display = result.stripTrailingZeros().toPlainString(),
                expression = "",
                history = newHistory,
                isCalculated = true,
                isError = false
            )
        }
        
        is CalculatorAction.MemoryAdd -> {
            val currentMemory = state.memory
            val valueToAdd = state.display.toBigDecimalOrNull() ?: BigDecimal.ZERO
            state.copy(memory = currentMemory.add(valueToAdd))
        }
        
        is CalculatorAction.MemorySubtract -> {
            val currentMemory = state.memory
            val valueToSubtract = state.display.toBigDecimalOrNull() ?: BigDecimal.ZERO
            state.copy(memory = currentMemory.subtract(valueToSubtract))
        }
        
        CalculatorAction.MemoryRecall -> {
            state.copy(display = state.memory.stripTrailingZeros().toPlainString())
        }
        
        CalculatorAction.MemoryClear -> {
            state.copy(memory = BigDecimal.ZERO)
        }
    }
}

private fun evaluateExpression(expr: String): String {
    if (expr.isEmpty()) return "0"
    return try {
        val evaluator = ExpressionEvaluator()
        evaluator.evaluate(expr).stripTrailingZeros().toPlainString()
    } catch (e: Exception) {
        "Error"
    }
}

private fun evaluateExpressionToBigDecimal(expr: String): BigDecimal {
    if (expr.isEmpty()) return BigDecimal.ZERO
    return try {
        val evaluator = ExpressionEvaluator()
        evaluator.evaluate(expr)
    } catch (e: Exception) {
        BigDecimal.ZERO
    }
}
