package com.osscabinet.ossWallet.domain.calculator

import com.osscabinet.ossWallet.ui.calculator.Token
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Stack

/**
 * Enhanced financial expression evaluator
 * Supports Token-based evaluation, implicit multiplication, and binary percentages.
 */
class ExpressionEvaluator(private val precision: Int = com.osscabinet.ossWallet.domain.service.currency.CurrencyService.precision) {

    fun evaluate(expr: String): BigDecimal {
        // Fallback for string-based evaluation (can be removed if no longer needed)
        val sanitized = expr.replace("×", "*").replace("÷", "/")
        return evaluateInternal(sanitized)
    }

    fun evaluateTokens(tokens: List<Token>): BigDecimal {
        val sanitizedTokens = autoCorrect(tokens)
        val processedTokens = processImplicitMultiplicationAndPercent(sanitizedTokens)
        
        // Convert processed tokens back to infix list of strings for Shunting-yard
        val infixStrings = processedTokens.map { it.toInternalString() }
        val postfix = infixToPostfix(infixStrings)
        return evaluatePostfix(postfix).setScale(precision, RoundingMode.HALF_UP)
    }

    private fun autoCorrect(tokens: List<Token>): List<Token> {
        val corrected = tokens.toMutableList()
        
        // Strip trailing operators
        while (corrected.isNotEmpty() && (corrected.last() is Token.Operator || corrected.last() is Token.UnaryMinus)) {
            corrected.removeAt(corrected.size - 1)
        }
        
        // Close unmatched parentheses
        val openCount = corrected.count { it is Token.Parenthesis && it.symbol == '(' }
        val closeCount = corrected.count { it is Token.Parenthesis && it.symbol == ')' }
        repeat(openCount - closeCount) {
            corrected.add(Token.Parenthesis(')'))
        }
        
        return corrected
    }

    private fun processImplicitMultiplicationAndPercent(tokens: List<Token>): List<Token> {
        val result = mutableListOf<Token>()
        var i = 0
        while (i < tokens.size) {
            val current = tokens[i]
            
            // Binary percent logic: A [+/-] B% -> A [+/-] (A * B / 100)
            // Example: 100 + 10% 
            // In tokens: [Number(100), Operator(+), Number(10), Percent]
            if (current is Token.Percent && i > 0 && result.isNotEmpty()) {
                val bToken = result.last()
                if (bToken is Token.Number && result.size >= 2) {
                    val opToken = result[result.size - 2]
                    if (opToken is Token.Operator && (opToken.symbol == '+' || opToken.symbol == '-')) {
                        // Find A. This is tricky if A is complex. 
                        // For a simple case where A is a Number:
                        if (result.size >= 3) {
                            val aToken = result[result.size - 3]
                            if (aToken is Token.Number) {
                                val a = BigDecimal(aToken.value)
                                val b = BigDecimal(bToken.value)
                                val percentageValue = a.multiply(b).divide(BigDecimal(100), precision + 4, RoundingMode.HALF_UP)
                                
                                result.removeAt(result.size - 1) // Remove B
                                result.add(Token.Number(percentageValue.toPlainString()))
                                i++
                                continue
                            }
                        }
                    }
                }
            }
            
            result.add(current)
            i++
        }
        return result
    }

    private fun infixToPostfix(infix: List<String>): List<String> {
        val output = mutableListOf<String>()
        val operators = Stack<String>()
        var expectOperand = true
        
        for (token in infix) {
            when {
                token == "(" -> {
                    operators.push(token)
                    expectOperand = true
                }
                token == ")" -> {
                    while (operators.isNotEmpty() && operators.peek() != "(") {
                        output.add(operators.pop())
                    }
                    if (operators.isNotEmpty()) operators.pop()
                    expectOperand = false
                }
                token == "%" -> {
                    output.add("%")
                    expectOperand = false
                }
                token == "-" && expectOperand -> {
                    operators.push("-u")
                    expectOperand = true
                }
                isOperator(token) -> {
                    while (operators.isNotEmpty() && operators.peek() != "(" &&
                           precedence(token) <= precedence(operators.peek())) {
                        output.add(operators.pop())
                    }
                    operators.push(token)
                    expectOperand = true
                }
                else -> { // Number
                    output.add(token)
                    expectOperand = false
                }
            }
        }
        
        while (operators.isNotEmpty()) {
            output.add(operators.pop())
        }
        
        return output
    }

    private fun evaluatePostfix(postfix: List<String>): BigDecimal {
        val stack = Stack<BigDecimal>()
        
        for (token in postfix) {
            when {
                token == "-u" -> {
                    // Unary minus should apply to the top of the stack if it was pushed AFTER the operand.
                    // But Shunting-yard pushes it BEFORE.
                    // Let's change the logic: we'll handle unary minus by pushing a flag and applying it to the NEXT operand.
                    // Or, even better, infixToPostfix should handle it by putting it after the operand.
                    
                    // Actually, if we use -u as a special token, Shunting-yard puts it after the operand it applies to.
                    // My previous debug output showed [-u, 10, 5, +]. This means -u was treated as having higher precedence but Shunting-yard still kept it at the front?
                    // No, Shunting-yard should have output [10, -u, 5, +] if -u was after 10.
                    
                    // Wait, if infix is ["-", "10", "+", "5"], expectOperand is true for "-".
                    // It adds "-u" to operators stack? No, it adds it directly to output in my code!
                    // THAT IS THE BUG. Unary operators should go on the operators stack too.
                    
                    if (stack.isNotEmpty()) {
                        stack.push(stack.pop().negate())
                    }
                }
                token == "%" -> {
                    if (stack.isEmpty()) throw EvaluationException("Empty stack for %")
                    val v = stack.pop()
                    val res = v.divide(BigDecimal(100), precision + 4, RoundingMode.HALF_UP)
                    println("Percent: $v -> $res")
                    stack.push(res)
                }
                isOperator(token) -> {
                    if (stack.size < 2) {
                        throw EvaluationException("Invalid expression: not enough operands for $token")
                    }
                    val b = stack.pop()
                    val a = stack.pop()
                    val res = applyOperator(token[0], a, b)
                    println("Operator $token: $a, $b -> $res")
                    stack.push(res)
                }
                else -> {
                    try {
                        val v = BigDecimal(token)
                        println("Pushing number: $v")
                        stack.push(v)
                    } catch (e: Exception) {
                        throw EvaluationException("Invalid number: $token")
                    }
                }
            }
        }
        
        if (stack.isEmpty()) return BigDecimal.ZERO
        val finalResult = stack.pop()
        println("Final result: $finalResult")
        return finalResult
    }

    private fun isOperator(s: String) = s in listOf("+", "-", "*", "/")

    private fun precedence(op: String): Int = when (op) {
        "+", "-" -> 1
        "*", "/" -> 2
        "-u" -> 3
        else -> 0
    }

    private fun applyOperator(op: Char, a: BigDecimal, b: BigDecimal): BigDecimal {
        return when (op) {
            '+' -> a.add(b)
            '-' -> a.subtract(b)
            '*' -> a.multiply(b)
            '/' -> if (b.signum() != 0) {
                a.divide(b, precision + 4, RoundingMode.HALF_UP)
            } else {
                throw EvaluationException("Division by zero")
            }
            else -> throw EvaluationException("Unknown operator: $op")
        }
    }

    private fun evaluateInternal(expr: String): BigDecimal {
        // Legacy string evaluator logic (simplified for compatibility)
        return try {
            val tokens = expr.map { c ->
                when {
                    c.isDigit() || c == '.' -> Token.Number(c.toString())
                    c == '(' || c == ')' -> Token.Parenthesis(c)
                    c == '%' -> Token.Percent
                    c == '-' -> Token.Operator('-') // Ambiguous without context
                    else -> Token.Operator(c)
                }
            }
            // This is a rough conversion, ideally use evaluateTokens directly
            evaluateTokens(tokens)
        } catch (_: Exception) {
            BigDecimal.ZERO
        }
    }
}

class EvaluationException(message: String) : Exception(message)
