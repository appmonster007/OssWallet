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
    val transactionType: String = "income"
) {
    val expression: String get() = tokens.joinToString("") { it.toInternalString() }
    val displayExpression: String get() = tokens.joinToString("") { it.toDisplayString() }
}

sealed class CalculatorAction {
    data class AppendNumber(val number: String) : CalculatorAction()
    data class AppendOperator(val operator: Char) : CalculatorAction()
    data class AppendParenthesis(val parenthesis: Char) : CalculatorAction()
    object Clear : CalculatorAction()
    object Backspace : CalculatorAction()
    object Equals : CalculatorAction()
    object Percent : CalculatorAction()
    data class SetInitialType(val type: String) : CalculatorAction()
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
        is CalculatorAction.SetInitialType -> {
            val initialTokens = if (action.type == "expense") listOf(Token.UnaryMinus) else emptyList()
            state.copy(
                transactionType = action.type,
                tokens = initialTokens,
                display = if (action.type == "expense") "-0" else "0"
            )
        }

        is CalculatorAction.AppendNumber -> {
            val currentTokens = if (state.isCalculated) emptyList() else state.tokens
            val lastToken = currentTokens.lastOrNull()
            
            val newTokens = when (lastToken) {
                is Token.Number -> {
                    // Prevent multiple decimals
                    if (action.number == "." && lastToken.value.contains(".")) {
                        currentTokens
                    } else {
                        currentTokens.dropLast(1) + Token.Number(lastToken.value + action.number)
                    }
                }
                is Token.Percent, is Token.Parenthesis -> {
                    if (lastToken is Token.Parenthesis && lastToken.symbol == '(') {
                        currentTokens + Token.Number(action.number)
                    } else {
                        // Implicit multiplication: 5% 2 -> 5% * 2
                        currentTokens + Token.Operator('*') + Token.Number(action.number)
                    }
                }
                else -> currentTokens + Token.Number(action.number)
            }
            
            updateStateWithTokens(state.copy(isCalculated = false), newTokens, evaluator)
        }

        is CalculatorAction.AppendOperator -> {
            if (state.tokens.isEmpty()) {
                // Empty + Operator toggles type
                val newType = if (action.operator == '-') "expense" else "income"
                val newTokens = if (newType == "expense") listOf(Token.UnaryMinus) else emptyList()
                return state.copy(transactionType = newType, tokens = newTokens, display = if (newType == "expense") "-0" else "0")
            }

            val lastToken = state.tokens.lastOrNull()
            val newTokens = when (lastToken) {
                is Token.Operator -> state.tokens.dropLast(1) + Token.Operator(action.operator)
                is Token.UnaryMinus -> {
                    if (action.operator == '+') emptyList() else state.tokens
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
            val newTokens = if (lastToken is Token.Number || (lastToken is Token.Parenthesis && lastToken.symbol == ')')) {
                state.tokens + Token.Percent
            } else {
                state.tokens
            }
            updateStateWithTokens(state.copy(isCalculated = false), newTokens, evaluator)
        }

        CalculatorAction.Clear -> {
            val initialTokens = if (state.transactionType == "expense") listOf(Token.UnaryMinus) else emptyList()
            state.copy(
                tokens = initialTokens,
                display = if (state.transactionType == "expense") "-0" else "0",
                ghostResult = "",
                isError = false,
                isCalculated = false
            )
        }

        CalculatorAction.Backspace -> {
            if (state.tokens.isEmpty()) return state
            
            val lastToken = state.tokens.last()
            val newTokens = when (lastToken) {
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
                val newType = if (result.signum() < 0) "expense" else "income"
                state.copy(
                    display = absoluteResult,
                    tokens = listOf(Token.Number(absoluteResult)),
                    isCalculated = true,
                    transactionType = newType,
                    ghostResult = ""
                )
            }
        }
    }
}

private fun updateStateWithTokens(
    state: CalculatorState, 
    newTokens: List<Token>, 
    evaluator: ExpressionEvaluator
): CalculatorState {
    val ghost = if (newTokens.isNotEmpty()) {
        evaluateTokens(newTokens, evaluator)?.abs()?.stripTrailingZeros()?.toPlainString() ?: ""
    } else ""
    
    val displayVal = if (newTokens.isEmpty()) {
        "0"
    } else {
        val last = newTokens.last()
        if (last is Token.Number) last.value else ghost.ifEmpty { "0" }
    }

    return state.copy(
        tokens = newTokens,
        display = displayVal,
        ghostResult = if (ghost == displayVal) "" else ghost,
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
