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
data class AnalyticsResponseDto(
    val cards: List<CardDataDto>,
    val commonFilterEnabled: Boolean
)

@Serializable
data class CardFilterUpdateDto(
    val timelineType: String? = null,
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

@Serializable
data class FinancialSummaryDto(
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val transferIn: Double = 0.0,
    val transferOut: Double = 0.0,
    val netFlow: Double = 0.0,
    val transfer: Double = 0.0,
    val inflow: Double = 0.0,
    val outflow: Double = 0.0,
    val transactionCount: Int = 0
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class CardPayloadDto {
    @Serializable
    data class NetWorth(
        val netWorth: Double,
        val current: FinancialSummaryDto,
        val previous: FinancialSummaryDto? = null,
        val currency: String,
        val fontSize: String = "Medium",
        val perspective: String = "Accounting",
        val showComparison: Boolean = false
    ) : CardPayloadDto()

    @Serializable
    data class CashFlow(
        val metrics: FinancialSummaryDto,
        val currency: String,
        val perspective: String = "Accounting",
        val showNetColumn: Boolean = false,
        val showBaseConversion: Boolean = false,
        val breakdown: List<CashFlowBreakdownItemDto> = emptyList()
    ) : CardPayloadDto()

    @Serializable
    data class CashFlowBreakdownItemDto(
        val currency: String,
        val metrics: FinancialSummaryDto,
        val metricsInBase: FinancialSummaryDto
    )

    @Serializable
    data class CurrencyBalances(
        val items: List<CurrencyBalanceItem>,
        val baseCurrency: String,
        val layout: String = "grid",
        val gridColumns: Int = 2,
        val showBaseConversion: Boolean = true
    ) : CardPayloadDto()

    @Serializable
    data class CurrencyBalanceItem(
        val currency: String,
        val balance: Double,
        val balanceInBase: Double
    )

    @Serializable
    data class Accounts(
        val accounts: List<AccountSummaryItem>,
        val layout: String = "grid",
        val gridColumns: Int = 2,
        val showAddAccountCard: Boolean = false,
        val perspective: String = "Accounting",
        val excludeInternalTransfers: Boolean = false
    ) : CardPayloadDto()

    @Serializable
    data class AccountSummaryItem(
        val id: String,
        val name: String,
        val currency: String,
        val balance: Double,
        val metrics: FinancialSummaryDto,
        val balanceInBase: Double? = null
    )

    @Serializable
    data class Budgets(
        val items: List<BudgetDto>,
        val showArchived: Boolean = false,
        val maxItems: Int = 5
    ) : CardPayloadDto()

    @Serializable
    data class PlannedPayments(
        val upcoming: List<PlannedPaymentDto>,
        val overdue: List<PlannedPaymentDto>,
        val viewMode: String = "BOTH",
        val maxItems: Int = 10
    ) : CardPayloadDto()

    @Serializable
    data class Loans(
        val items: List<LoanDto>,
        val showType: String = "ALL",
        val maxItems: Int = 5
    ) : CardPayloadDto()

    @Serializable
    data class RecentActivities(
        val transactions: List<TransactionDto>,
        val total: Int,
        val groups: List<TransactionGroupDto> = emptyList(),
        val groupBy: String = "BY_DAY",
        val showAccount: Boolean = true,
        val showCategory: Boolean = true,
        val showNote: Boolean = true,
        val showTags: Boolean = true,
        val showTimestamp: Boolean = false,
        val showIcon: Boolean = true,
        val showOpeningBalance: Boolean = false,
        val showClosingBalance: Boolean = false
    ) : CardPayloadDto()

    @Serializable
    data class TransactionGroupDto(
        val label: String,
        val transactions: List<TransactionDto>
    )

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
        val baseCurrency: String,
        val isCumulative: Boolean = false,
        val showGridLines: Boolean = true,
        val showCurrent: Boolean = true,
        val showPrevious: Boolean = true,
        val showLastYear: Boolean = false,
        val selectedMetric: String = "NET"
    ) : CardPayloadDto()

    @Serializable
    data class IncomeExpenseBar(
        val buckets: List<CashFlowPointDto>,
        val granularity: String,
        val baseCurrency: String,
        val todayBucketIndex: Int = -1,
        val isCumulative: Boolean = false,
        val showGridLines: Boolean = true
    ) : CardPayloadDto()

    @Serializable
    data class CategorySpendingTrend(
        val buckets: List<CategorySpendingBucketDto>,
        val granularity: String,
        val baseCurrency: String,
        val isCumulative: Boolean = false,
        val showGridLines: Boolean = true,
        val selectedMetric: String = "EXPENSE",
        val selectedNatures: List<String> = listOf("MUST", "NEED", "WANT"),
        val activeCategories: List<CategoryBreakdownDto> = emptyList()
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
        val baseCurrency: String,
        val selectedMetric: String = "EXPENSE",
        val maxTags: Int = 7,
        val maxIntersections: Int = 10
    ) : CardPayloadDto()

    @Serializable
    data class TagSetDto(
        val tagName: String,
        val totalAmount: Double,
        val metrics: FinancialSummaryDto,
        val colorHex: String? = null
    )

    @Serializable
    data class TagIntersectionDto(
        val tags: List<String>,
        val amount: Double,
        val metrics: FinancialSummaryDto
    )

    @Serializable
    data class AccountHero(
        val accountId: String,
        val accountName: String,
        val currency: String,
        val balance: Double,
        val balanceInBase: Double,
        val colorHex: String,
        val iconName: String,
        val structureType: String,
        val metrics: FinancialSummaryDto,
        val distribution: Map<String, Double> = emptyMap(),
        val showCurrencyBadges: Boolean = true,
        val showComparison: Boolean = false,
        val perspective: String = "Accounting"
    ) : CardPayloadDto()

    @Serializable
    data class Error(val message: String) : CardPayloadDto()
}
