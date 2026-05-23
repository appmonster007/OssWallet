package com.osscabinet.ossWallet.data.web.dto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
data class CardDataDto(
    val cardId: String,
    val cardType: String,
    val title: String,
    val order: Int,
    val isEnabled: Boolean,
    val filterSummary: FilterSummaryDto,
    val data: CardPayloadDto
)

@Serializable
data class FilterSummaryDto(
    val timelineType: String,
    val dateFrom: Long? = null,
    val dateTo: Long? = null,
    val accountIds: List<String> = emptyList(),
    val categoryIds: List<Long> = emptyList(),
    val transactionTypes: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val currencies: List<String> = emptyList(),
    val statuses: List<String> = emptyList(),
    val paymentMethods: List<String> = emptyList(),
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val queryNote: String? = null,
    val queryPayee: String? = null,
    val excludeFuture: Boolean = true
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class CardPayloadDto {
    @Serializable
    data class NetWorth(
        val netWorth: Double,
        val income: Double,
        val expense: Double,
        val cashFlow: Double,
        val currency: String
    ) : CardPayloadDto()

    @Serializable
    data class CashFlow(
        val income: Double,
        val expense: Double,
        val net: Double,
        val currency: String
    ) : CardPayloadDto()

    @Serializable
    data class CurrencyBalances(
        val items: List<CurrencyBalanceItem>,
        val baseCurrency: String
    ) : CardPayloadDto()

    @Serializable
    data class CurrencyBalanceItem(
        val currency: String,
        val balance: Double,
        val balanceInBase: Double
    )

    @Serializable
    data class Accounts(
        val accounts: List<AccountSummaryItem>
    ) : CardPayloadDto()

    @Serializable
    data class AccountSummaryItem(
        val id: String,
        val name: String,
        val currency: String,
        val balance: Double,
        val income: Double,
        val expense: Double
    )

    @Serializable
    data class Budgets(
        val items: List<BudgetDto>
    ) : CardPayloadDto()

    @Serializable
    data class PlannedPayments(
        val upcoming: List<PlannedPaymentDto>,
        val overdue: List<PlannedPaymentDto>
    ) : CardPayloadDto()

    @Serializable
    data class Loans(
        val items: List<LoanDto>
    ) : CardPayloadDto()

    @Serializable
    data class RecentActivities(
        val transactions: List<TransactionDto>,
        val total: Int
    ) : CardPayloadDto()

    @Serializable
    data class CategoryBreakdown(
        val items: List<CategoryBreakdownDto>,
        val displayCurrency: String
    ) : CardPayloadDto()

    @Serializable
    data class CashFlowComparison(
        val current: List<CashFlowPointDto>,
        val previous: List<CashFlowPointDto>,
        val lastYear: List<CashFlowPointDto>,
        val granularity: String,
        val baseCurrency: String
    ) : CardPayloadDto()

    @Serializable
    data class IncomeExpenseBar(
        val buckets: List<CashFlowPointDto>,
        val granularity: String,
        val baseCurrency: String
    ) : CardPayloadDto()

    @Serializable
    data class CategorySpendingTrend(
        val buckets: List<CategorySpendingBucketDto>,
        val granularity: String,
        val baseCurrency: String
    ) : CardPayloadDto()

    @Serializable
    data class CategorySpendingBucketDto(
        val label: String,
        val epochMillis: Long,
        val categories: List<CategoryAmountDto>
    )

    @Serializable
    data class CategoryAmountDto(
        val categoryId: Long,
        val categoryName: String,
        val amount: Double,
        val nature: String
    )

    @Serializable
    data class TagUpSetPlot(
        val sets: List<TagSetDto>,
        val intersections: List<TagIntersectionDto>,
        val baseCurrency: String
    ) : CardPayloadDto()

    @Serializable
    data class TagSetDto(
        val tagName: String,
        val totalAmount: Double,
        val income: Double,
        val expense: Double,
        val transactionCount: Int
    )

    @Serializable
    data class TagIntersectionDto(
        val tags: List<String>,
        val amount: Double,
        val income: Double,
        val expense: Double,
        val transactionCount: Int
    )

    @Serializable
    data class Error(val message: String) : CardPayloadDto()
}
