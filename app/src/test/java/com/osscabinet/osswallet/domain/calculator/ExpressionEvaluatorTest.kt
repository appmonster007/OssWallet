package com.osscabinet.ossWallet.domain.calculator

import org.junit.Test
import org.junit.Assert.*
import java.math.BigDecimal

class ExpressionEvaluatorTest {
    
    private val evaluator = ExpressionEvaluator()
    
    @Test
    fun testSimpleAddition() {
        val result = evaluator.evaluate("10+5")
        assertEquals(BigDecimal("15.00"), result)
    }
    
    @Test
    fun testSimpleSubtraction() {
        val result = evaluator.evaluate("10-5")
        assertEquals(BigDecimal("5.00"), result)
    }
    
    @Test
    fun testSimpleMultiplication() {
        val result = evaluator.evaluate("10*5")
        assertEquals(BigDecimal("50.00"), result)
    }
    
    @Test
    fun testSimpleDivision() {
        val result = evaluator.evaluate("10/2")
        assertEquals(BigDecimal("5.00"), result)
    }
    
    @Test
    fun testOperatorPrecedence() {
        val result = evaluator.evaluate("10+5*2")
        assertEquals(BigDecimal("20.00"), result)
    }
    
    @Test
    fun testParentheses() {
        val result = evaluator.evaluate("(10+5)*2")
        assertEquals(BigDecimal("30.00"), result)
    }
    
    @Test
    fun testComplexExpression() {
        val result = evaluator.evaluate("10+5*2-8/4")
        assertEquals(BigDecimal("18.00"), result)
    }
    
    @Test
    fun testDecimalNumbers() {
        val result = evaluator.evaluate("10.5+5.25")
        assertEquals(BigDecimal("15.75"), result)
    }
    
    @Test
    fun testPercentage() {
        val result = evaluator.evaluate("50%")
        assertEquals(BigDecimal("0.50"), result)
    }
    
    @Test
    fun testNegativeNumbers() {
        val result = evaluator.evaluate("-10+5")
        assertEquals(BigDecimal("-5.00"), result)
    }
    
    @Test
    fun testEmptyExpression() {
        val result = evaluator.evaluate("")
        assertEquals(BigDecimal.ZERO, result)
    }
    
    @Test
    fun testBlankExpression() {
        val result = evaluator.evaluate("   ")
        assertEquals(BigDecimal.ZERO, result)
    }
    
    @Test(expected = EvaluationException::class)
    fun testInvalidExpression() {
        evaluator.evaluate("10++5")
    }
    
    @Test(expected = EvaluationException::class)
    fun testDivisionByZero() {
        evaluator.evaluate("10/0")
    }
    
    @Test
    fun testChainedOperations() {
        val result = evaluator.evaluate("10+5+3")
        assertEquals(BigDecimal("18.00"), result)
    }
    
    @Test
    fun testNestedParentheses() {
        val result = evaluator.evaluate("((10+5)*2)-3")
        assertEquals(BigDecimal("27.00"), result)
    }
    
    @Test
    fun testMixedOperators() {
        val result = evaluator.evaluate("10*2+5/2-3")
        assertEquals(BigDecimal("19.50"), result)
    }
}
