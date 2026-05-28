package com.osscabinet.ossWallet.domain.card

import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionType
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class CardFilterRulesTest {

    @Test
    fun `CategoricalRule Limited - when input matches subset - returns subset`() {
        val rule = CategoricalRule.Limited(setOf(TransactionType.INCOME, TransactionType.EXPENSE))
        val selection = setOf(TransactionType.INCOME)
        
        val result = rule.apply(selection)
        
        assertEquals(setOf(TransactionType.INCOME), result)
    }

    @Test
    fun `CategoricalRule Limited - when input is outside subset - returns empty`() {
        val rule = CategoricalRule.Limited(setOf(TransactionType.INCOME, TransactionType.EXPENSE))
        val selection = setOf(TransactionType.TRANSFER)
        
        val result = rule.apply(selection)
        
        assertTrue(result.isEmpty())
    }

    @Test
    fun `CategoricalRule Limited - when input is empty (select all) - returns allowed subset`() {
        val rule = CategoricalRule.Limited(setOf(TransactionType.INCOME, TransactionType.EXPENSE))
        val selection = emptySet<TransactionType>()
        
        val result = rule.apply(selection)
        
        assertEquals(setOf(TransactionType.INCOME, TransactionType.EXPENSE), result)
    }

    @Test
    fun `CategoricalRule Locked - always returns forced values`() {
        val rule = CategoricalRule.Locked(setOf(TransactionType.INCOME))
        val selection = setOf(TransactionType.EXPENSE, TransactionType.TRANSFER)
        
        val result = rule.apply(selection)
        
        assertEquals(setOf(TransactionType.INCOME), result)
    }

    @Test
    fun `CategoricalRule Filtered - strips forbidden values`() {
        val rule = CategoricalRule.Filtered(setOf("acc_hidden"))
        val selection = setOf("acc1", "acc_hidden", "acc2")
        
        val result = rule.apply(selection)
        
        assertEquals(setOf("acc1", "acc2"), result)
    }

    @Test
    fun `RangeRule Clamped - enforces boundaries`() {
        val rule = RangeRule.Clamped(BigDecimal("10"), BigDecimal("100"))
        
        assertEquals(BigDecimal("10"), rule.applyMin(BigDecimal("5")))
        assertEquals(BigDecimal("20"), rule.applyMin(BigDecimal("20")))
        assertEquals(BigDecimal("100"), rule.applyMax(BigDecimal("150")))
        assertEquals(BigDecimal("50"), rule.applyMax(BigDecimal("50")))
    }

    @Test
    fun `RangeRule Clamped - fills nulls with boundaries`() {
        val rule = RangeRule.Clamped(BigDecimal("10"), BigDecimal("100"))
        
        assertEquals(BigDecimal("10"), rule.applyMin(null))
        assertEquals(BigDecimal("100"), rule.applyMax(null))
    }

    @Test
    fun `ToggleRule Forced - overrides user choice`() {
        val rule = ToggleRule.Forced(true)
        
        assertTrue(rule.apply(false))
        assertTrue(rule.apply(true))
    }

    @Test
    fun `normalize - applies all rules to filter`() {
        val rules = CardFilterRules(
            typeRule = CategoricalRule.Limited(setOf(TransactionType.EXPENSE)),
            amountRule = RangeRule.Clamped(BigDecimal.ZERO, BigDecimal("1000")),
            futureToggleRule = ToggleRule.Forced(true)
        )
        val inputFilter = TransactionFilter(
            transactionTypes = setOf(TransactionType.INCOME, TransactionType.EXPENSE),
            minAmount = BigDecimal("-50"),
            maxAmount = BigDecimal("5000"),
            excludeFuture = false
        )
        
        val normalized = rules.normalize(inputFilter)
        
        assertEquals(setOf(TransactionType.EXPENSE), normalized.transactionTypes)
        assertEquals(BigDecimal.ZERO, normalized.minAmount)
        assertEquals(BigDecimal("1000"), normalized.maxAmount)
        assertTrue(normalized.excludeFuture)
    }

    @Test
    fun `layeredWith - merges rules correctly (most restrictive wins)`() {
        val baseRules = CardFilterRules(
            typeRule = CategoricalRule.Limited(setOf(TransactionType.INCOME, TransactionType.EXPENSE))
        )
        val constraintRules = CardFilterRules(
            typeRule = CategoricalRule.Limited(setOf(TransactionType.EXPENSE, TransactionType.TRANSFER))
        )
        
        val combined = baseRules.layeredWith(constraintRules)
        
        // Intersection of {INCOME, EXPENSE} and {EXPENSE, TRANSFER} is {EXPENSE}
        val result = combined.typeRule.apply(emptySet())
        assertEquals(setOf(TransactionType.EXPENSE), result)
    }
}
