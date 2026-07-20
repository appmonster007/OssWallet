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

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class CardPayloadDto {
    @Serializable
    data class NetWorth(
        val netWorth: Double,
        val income: Double,
        val expense: Double,
        val transfer: Double = 0.0,
        val cashFlow: Double,
        val previousNetWorth: Double? = null,
        val previousIncome: Double? = null,
        val previousExpense: Double? = null,
        val previousTransfer: Double? = null,
        val currency: String,
        val fontSize: String = "Medium",
        val perspective: String = "Accounting",
        val showComparison: Boolean = false
    ) : CardPayloadDto()

    @Serializable
    data class CashFlow(
        val income: Double,
        val expense: Double,
        val transfer: Double = 0.0,
        val net: Double,
        val currency: String,
        val perspective: String = "Accounting",
        val showNetColumn: Boolean = false,
        val showBaseConversion: Boolean = false,
        val breakdown: List<CashFlowBreakdownItemDto> = emptyList()
    ) : CardPayloadDto()

    @Serializable
    data class CashFlowBreakdownItemDto(
        val currency: String,
        val income: Double,
        val incomeInBase: Double,
        val expense: Double,
        val expenseInBase: Double,
        val transfer: Double,
        val transferInBase: Double,
        val transferIn: Double,
        val transferInInBase: Double,
        val transferOut: Double,
        val transferOutInBase: Double
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
        val income: Double,
        val expense: Double,
        val transfer: Double = 0.0,
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
        val income: Double,
        val expense: Double,
        val transactionCount: Int,
        val colorHex: String? = null
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
    data class AccountHero(
        val accountId: String,
        val accountName: String,
        val currency: String,
        val balance: Double,
        val balanceInBase: Double,
        val colorHex: String,
        val iconName: String,
        val structureType: String,
        val income: Double,
        val expense: Double,
        val transferIn: Double,
        val transferOut: Double,
        val distribution: Map<String, Double> = emptyMap(),
        val showCurrencyBadges: Boolean = true,
        val showComparison: Boolean = false,
        val perspective: String = "Accounting"
    ) : CardPayloadDto()

    @Serializable
    data class Error(val message: String) : CardPayloadDto()
}
