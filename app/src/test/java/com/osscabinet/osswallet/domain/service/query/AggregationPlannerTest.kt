package com.osscabinet.ossWallet.domain.service.query

import com.osscabinet.ossWallet.domain.model.query.AggregationDimension
import com.osscabinet.ossWallet.domain.model.query.AggregationFunction
import com.osscabinet.ossWallet.domain.model.query.MetricConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AggregationPlannerTest {

    private val planner = AggregationPlanner()

    @Test
    fun `plan should auto-insert currency when amount metrics are present`() {
        val dimensions = listOf(AggregationDimension.CATEGORY)
        val metrics = listOf(
            MetricConfig("sum", AggregationFunction.SUM, "amount")
        )

        val plan = planner.plan(dimensions, metrics)

        assertEquals(2, plan.sqlDimensions.size)
        assertEquals(AggregationDimension.CATEGORY, plan.sqlDimensions[0])
        assertEquals(AggregationDimension.CURRENCY, plan.sqlDimensions[1])
    }

    @Test
    fun `plan should not insert currency if already present`() {
        val dimensions = listOf(AggregationDimension.CURRENCY, AggregationDimension.CATEGORY)
        val metrics = listOf(
            MetricConfig("sum", AggregationFunction.SUM, "amount")
        )

        val plan = planner.plan(dimensions, metrics)

        assertEquals(2, plan.sqlDimensions.size)
        assertEquals(AggregationDimension.CURRENCY, plan.sqlDimensions[0])
        assertEquals(AggregationDimension.CATEGORY, plan.sqlDimensions[1])
    }

    @Test
    fun `plan should respect SQL level limit`() {
        val dimensions = listOf(
            AggregationDimension.ACCOUNT,
            AggregationDimension.CURRENCY,
            AggregationDimension.CATEGORY,
            AggregationDimension.STATUS
        )
        val metrics = listOf(MetricConfig("count", AggregationFunction.COUNT, "*"))

        // Use AD_HOC which uses 1 SQL level
        val plan = planner.plan(dimensions, metrics, useCase = AggregationPlanner.UseCase.AD_HOC)

        assertEquals(1, plan.sqlDimensions.size)
        assertEquals(3, plan.kotlinDimensions.size)
        assertEquals(AggregationDimension.ACCOUNT, plan.sqlDimensions[0])
        assertEquals(AggregationDimension.CURRENCY, plan.kotlinDimensions[0])
        assertEquals(AggregationDimension.CATEGORY, plan.kotlinDimensions[1])
        assertEquals(AggregationDimension.STATUS, plan.kotlinDimensions[2])
    }
}
