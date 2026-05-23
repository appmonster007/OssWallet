package com.osscabinet.ossWallet.ui.calculator

import com.osscabinet.ossWallet.domain.calculator.ExpressionEvaluator
import com.osscabinet.ossWallet.domain.model.TransactionType
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
    val transactionType: TransactionType = TransactionType.INCOME,
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
    data class SetInitialType(val type: TransactionType) : CalculatorAction()
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
        is CalculatorAction.SetInitialType -> {
            val initialTokens =
                if (action.type == TransactionType.EXPENSE) listOf(Token.UnaryMinus) else emptyList()
            state.copy(
                transactionType = action.type,
                tokens = initialTokens,
                display = if (action.type == TransactionType.EXPENSE) "-0" else "0"
            )
        }

        is CalculatorAction.AppendNumber -> {
            val currentTokens = if (state.isCalculated) emptyList() else state.tokens
            val newTokens = when (val lastToken = currentTokens.lastOrNull()) {
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
            if (state.tokens.isEmpty() || (state.tokens.size == 1 && state.tokens.first() is Token.UnaryMinus)) {
                // Empty or just UnaryMinus + Operator toggles type
                val newType =
                    if (action.operator == '-') TransactionType.EXPENSE else TransactionType.INCOME
                val newTokens =
                    if (newType == TransactionType.EXPENSE) listOf(Token.UnaryMinus) else emptyList()
                return state.copy(
                    transactionType = newType,
                    tokens = newTokens,
                    display = if (newType == TransactionType.EXPENSE) "-0" else "0",
                    isCalculated = false
                )
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
            val initialTokens =
                if (state.transactionType == TransactionType.EXPENSE) listOf(Token.UnaryMinus) else emptyList()
            state.copy(
                tokens = initialTokens,
                display = if (state.transactionType == TransactionType.EXPENSE) "-0" else "0",
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
                val newType = when {
                    state.transactionType == TransactionType.TRANSFER -> TransactionType.TRANSFER
                    result.signum() < 0 -> TransactionType.EXPENSE
                    else -> TransactionType.INCOME
                }

                val newHistoryItem = CalculationHistoryItem(
                    expression = state.displayExpression,
                    result = if (result.signum() < 0) "−$absoluteResult" else absoluteResult
                )

                // If the result is negative, we represent it as UnaryMinus + Number in tokens 
                // to maintain consistent state for further operations
                val newTokens = if (result.signum() < 0) {
                    listOf(Token.UnaryMinus, Token.Number(absoluteResult))
                } else {
                    listOf(Token.Number(absoluteResult))
                }

                state.copy(
                    display = absoluteResult,
                    tokens = newTokens,
                    isCalculated = true,
                    transactionType = newType,
                    ghostResult = "",
                    history = (listOf(newHistoryItem) + state.history).take(50) // Keep last 50 items
                )
            }
        }

        is CalculatorAction.LoadFromHistory -> {
            // Logic to parse result back into tokens for further calculation
            val resultVal = action.item.result.replace("−", "-")
            val isNegative = resultVal.startsWith("-")
            val absVal = resultVal.removePrefix("-")

            val newTokens = if (isNegative) {
                listOf(Token.UnaryMinus, Token.Number(absVal))
            } else {
                listOf(Token.Number(absVal))
            }

            state.copy(
                display = absVal,
                tokens = newTokens,
                isCalculated = true,
                transactionType = if (isNegative) TransactionType.EXPENSE else TransactionType.INCOME,
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
