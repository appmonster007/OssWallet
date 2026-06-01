package com.osscabinet.ossWallet.data.web.routes

import com.osscabinet.ossWallet.data.web.AuthSessionManager
import com.osscabinet.ossWallet.data.web.authenticate
import com.osscabinet.ossWallet.data.web.dto.*
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

fun Route.analyticsRoutes(
    authSessionManager: AuthSessionManager,
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    settingsRepository: SettingsRepository
) {
    route("/analytics") {
        get("/category-breakdown") {
            if (!authenticate(authSessionManager)) return@get
            try {
                val profileId = settingsRepository.currentProfileId.first()
                val dateFrom = call.request.queryParameters["dateFrom"]?.toLongOrNull()
                val dateTo = call.request.queryParameters["dateTo"]?.toLongOrNull()
                val typeParam =
                    call.request.queryParameters["type"] ?: "EXPENSE"

                val allTransactions =
                    transactionRepository.getAllTransactionsSync(profileId)
                val filtered = allTransactions.filter { tx ->
                    (dateFrom == null || tx.timestamp >= dateFrom) &&
                            (dateTo == null || tx.timestamp <= dateTo) &&
                            tx.transactionType.name.equals(typeParam, ignoreCase = true)
                }

                val categoryMap =
                    categoryRepository.getAllCategoriesSync().associateBy { it.id }
                val totals = filtered.groupBy { it.categoryId }
                    .mapValues { (_, txs) ->
                        txs.sumOf { it.amount.toDouble() }
                    }

                val total = totals.values.sum()
                val dtos = totals.map { (catId, amount) ->
                    val cat = categoryMap[catId]
                    CategoryBreakdownDto(
                        categoryId = catId,
                        categoryName = cat?.name ?: "Unknown",
                        amount = amount,
                        percentage = if (total > 0) (amount / total * 100.0).toBigDecimal()
                            .setScale(2, RoundingMode.HALF_UP).toDouble() else 0.0,
                        transactionCount = filtered.count { it.categoryId == catId },
                        colorHex = if (cat != null) "#${(cat.colorHex and 0xFFFFFF).toString(16).padStart(6, '0')}" else "#CCCCCC"
                    )
                }.sortedByDescending { it.amount }

                call.respond(ApiResponse(success = true, data = dtos))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Unit>(
                        success = false,
                        error = "Internal error"
                    )
                )
            }
        }

        get("/cash-flow") {
            if (!authenticate(authSessionManager)) return@get
            try {
                val profileId = settingsRepository.currentProfileId.first()
                val dateFrom = call.request.queryParameters["dateFrom"]?.toLongOrNull()
                val dateTo = call.request.queryParameters["dateTo"]?.toLongOrNull()

                val allTransactions =
                    transactionRepository.getAllTransactionsSync(profileId)
                val filtered = allTransactions.filter { tx ->
                    (dateFrom == null || tx.timestamp >= dateFrom) &&
                            (dateTo == null || tx.timestamp <= dateTo)
                }

                val granularity =
                    call.request.queryParameters["granularity"] ?: "MONTH"
                val points = filtered.groupBy { tx ->
                    val cal = Calendar.getInstance().also { it.timeInMillis = tx.timestamp }
                    when (granularity.uppercase()) {
                        "DAY" -> "${cal.get(Calendar.YEAR)}-${
                            (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
                        }-${cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')}"
                        "WEEK" -> "${cal.get(Calendar.YEAR)}-W${cal.get(Calendar.WEEK_OF_YEAR)}"
                        else -> "${cal.get(Calendar.YEAR)}-${
                            (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
                        }"
                    }
                }.map { (label, txs) ->
                    val income = txs.filter { it.transactionType == TransactionType.INCOME }
                        .sumOf { it.amount.toDouble() }
                    val expense = txs.filter { it.transactionType == TransactionType.EXPENSE }
                        .sumOf { it.amount.toDouble() }
                    CashFlowPointDto(
                        label = label,
                        epochMillis = txs.minOf { it.timestamp },
                        income = income,
                        expense = expense,
                        net = income - expense
                    )
                }.sortedBy { it.epochMillis }

                call.respond(ApiResponse(success = true, data = points))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Unit>(
                        success = false,
                        error = "Internal error"
                    )
                )
            }
        }
    }
}
