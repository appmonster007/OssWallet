package com.osscabinet.ossWallet.domain.calculator

import com.osscabinet.ossWallet.ui.calculator.Token
import org.junit.Test
import org.junit.Assert.*
import java.math.BigDecimal

class ExpressionEvaluatorTest {
    
    private val evaluator = ExpressionEvaluator()
    
    @Test
    fun testSimpleAddition() {
        val tokens = listOf(Token.Number("10"), Token.Operator('+'), Token.Number("5"))
        val result = evaluator.evaluateTokens(tokens)
        assertEquals(BigDecimal("15.00"), result)
    }
    
    @Test
    fun testSimpleSubtraction() {
        val tokens = listOf(Token.Number("10"), Token.Operator('-'), Token.Number("5"))
        val result = evaluator.evaluateTokens(tokens)
        assertEquals(BigDecimal("5.00"), result)
    }
    
    @Test
    fun testOperatorPrecedence() {
        val tokens = listOf(
            Token.Number("10"), 
            Token.Operator('+'), 
            Token.Number("5"), 
            Token.Operator('*'), 
            Token.Number("2")
        )
        val result = evaluator.evaluateTokens(tokens)
        assertEquals(BigDecimal("20.00"), result)
    }
    
    @Test
    fun testParentheses() {
        val tokens = listOf(
            Token.Parenthesis('('),
            Token.Number("10"), 
            Token.Operator('+'), 
            Token.Number("5"),
            Token.Parenthesis(')'),
            Token.Operator('*'), 
            Token.Number("2")
        )
        val result = evaluator.evaluateTokens(tokens)
        assertEquals(BigDecimal("30.00"), result)
    }
    
    @Test
    fun testUnaryMinus() {
        val tokens = listOf(Token.UnaryMinus, Token.Number("10"), Token.Operator('+'), Token.Number("5"))
        val result = evaluator.evaluateTokens(tokens)
        assertEquals(BigDecimal("-5.00"), result)
    }

    @Test
    fun testPercent() {
        val tokens = listOf(Token.Number("50"), Token.Percent)
        val result = evaluator.evaluateTokens(tokens)
        assertEquals(BigDecimal("0.50"), result)
    }

    @Test
    fun testAutoCorrection() {
        // Trailing operator and unclosed parenthesis: (10 + 5 +
        val tokens = listOf(
            Token.Parenthesis('('),
            Token.Number("10"),
            Token.Operator('+'),
            Token.Number("5"),
            Token.Operator('+')
        )
        val result = evaluator.evaluateTokens(tokens)
        assertEquals(BigDecimal("15.00"), result)
    }

    @Test
    fun testImplicitMultiplication() {
        // 2(3+4)
        val tokens = listOf(
            Token.Number("2"),
            Token.Parenthesis('('),
            Token.Number("3"),
            Token.Operator('+'),
            Token.Number("4"),
            Token.Parenthesis(')')
        )
        // Note: The reducer is responsible for inserting the * token for implicit multiplication.
        // Let's test the evaluation of tokens WITH the explicit operator first, 
        // then consider if ExpressionEvaluator should handle it too.
        
        val tokensWithOp = listOf(
            Token.Number("2"),
            Token.Operator('*'),
            Token.Parenthesis('('),
            Token.Number("3"),
            Token.Operator('+'),
            Token.Number("4"),
            Token.Parenthesis(')')
        )
        val result = evaluator.evaluateTokens(tokensWithOp)
        assertEquals(BigDecimal("14.00"), result)
    }

    @Test
    fun testBinaryPercent() {
        // 100 + 10% should be 110
        val tokens = listOf(
            Token.Number("100"),
            Token.Operator('+'),
            Token.Number("10"),
            Token.Percent
        )
        val result = evaluator.evaluateTokens(tokens)
        assertEquals(BigDecimal("110.00"), result)
    }

    @Test
    fun testBinaryPercentSubtraction() {
        // 100 - 20% should be 80
        val tokens = listOf(
            Token.Number("100"),
            Token.Operator('-'),
            Token.Number("20"),
            Token.Percent
        )
        val result = evaluator.evaluateTokens(tokens)
        assertEquals(BigDecimal("80.00"), result)
    }
}
