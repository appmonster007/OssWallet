package com.osscabinet.ossWallet.domain.calculator

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Stack

/**
 * Robust expression evaluator adapted from Calc-OS patterns
 * Uses Shunting-yard algorithm for expression parsing
 * Supports: +, -, *, /, %, (, ), and decimal numbers
 */
class ExpressionEvaluator(private val precision: Int = 2) {
    
    /**
     * Evaluates a mathematical expression string
     * @param expr The expression to evaluate (e.g., "10+5*2")
     * @return The result as BigDecimal
     */
    fun evaluate(expr: String): BigDecimal {
        if (expr.isBlank()) return BigDecimal.ZERO
        
        try {
            // Sanitize input
            val sanitized = sanitizeExpression(expr)
            
            // Parse and evaluate using Shunting-yard algorithm
            val postfix = infixToPostfix(sanitized)
            val result = evaluatePostfix(postfix)
            
            return result.setScale(precision, RoundingMode.HALF_UP)
        } catch (e: Exception) {
            throw EvaluationException("Invalid expression: ${e.message}")
        }
    }
    
    private fun sanitizeExpression(expr: String): String {
        return expr
            .replace("×", "*")
            .replace("÷", "/")
            .replace("%", "/100")
            .trim()
    }
    
    /**
     * Converts infix expression to postfix notation (Shunting-yard algorithm)
     */
    private fun infixToPostfix(infix: String): List<String> {
        val output = mutableListOf<String>()
        val operators = Stack<Char>()
        var i = 0
        var expectOperand = true // True when we expect an operand (number or unary minus)
        
        while (i < infix.length) {
            val c = infix[i]
            
            when {
                c.isWhitespace() -> i++
                c.isDigit() || c == '.' -> {
                    val number = parseNumber(infix, i)
                    output.add(number.first)
                    i = number.second
                    expectOperand = false
                }
                c == '(' -> {
                    operators.push(c)
                    i++
                    expectOperand = true
                }
                c == ')' -> {
                    while (operators.isNotEmpty() && operators.peek() != '(') {
                        output.add(operators.pop().toString())
                    }
                    if (operators.isNotEmpty()) operators.pop() // Remove '('
                    i++
                    expectOperand = false
                }
                c in "+-*/" -> {
                    if (c == '-' && expectOperand) {
                        // Unary minus - treat as negative number
                        i++
                        if (i < infix.length && (infix[i].isDigit() || infix[i] == '.')) {
                            val number = parseNumber(infix, i)
                            output.add("-" + number.first)
                            i = number.second
                            expectOperand = false
                        } else {
                            // Handle edge case of just unary minus
                            output.add("-0")
                            expectOperand = false
                        }
                    } else {
                        while (operators.isNotEmpty() && operators.peek() != '(' &&
                               precedence(c) <= precedence(operators.peek())) {
                            output.add(operators.pop().toString())
                        }
                        operators.push(c)
                        i++
                        expectOperand = true
                    }
                }
                else -> i++
            }
        }
        
        while (operators.isNotEmpty()) {
            output.add(operators.pop().toString())
        }
        
        return output
    }
    
    private fun parseNumber(expr: String, start: Int): Pair<String, Int> {
        val sb = StringBuilder()
        var i = start
        var hasDecimal = false
        
        while (i < expr.length) {
            val c = expr[i]
            if (c.isDigit()) {
                sb.append(c)
                i++
            } else if (c == '.' && !hasDecimal) {
                sb.append(c)
                hasDecimal = true
                i++
            } else {
                break
            }
        }
        
        return Pair(sb.toString(), i)
    }
    
    private fun precedence(op: Char): Int {
        return when (op) {
            '+', '-' -> 1
            '*', '/' -> 2
            else -> 0
        }
    }
    
    /**
     * Evaluates postfix expression
     */
    private fun evaluatePostfix(postfix: List<String>): BigDecimal {
        val stack = Stack<BigDecimal>()
        
        for (token in postfix) {
            when {
                token.isNotEmpty() && (token[0].isDigit() || token[0] == '.' || (token[0] == '-' && token.length > 1 && (token[1].isDigit() || token[1] == '.'))) -> {
                    stack.push(BigDecimal(token))
                }
                token in "+-*/" -> {
                    if (stack.size < 2) throw EvaluationException("Invalid expression")
                    val b = stack.pop()
                    val a = stack.pop()
                    stack.push(applyOperator(token[0], a, b))
                }
            }
        }
        
        if (stack.size != 1) throw EvaluationException("Invalid expression")
        return stack.pop()
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
}

class EvaluationException(message: String) : Exception(message)
