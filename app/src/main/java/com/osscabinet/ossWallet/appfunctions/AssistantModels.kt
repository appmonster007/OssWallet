package com.osscabinet.ossWallet.appfunctions

import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionUriGrant
import java.time.LocalDateTime

// Everything an assistant sends or receives. Amounts travel as text ("12.50") so no precision is lost, and
// choices (type, period, grouping) travel as words, because these calls can't carry enums.

/** One of the user's accounts. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AccountInfo(
    /** The account's name, as the user knows it. Use this exact name when asking for an account. */
    val name: String,
    /** The currency the account is kept in, as a three-letter code. */
    val currency: String
)

/** The user's accounts. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AccountList(
    /** The accounts that can be used for new records. */
    val accounts: List<AccountInfo>
)

/** One category. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class CategoryInfo(
    /** The category's name. Use this exact name when asking for a category. */
    val name: String,
    /** The name of the category it sits under, if any. */
    val parent: String?
)

/** The user's categories. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class CategoryList(
    /** The categories that can be used for new records. */
    val categories: List<CategoryInfo>
)

/** A filter the user saved in the app. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class SavedFilterInfo(
    /** The filter's name. Use this exact name to apply it. */
    val name: String,
    /** What the filter selects, in a short line. */
    val description: String
)

/** The user's saved filters. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class SavedFilterList(
    /** The saved filters of the current profile. */
    val filters: List<SavedFilterInfo>
)

/** What to record as a new transaction. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class CreateTransactionParams(
    /** The amount as a positive number with at most two decimals, for example "12.50". The sign comes from [type]. */
    val amount: String,
    /** "expense" or "income". Defaults to expense. */
    val type: String?,
    /** The account's name. Optional only if the user has a single account. */
    val accountName: String?,
    /** The category's name. Defaults to an uncategorised record. */
    val categoryName: String?,
    /** Who was paid or who paid, such as a shop or an employer. */
    val payee: String?,
    /** A short note about the record. */
    val note: String?,
    /** Tags to attach. Missing tags are created. */
    val tags: List<String>,
    /** When it happened, in the user's local time. Defaults to now. */
    val dateTime: LocalDateTime?
)

/** The record that was created. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class CreatedTransaction(
    /** The new transaction's id. */
    val id: Long,
    /** A one-line description of what was recorded. */
    val summary: String,
    /** True if an identical record had just been created, so nothing new was added. */
    val alreadyExisted: Boolean
)

/** What to look for among the user's transactions. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class SearchTransactionsParams(
    /**
     * Words to look for in notes, payees, names and categories. Also understands "#tag" for a tag, "49.99" for an
     * exact amount, ">100" or "<50" for amounts over or under, and "20-50" for a range. Leave empty to list by
     * the other filters alone.
     */
    val query: String?,
    /** One of: this month, last month, last 30 days, last 3 months, this year, last year, all time. */
    val period: String?,
    /** The name of one of the user's saved filters to apply. */
    val savedFilterName: String?,
    /** Only transactions in this account. */
    val accountName: String?,
    /** "expense" or "income" to narrow by direction. */
    val type: String?,
    /** How many transactions to return, at most 50. Defaults to 20. */
    val limit: Int
)

/** One transaction in a result. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class TransactionItem(
    /** The transaction's id. */
    val id: Long,
    /** The date and time, as ISO text in the user's local time. */
    val date: String,
    /** The payee, or the transaction's name when there is none. */
    val title: String,
    /** The category name. */
    val category: String?,
    /** The account name. */
    val account: String?,
    /** The amount: negative for spending, positive for income. */
    val amount: String,
    /** The currency code. */
    val currency: String,
    /** "expense", "income" or "transfer". */
    val type: String,
    /** The note, if any. */
    val note: String?,
    /** The tags. */
    val tags: List<String>
)

/** The matching transactions, newest first. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class TransactionSearchResult(
    /** How many transactions match in all (capped at 1000 for text searches). */
    val totalMatches: Int,
    /** The matching transactions, newest first, up to the limit. */
    val transactions: List<TransactionItem>
)

/** What to analyse. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class StatisticsParams(
    /** One of: this month, last month, last 30 days, last 3 months, this year, last year, all time. Defaults to this month. */
    val period: String?,
    /** The name of a saved filter to analyse. Its own date range is used unless a period is also given. */
    val savedFilterName: String?,
    /** Only this account. */
    val accountName: String?,
    /** What to break spending down by: "category" (default), "account", "tag" or "payment method". */
    val groupBy: String?,
    /** Whether to also draw a chart image of spending and income over time. */
    val includeChart: Boolean
)

/** Income and spending in one currency. Different currencies are never added together. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class CurrencySummary(
    /** The currency code. */
    val currency: String,
    /** Total income. */
    val income: String,
    /** Total spending, as a positive number. */
    val expense: String,
    /** Income minus spending. */
    val net: String,
    /** How many transactions it covers. */
    val transactionCount: Int,
    /** Income in the previous period, when there is one to compare with. */
    val previousIncome: String?,
    /** Spending in the previous period. */
    val previousExpense: String?,
    /** How spending changed against the previous period, in percent (negative means less). */
    val expenseChangePercent: String?
)

/** One slice of spending. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class BreakdownItem(
    /** The currency code. */
    val currency: String,
    /** The category, account, tag or payment method. */
    val label: String,
    /** The amount spent, as a positive number. */
    val amount: String,
    /** Share of that currency's total spending, in percent. */
    val percent: String
)

/** Income and spending in one stretch of time. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class TrendPoint(
    /** The day, month or year this covers, such as "2026-10". */
    val label: String,
    /** The currency code. */
    val currency: String,
    /** Income in that stretch. */
    val income: String,
    /** Spending in that stretch, as a positive number. */
    val expense: String
)

/** The analysis of a period or saved filter. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class StatisticsResult(
    /** What was analysed, in words. */
    val description: String,
    /** The first day covered, or empty for all time. */
    val from: String,
    /** The last day covered, or empty for all time. */
    val to: String,
    /** Totals per currency, with a comparison to the previous period where there is one. */
    val summaries: List<CurrencySummary>,
    /** Spending split by the requested grouping, largest first. */
    val breakdown: List<BreakdownItem>,
    /** Income and spending over time. */
    val trend: List<TrendPoint>,
    /** What the comparison period was, if any. */
    val comparedWith: String?,
    /** A chart image of the trend, when asked for. */
    val chart: AppFunctionUriGrant?
)
