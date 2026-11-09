package com.osscabinet.ossWallet.domain.model

import org.junit.Test

class CategoryTransferRulesTest {
    @Test
    fun `the three transfer categories are Transfer, Initial Balance and Adjustment`() {
        assertThat(Category.isTransfer(Category.SYSTEM_TRANSFER)).isTrue()
        assertThat(Category.isTransfer(Category.SYSTEM_INITIAL_BALANCE)).isTrue()
        assertThat(Category.isTransfer(Category.SYSTEM_RECONCILIATION_ADJUSTMENT)).isTrue()
        assertThat(Category.isTransfer(Category.SYSTEM_UNKNOWN)).isFalse()
        assertThat(Category.isTransfer(Category.SYSTEM_OTHERS)).isFalse()
        assertThat(Category.isTransfer(1021L)).isFalse()
    }

    @Test
    fun `a transfer category that is not a transfer category falls back to Transfer`() {
        assertThat(Category.transferCategoryFor(Category.SYSTEM_UNKNOWN, isTwoSided = true)).isEqualTo(Category.SYSTEM_TRANSFER)
        assertThat(Category.transferCategoryFor(Category.SYSTEM_OTHERS, isTwoSided = false)).isEqualTo(Category.SYSTEM_TRANSFER)
        assertThat(Category.transferCategoryFor(1021L, isTwoSided = false)).isEqualTo(Category.SYSTEM_TRANSFER)
    }

    @Test
    fun `a single-leg transfer keeps its own system category`() {
        assertThat(Category.transferCategoryFor(Category.SYSTEM_INITIAL_BALANCE, isTwoSided = false))
            .isEqualTo(Category.SYSTEM_INITIAL_BALANCE)
        assertThat(Category.transferCategoryFor(Category.SYSTEM_RECONCILIATION_ADJUSTMENT, isTwoSided = false))
            .isEqualTo(Category.SYSTEM_RECONCILIATION_ADJUSTMENT)
    }

    @Test
    fun `a two-sided transfer is never an opening balance or adjustment`() {
        assertThat(Category.transferCategoryFor(Category.SYSTEM_INITIAL_BALANCE, isTwoSided = true)).isEqualTo(Category.SYSTEM_TRANSFER)
        assertThat(Category.transferCategoryFor(Category.SYSTEM_RECONCILIATION_ADJUSTMENT, isTwoSided = true)).isEqualTo(Category.SYSTEM_TRANSFER)
        assertThat(Category.transferCategoryFor(Category.SYSTEM_TRANSFER, isTwoSided = true)).isEqualTo(Category.SYSTEM_TRANSFER)
    }
}

// Truth is not on the app's unit-test classpath; this keeps the assertions readable with JUnit.
private class Subject<T>(private val actual: T) {
    fun isEqualTo(expected: Any?) = org.junit.Assert.assertEquals(expected, actual)
    fun isTrue() = org.junit.Assert.assertTrue(actual as Boolean)
    fun isFalse() = org.junit.Assert.assertFalse(actual as Boolean)
    fun containsExactly(vararg expected: Any?) = org.junit.Assert.assertEquals(expected.toSet(), actual as Set<*>)
}

private fun <T> assertThat(actual: T) = Subject(actual)
