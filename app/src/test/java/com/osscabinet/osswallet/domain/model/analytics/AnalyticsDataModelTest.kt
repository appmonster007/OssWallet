package com.osscabinet.ossWallet.domain.model.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

/**
 * Unit tests for analytics data models.
 * Tests ensure that analytics data models are properly constructed.
 */
class AnalyticsDataModelTest {

    @Test
    fun `CategoryBreakdownData can be created with valid parameters`() {
        val data = CategoryBreakdownData(
            category = "Food",
            categoryId = 1L,
            netBalance = BigDecimal("100.50"),
            income = BigDecimal("200.00"),
            expense = BigDecimal("99.50"),
            colorHex = 0xFFFF5733.toInt(),
            percentage = 0.25f
        )
        assertEquals("Food", data.category)
        assertEquals(1L, data.categoryId)
        assertEquals(BigDecimal("100.50"), data.netBalance)
        assertEquals(BigDecimal("200.00"), data.income)
        assertEquals(BigDecimal("99.50"), data.expense)
        assertEquals(0xFFFF5733.toInt(), data.colorHex)
        assertEquals(0.25f, data.percentage, 0.001f)
    }

    @Test
    fun `CategoryBreakdownData with null categoryId can be created`() {
        val data = CategoryBreakdownData(
            category = "Food",
            categoryId = null,
            netBalance = BigDecimal("100.50"),
            income = BigDecimal("200.00"),
            expense = BigDecimal("99.50"),
            colorHex = 0xFFFF5733.toInt(),
            percentage = 0.25f
        )
        assertNotNull(data)
        assertEquals("Food", data.category)
    }

    @Test
    fun `TimelineTrajectoryData can be created with valid parameters`() {
        val timestamp = System.currentTimeMillis()
        val data = TimelineTrajectoryData(
            timestamp = timestamp,
            balance = BigDecimal("1000.00"),
            expense = BigDecimal("200.00"),
            income = BigDecimal("500.00")
        )
        assertEquals(timestamp, data.timestamp)
        assertEquals(BigDecimal("1000.00"), data.balance)
        assertEquals(BigDecimal("200.00"), data.expense)
        assertEquals(BigDecimal("500.00"), data.income)
    }

    @Test
    fun `CategoryExpenseTreemapData can be created with valid parameters`() {
        val data = CategoryExpenseTreemapData(
            category = "Food",
            categoryId = 1L,
            expense = BigDecimal("500.00"),
            colorHex = 0xFFFF5733.toInt(),
            percentage = 0.5f
        )
        assertEquals("Food", data.category)
        assertEquals(1L, data.categoryId)
        assertEquals(BigDecimal("500.00"), data.expense)
        assertEquals(0xFFFF5733.toInt(), data.colorHex)
        assertEquals(0.5f, data.percentage, 0.001f)
    }

    @Test
    fun `CategoryExpenseTreemapData with null categoryId can be created`() {
        val data = CategoryExpenseTreemapData(
            category = "Food",
            categoryId = null,
            expense = BigDecimal("500.00"),
            colorHex = 0xFFFF5733.toInt(),
            percentage = 0.5f
        )
        assertNotNull(data)
        assertEquals("Food", data.category)
    }

    @Test
    fun `CategoryExpenseTreemapData with children can be created`() {
        val childData = CategoryExpenseTreemapData(
            category = "Groceries",
            categoryId = 2L,
            expense = BigDecimal("300.00"),
            colorHex = 0xFF33FF57.toInt(),
            percentage = 0.3f
        )
        val data = CategoryExpenseTreemapData(
            category = "Food",
            categoryId = 1L,
            expense = BigDecimal("500.00"),
            colorHex = 0xFFFF5733.toInt(),
            percentage = 0.5f,
            subcategoryData = listOf(childData)
        )
        assertNotNull(data.subcategoryData)
        assertEquals(1, data.subcategoryData?.size)
        assertEquals("Groceries", data.subcategoryData?.get(0)?.category)
    }

    @Test
    fun `CategoryBreakdownData percentage is within valid range`() {
        val data = CategoryBreakdownData(
            category = "Food",
            categoryId = 1L,
            netBalance = BigDecimal("100.50"),
            income = BigDecimal("200.00"),
            expense = BigDecimal("99.50"),
            colorHex = 0xFFFF5733.toInt(),
            percentage = 0.75f
        )
        assertTrue(data.percentage >= 0f)
        assertTrue(data.percentage <= 1f)
    }

    @Test
    fun `TimelineTrajectoryData can have zero values`() {
        val timestamp = System.currentTimeMillis()
        val data = TimelineTrajectoryData(
            timestamp = timestamp,
            balance = BigDecimal.ZERO,
            expense = BigDecimal.ZERO,
            income = BigDecimal.ZERO
        )
        assertEquals(BigDecimal.ZERO, data.balance)
        assertEquals(BigDecimal.ZERO, data.expense)
        assertEquals(BigDecimal.ZERO, data.income)
    }

    @Test
    fun `CategoryBreakdownData list can be created`() {
        val dataList = listOf(
            CategoryBreakdownData(
                category = "Food",
                categoryId = 1L,
                netBalance = BigDecimal("100.50"),
                income = BigDecimal("200.00"),
                expense = BigDecimal("99.50"),
                colorHex = 0xFFFF5733.toInt(),
                percentage = 0.25f
            ),
            CategoryBreakdownData(
                category = "Transport",
                categoryId = 2L,
                netBalance = BigDecimal("200.00"),
                income = BigDecimal("400.00"),
                expense = BigDecimal("200.00"),
                colorHex = 0xFF33FF57.toInt(),
                percentage = 0.50f
            )
        )
        assertEquals(2, dataList.size)
        assertEquals("Food", dataList[0].category)
        assertEquals("Transport", dataList[1].category)
    }

    @Test
    fun `TimelineTrajectoryData list can be created`() {
        val timestamp = System.currentTimeMillis()
        val dataList = listOf(
            TimelineTrajectoryData(
                timestamp = timestamp,
                balance = BigDecimal("1000.00"),
                expense = BigDecimal("200.00"),
                income = BigDecimal("500.00")
            ),
            TimelineTrajectoryData(
                timestamp = timestamp + 86400000,
                balance = BigDecimal("1300.00"),
                expense = BigDecimal("300.00"),
                income = BigDecimal("600.00")
            )
        )
        assertEquals(2, dataList.size)
        assertEquals(BigDecimal("1000.00"), dataList[0].balance)
        assertEquals(BigDecimal("1300.00"), dataList[1].balance)
    }

    @Test
    fun `CategoryExpenseTreemapData list can be created`() {
        val dataList = listOf(
            CategoryExpenseTreemapData(
                category = "Food",
                categoryId = 1L,
                expense = BigDecimal("500.00"),
                colorHex = 0xFFFF5733.toInt(),
                percentage = 0.5f
            ),
            CategoryExpenseTreemapData(
                category = "Transport",
                categoryId = 2L,
                expense = BigDecimal("300.00"),
                colorHex = 0xFF33FF57.toInt(),
                percentage = 0.3f
            )
        )
        assertEquals(2, dataList.size)
        assertEquals("Food", dataList[0].category)
        assertEquals("Transport", dataList[1].category)
    }
}
