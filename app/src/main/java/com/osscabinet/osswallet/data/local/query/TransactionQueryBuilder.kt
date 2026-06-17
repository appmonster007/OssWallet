package com.osscabinet.ossWallet.data.local.query

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.query.AggregationDimension
import com.osscabinet.ossWallet.domain.model.query.AggregationFunction
import com.osscabinet.ossWallet.domain.model.query.AggregationLimits
import com.osscabinet.ossWallet.domain.model.query.MetricConfig
import com.osscabinet.ossWallet.domain.service.query.AggregationPlanner
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService

/**
 * Generates type-safe, parameterized SQLite queries for complex aggregations.
 */
class TransactionQueryBuilder {

    fun buildAggregationQuery(
        profileId: Int,
        filter: TransactionFilter,
        plan: AggregationPlanner.AggregationPlan
    ): SupportSQLiteQuery {
        return buildQuery(profileId, filter, plan = plan)
    }

    fun buildListQuery(
        profileId: Int,
        filter: TransactionFilter,
        limit: Int? = null
    ): SupportSQLiteQuery {
        return buildQuery(profileId, filter, limit = limit)
    }

    private fun buildQuery(
        profileId: Int,
        filter: TransactionFilter,
        plan: AggregationPlanner.AggregationPlan? = null,
        limit: Int? = null
    ): SupportSQLiteQuery {
        val select = StringBuilder("SELECT ")
        val group = StringBuilder("GROUP BY ")
        val args = mutableListOf<Any>()

        if (plan != null) {
            // 1. Dimensions (d1 - d4)
            plan.sqlDimensions.forEachIndexed { index, dim ->
                val alias = "d${index + 1}"
                val expression =
                    if (dim == AggregationDimension.TIME && plan.timeGranularity != null) {
                        "strftime('${plan.timeGranularity.strftime}', datetime(timestamp / 1000, 'unixepoch'))"
                    } else {
                        dim.column
                    }
                select.append("$expression AS $alias, ")
                group.append("$alias, ")
            }

            // Pad dimensions
            for (i in (plan.sqlDimensions.size + 1)..AggregationLimits.MAX_DIMENSIONS) {
                select.append("NULL AS d$i, ")
            }

            // 2. Metrics (m1 - m10)
            plan.metrics.take(AggregationLimits.MAX_METRICS).forEachIndexed { index, metric ->
                val alias = "m${index + 1}"
                val func = when (metric.function) {
                    AggregationFunction.SUM -> "SUM"
                    AggregationFunction.AVG -> "AVG"
                    AggregationFunction.MIN -> "MIN"
                    AggregationFunction.MAX -> "MAX"
                    AggregationFunction.COUNT -> "COUNT"
                }

                // Handle Metric Filters
                val filterExpr = when (metric.filter) {
                    MetricConfig.MetricFilter.INCOME_ONLY -> "CASE WHEN transactionType = 'income' THEN ${metric.column} ELSE 0.0 END"
                    MetricConfig.MetricFilter.EXPENSE_ONLY -> "CASE WHEN transactionType = 'expense' THEN ${metric.column} ELSE 0.0 END"
                    MetricConfig.MetricFilter.TRANSFER_ONLY -> "CASE WHEN transactionType IN ('transfer_inflow','transfer_outflow') THEN ${metric.column} ELSE 0.0 END"
                    MetricConfig.MetricFilter.TRANSFER_IN_ONLY -> "CASE WHEN (transactionType = 'transfer_inflow') THEN ${metric.column} ELSE 0.0 END"
                    MetricConfig.MetricFilter.TRANSFER_OUT_ONLY -> "CASE WHEN (transactionType = 'transfer_outflow') THEN ${metric.column} ELSE 0.0 END"
                    MetricConfig.MetricFilter.CROSS_CCY_TRANSFER_IN_ONLY -> "CASE WHEN (transactionType = 'transfer_inflow') AND (targetAccountId IS NULL OR currency <> targetCurrency) THEN ${metric.column} ELSE 0.0 END"
                    MetricConfig.MetricFilter.CROSS_CCY_TRANSFER_OUT_ONLY -> "CASE WHEN (transactionType = 'transfer_outflow') AND (targetAccountId IS NULL OR currency <> targetCurrency) THEN ${metric.column} ELSE 0.0 END"
                    MetricConfig.MetricFilter.POSITIVE_ONLY -> "CASE WHEN ${metric.column} > 0 THEN ${metric.column} ELSE 0.0 END"
                    MetricConfig.MetricFilter.NEGATIVE_ONLY -> "CASE WHEN ${metric.column} < 0 THEN ${metric.column} ELSE 0.0 END"
                    MetricConfig.MetricFilter.NONE -> metric.column
                }

                // Handle Metric Transforms
                val finalExpr = when (metric.transform) {
                    MetricConfig.MetricTransform.ABS -> "ABS($filterExpr)"
                    MetricConfig.MetricTransform.NEGATE -> "-($filterExpr)"
                    MetricConfig.MetricTransform.NONE -> filterExpr
                }

                // Force REAL casting for numeric functions to preserve decimal precision during aggregation,
                // then CAST the final aggregate result to TEXT to prevent Room/SQLite from using 
                // floating point types that might round or use scientific notation.
                val wrappedExpr = if (func != "COUNT") {
                    "CAST($func(CAST($finalExpr AS REAL)) AS TEXT)"
                } else {
                    "$func($finalExpr)"
                }
                select.append("$wrappedExpr AS $alias, ")
            }

            // Pad metrics (RawAggregateResult has m1..m10)
            for (i in (plan.metrics.size + 1)..AggregationLimits.MAX_METRICS) {
                select.append("NULL AS m$i, ")
            }

            select.append("COUNT(*) AS count")
        } else {
            select.append("*")
        }

        // 3. FROM & WHERE
        val where = StringBuilder(" FROM transactions WHERE profileId = ? AND status != 'DELETED'")
        args.add(profileId)

        // Resolve timelineRange to timestamp bounds
        val (effectiveStartDate, effectiveEndDate) = TimelineService.getDateRange(filter.timelineRange)

        if (effectiveStartDate != null) {
            where.append(" AND timestamp >= ?")
            args.add(effectiveStartDate)
        }
        if (effectiveEndDate != null) {
            where.append(" AND timestamp <= ?")
            args.add(effectiveEndDate)
        }
        if (filter.accountIds.isNotEmpty()) {
            val placeholders = filter.accountIds.joinToString(",") { "?" }
            where.append(" AND accountId IN ($placeholders)")
            args.addAll(filter.accountIds)
        }
        if (filter.categoryIds.isNotEmpty()) {
            val placeholders = filter.categoryIds.joinToString(",") { "?" }
            where.append(" AND categoryId IN ($placeholders)")
            args.addAll(filter.categoryIds)
        }
        if (filter.transactionTypes.isNotEmpty()) {
            val placeholders = filter.transactionTypes.joinToString(",") { "?" }
            where.append(" AND transactionType IN ($placeholders)")
            args.addAll(filter.transactionTypes.map { it.value })
        }
        if (filter.currencies.isNotEmpty()) {
            val placeholders = filter.currencies.joinToString(",") { "?" }
            where.append(" AND currency IN ($placeholders)")
            args.addAll(filter.currencies)
        }

        // Amount Filters
        if (filter.minAmount != null) {
            where.append(" AND amount >= ?")
            args.add(filter.minAmount)
        }
        if (filter.maxAmount != null) {
            where.append(" AND amount <= ?")
            args.add(filter.maxAmount)
        }

        if (filter.paymentMethods.isNotEmpty()) {
            val placeholders = filter.paymentMethods.joinToString(",") { "?" }
            where.append(" AND paymentMethod IN ($placeholders)")
            args.addAll(filter.paymentMethods)
        }

        // Warranty Filters
        if (filter.hasWarranty == true) {
            where.append(" AND warrantyExpiration IS NOT NULL")
        } else if (filter.hasWarranty == false) {
            where.append(" AND warrantyExpiration IS NULL")
        }
        if (filter.warrantyExpirationStart != null) {
            where.append(" AND warrantyExpiration >= ?")
            args.add(filter.warrantyExpirationStart)
        }
        if (filter.warrantyExpirationEnd != null) {
            where.append(" AND warrantyExpiration <= ?")
            args.add(filter.warrantyExpirationEnd)
        }

        if (filter.tags.isNotEmpty()) {
            where.append(" AND (")
            filter.tags.forEachIndexed { i, tag ->
                if (i > 0) where.append(" OR ")
                where.append("tags LIKE ?")
                args.add("%$tag%")
            }
            where.append(")")
        }

        // Text & Status Filters
        if (!filter.queryNote.isNullOrBlank()) {
            where.append(" AND note LIKE ?")
            args.add("%${filter.queryNote}%")
        }
        if (!filter.queryPayee.isNullOrBlank()) {
            where.append(" AND payee LIKE ?")
            args.add("%${filter.queryPayee}%")
        }
        if (filter.statuses.isNotEmpty()) {
            val placeholders = filter.statuses.joinToString(",") { "?" }
            where.append(" AND status IN ($placeholders)")
            args.addAll(filter.statuses.map { it.name })
        }

        // 4. Combine
        var finalQuery = select.toString() + where.toString()

        if (plan != null && plan.sqlDimensions.isNotEmpty()) {
            finalQuery += " " + group.removeSuffix(", ")
        } else if (plan == null) {
            finalQuery += " ORDER BY timestamp DESC"
            if (limit != null) {
                finalQuery += " LIMIT $limit"
            }
        }

        return SimpleSQLiteQuery(finalQuery, args.toTypedArray())
    }
}
