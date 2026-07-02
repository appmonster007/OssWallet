package com.osscabinet.ossWallet.ui.calculator

import com.osscabinet.ossWallet.domain.calculator.ExpressionEvaluator
import java.math.BigDecimal

/**
 * Token types for the financial expression engine
 */
sealed class Token {
    data class Number(val value: String) : Token()
    data class Operator(val symbol: Char) : Token()
    data class Parenthesis(val symbol: Char) : Token()
    object Percent : Token()
    object UnaryMinus : Token()

    fun toDisplayString(): String = when (this) {
        is Number -> value
        is Operator -> when (symbol) {
            '*' -> "×"
            '/' -> "÷"
            '-' -> "−"
            else -> symbol.toString()
        }

        is Parenthesis -> symbol.toString()
        is Percent -> "%"
        is UnaryMinus -> "−"
    }

    fun toInternalString(): String = when (this) {
        is Number -> value
        is Operator -> symbol.toString()
        is Parenthesis -> symbol.toString()
        is Percent -> "%"
        is UnaryMinus -> "-"
    }
}

/**
 * Calculator state management for token-based expression engine
 */
data class CalculatorState(
    val display: String = "0",
    val tokens: List<Token> = emptyList(),
    val ghostResult: String = "",
    val isError: Boolean = false,
    val isCalculated: Boolean = false,
    val history: List<CalculationHistoryItem> = emptyList()
) {
    val expression: String get() = tokens.joinToString("") { it.toInternalString() }
    val displayExpression: String get() = tokens.joinToString("") { it.toDisplayString() }
}

data class CalculationHistoryItem(
    val expression: String,
    val result: String,
    val timestamp: Long = System.currentTimeMillis()
)

sealed class CalculatorAction {
    data class AppendNumber(val number: String) : CalculatorAction()
    data class AppendOperator(val operator: Char) : CalculatorAction()
    data class AppendParenthesis(val parenthesis: Char) : CalculatorAction()
    object Clear : CalculatorAction()
    object Backspace : CalculatorAction()
    object Equals : CalculatorAction()
    object Percent : CalculatorAction()
    data class LoadFromHistory(val item: CalculationHistoryItem) : CalculatorAction()
    object ClearHistory : CalculatorAction()
}

/**
 * Deterministic reducer for the financial expression engine
 */
fun calculatorReducer(
    state: CalculatorState,
    action: CalculatorAction,
    evaluator: ExpressionEvaluator = ExpressionEvaluator()
): CalculatorState {
    return when (action) {
        is CalculatorAction.AppendNumber -> {
            val currentTokens = if (state.isCalculated) emptyList() else state.tokens
            val newTokens = when (val lastToken = currentTokens.lastOrNull()) {
                is Token.Number -> {
                    if (action.number == "." && lastToken.value.contains(".")) {
                        currentTokens
                    } else {
                        val combined = lastToken.value + action.number
                        // Use BigDecimal to auto-strip leading zeros (e.g. "05" -> "5")
                        // but preserve the trailing dot while typing.
                        val newValue = if (combined.endsWith(".")) combined
                        else combined.toBigDecimalOrNull()?.toPlainString() ?: combined
                        
                        currentTokens.dropLast(1) + Token.Number(newValue)
                    }
                }

                is Token.Percent, is Token.Parenthesis -> {
                    val initialValue = if (action.number == ".") "0." else action.number
                    if (lastToken is Token.Parenthesis && lastToken.symbol == '(') {
                        currentTokens + Token.Number(initialValue)
                    } else {
                        currentTokens + Token.Operator('*') + Token.Number(initialValue)
                    }
                }

                else -> {
                    val initialValue = if (action.number == ".") "0." else action.number
                    currentTokens + Token.Number(initialValue)
                }
            }

            updateStateWithTokens(state.copy(isCalculated = false), newTokens, evaluator)
        }

        is CalculatorAction.AppendOperator -> {
            if (state.tokens.isEmpty()) {
                // Allow unary minus at start if '-' is pressed
                if (action.operator == '-') {
                    return updateStateWithTokens(state.copy(isCalculated = false), listOf(Token.UnaryMinus), evaluator)
                }
                return state
            }

            val newTokens = when (val lastToken = state.tokens.lastOrNull()) {
                is Token.Operator -> state.tokens.dropLast(1) + Token.Operator(action.operator)
                is Token.UnaryMinus -> {
                    if (action.operator == '+') emptyList() else state.tokens
                }

                is Token.Parenthesis -> {
                    if (lastToken.symbol == '(' && action.operator == '-') {
                        state.tokens + Token.UnaryMinus
                    } else if (lastToken.symbol == '(') {
                        state.tokens // Don't allow other operators right after (
                    } else {
                        state.tokens + Token.Operator(action.operator)
                    }
                }

                else -> state.tokens + Token.Operator(action.operator)
            }
            updateStateWithTokens(state.copy(isCalculated = false), newTokens, evaluator)
        }

        is CalculatorAction.AppendParenthesis -> {
            val lastToken = state.tokens.lastOrNull()
            val openCount = state.tokens.count { it is Token.Parenthesis && it.symbol == '(' }
            val closeCount = state.tokens.count { it is Token.Parenthesis && it.symbol == ')' }

            val newTokens = if (openCount > closeCount) {
                // If there's an open parenthesis, try to close it if the last token allows
                if (lastToken is Token.Number || lastToken is Token.Percent || (lastToken is Token.Parenthesis && lastToken.symbol == ')')) {
                    state.tokens + Token.Parenthesis(')')
                } else {
                    // Otherwise open a new one (e.g. after an operator or '(')
                    state.tokens + Token.Parenthesis('(')
                }
            } else {
                // No open parentheses to close, so open a new one
                if (lastToken is Token.Number || lastToken is Token.Percent || (lastToken is Token.Parenthesis && lastToken.symbol == ')')) {
                    // Implicit multiplication
                    state.tokens + Token.Operator('*') + Token.Parenthesis('(')
                } else {
                    state.tokens + Token.Parenthesis('(')
                }
            }
            updateStateWithTokens(state.copy(isCalculated = false), newTokens, evaluator)
        }

        CalculatorAction.Percent -> {
            val lastToken = state.tokens.lastOrNull()
            val newTokens =
                if (lastToken is Token.Number || (lastToken is Token.Parenthesis && lastToken.symbol == ')')) {
                    state.tokens + Token.Percent
                } else {
                    state.tokens
                }
            updateStateWithTokens(state.copy(isCalculated = false), newTokens, evaluator)
        }

        CalculatorAction.Clear -> {
            state.copy(
                tokens = emptyList(),
                display = "0",
                ghostResult = "",
                isError = false,
                isCalculated = false
            )
        }

        CalculatorAction.Backspace -> {
            if (state.tokens.isEmpty()) return state

            val newTokens = when (val lastToken = state.tokens.last()) {
                is Token.Number -> {
                    if (lastToken.value.length > 1) {
                        state.tokens.dropLast(1) + Token.Number(lastToken.value.dropLast(1))
                    } else {
                        state.tokens.dropLast(1)
                    }
                }

                else -> state.tokens.dropLast(1)
            }
            updateStateWithTokens(state.copy(isCalculated = false), newTokens, evaluator)
        }

        CalculatorAction.Equals -> {
            val result = evaluateTokens(state.tokens, evaluator)
            if (result == null) {
                state.copy(isError = true, display = "Error")
            } else {
                val absoluteResult = result.abs().stripTrailingZeros().toPlainString()
                val isNegative = result.signum() < 0

                val newHistoryItem = CalculationHistoryItem(
                    expression = state.displayExpression,
                    result = if (isNegative) "−$absoluteResult" else absoluteResult
                )

                val newTokens = if (isNegative) {
                    listOf(Token.UnaryMinus, Token.Number(absoluteResult))
                } else {
                    listOf(Token.Number(absoluteResult))
                }

                state.copy(
                    display = if (isNegative) "-$absoluteResult" else absoluteResult,
                    tokens = newTokens,
                    isCalculated = true,
                    ghostResult = "",
                    history = (listOf(newHistoryItem) + state.history).take(50)
                )
            }
        }

        is CalculatorAction.LoadFromHistory -> {
            val resultVal = action.item.result.replace("−", "-")
            val isNegative = resultVal.startsWith("-")
            val absVal = resultVal.removePrefix("-")

            val newTokens = if (isNegative) {
                listOf(Token.UnaryMinus, Token.Number(absVal))
            } else {
                listOf(Token.Number(absVal))
            }

            state.copy(
                display = resultVal,
                tokens = newTokens,
                isCalculated = true,
                ghostResult = ""
            )
        }

        CalculatorAction.ClearHistory -> {
            state.copy(history = emptyList())
        }
    }
}

private fun updateStateWithTokens(
    state: CalculatorState,
    newTokens: List<Token>,
    evaluator: ExpressionEvaluator
): CalculatorState {
    val result = evaluateTokens(newTokens, evaluator)
    val ghost = result?.stripTrailingZeros()?.toPlainString() ?: ""

    val displayVal = if (newTokens.isEmpty()) {
        "0"
    } else {
        val last = newTokens.last()
        if (last is Token.Number) {
            // If it's the only token and it's a number, just show it
            if (newTokens.size == 1) last.value
            // If we have a UnaryMinus followed by a number, show them together
            else if (newTokens.size == 2 && newTokens.first() is Token.UnaryMinus) "-${last.value}"
            else last.value
        } else {
            ghost.ifEmpty { "0" }
        }
    }

    return state.copy(
        tokens = newTokens,
        display = displayVal,
        ghostResult = if (ghost == displayVal || ghost == "0") "" else ghost,
        isError = false
    )
}

private fun evaluateTokens(tokens: List<Token>, evaluator: ExpressionEvaluator): BigDecimal? {
    if (tokens.isEmpty()) return BigDecimal.ZERO
    return try {
        evaluator.evaluateTokens(tokens)
    } catch (_: Exception) {
        null
    }
}
