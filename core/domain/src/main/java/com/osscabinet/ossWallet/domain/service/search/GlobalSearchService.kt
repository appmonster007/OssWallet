package com.osscabinet.ossWallet.domain.service.search

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Goal
import com.osscabinet.ossWallet.domain.model.Loan
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionType
import java.math.BigDecimal

enum class SearchSection(val title: String) {
    TRANSACTIONS("Transactions"),
    ACCOUNTS("Accounts"),
    CATEGORIES("Categories"),
    TAGS("Tags"),
    LOANS("Loans"),
    BUDGETS("Budgets"),
    GOALS("Goals"),
    TEMPLATES("Templates"),
    PLANNED_PAYMENTS("Planned payments")
}

/** One result row. [id] is the record's id as text (accounts use string ids); tags use their name. */
data class SearchHit(
    val section: SearchSection,
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val amount: BigDecimal? = null,
    val currency: String? = null,
    val type: TransactionType? = null,
    val timestamp: Long? = null,
    /** The record's icon name and colour as the rest of the app shows them (category, account, tag). */
    val iconName: String? = null,
    val colorArgb: Int? = null,
    /** A second line of detail, such as a transaction's note. */
    val detail: String? = null
)

/** Everything that isn't a transaction, which is small enough to hold in memory and match directly. */
data class SearchData(
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val loans: List<Loan> = emptyList(),
    val budgets: List<Budget> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val templates: List<Template> = emptyList(),
    val plannedPayments: List<PlannedPayment> = emptyList()
)

data class GlobalSearchResults(
    val parsed: ParsedSearch,
    val hits: Map<SearchSection, List<SearchHit>>,
    /** How many transactions matched in all; only the first few are listed. */
    val transactionTotal: Int
) {
    val isEmpty: Boolean get() = hits.values.all { it.isEmpty() }
    val sections: List<SearchSection> get() = SearchSection.entries.filter { !hits[it].isNullOrEmpty() }
}

/** Matching and shaping of search results; the database fetch for transactions happens elsewhere. */
object GlobalSearchService {

    /** Matches the parsed text against one record's searchable fields; every word must appear in some field. */
    private fun textMatches(parsed: ParsedSearch, fields: List<String?>): Boolean =
        parsed.words.all { word -> fields.any { it?.contains(word, ignoreCase = true) == true } }

    /**
     * Does a record with these fields and (optional) amount fit the query? A lone number counts as either a
     * name fragment or an amount; any other mix needs both text and amount to fit. Records that can't carry
     * tags are out as soon as the query asks for a `#tag`.
     */
    private fun qualifies(parsed: ParsedSearch, fields: List<String?>, amount: BigDecimal?, tags: Set<String>?): Boolean {
        if (parsed.isEmpty) return false
        if (parsed.tags.isNotEmpty()) {
            if (tags == null) return false
            val lower = tags.map { it.lowercase() }
            if (!parsed.tags.all { wanted -> lower.any { it.startsWith(wanted) } }) return false
        }
        val amountSpec = parsed.amount
        if (parsed.numberOnly) {
            val textHit = textMatches(parsed, fields)
            val amountHit = amount != null && amountSpec != null && amountSpec.matches(amount)
            return textHit || amountHit
        }
        if (amountSpec != null && (amount == null || !amountSpec.matches(amount))) return false
        return parsed.words.isEmpty() || textMatches(parsed, fields)
    }

    fun searchRecords(parsed: ParsedSearch, data: SearchData, limitPerSection: Int = 8): Map<SearchSection, List<SearchHit>> {
        if (parsed.isEmpty) return emptyMap()
        fun <T> List<T>.rank(name: (T) -> String) = sortedWith(
            compareByDescending<T> { item -> parsed.words.any { name(item).startsWith(it, ignoreCase = true) } }
                .thenBy { name(it).lowercase() }
        ).take(limitPerSection)

        val out = mutableMapOf<SearchSection, List<SearchHit>>()

        out[SearchSection.ACCOUNTS] = data.accounts
            .filter { qualifies(parsed, listOf(it.name, it.currencyCode), null, null) }
            .rank { it.name }
            .map {
                SearchHit(
                    SearchSection.ACCOUNTS, it.id, it.name, it.currencyCode + if (it.isArchived) " · archived" else "",
                    iconName = it.icon, colorArgb = parseColor(it.colorHex)
                )
            }

        out[SearchSection.CATEGORIES] = data.categories
            .filter { qualifies(parsed, listOf(it.name), null, null) }
            .rank { it.name }
            .map { SearchHit(SearchSection.CATEGORIES, it.id.toString(), it.name, iconName = it.iconName, colorArgb = it.colorHex) }

        out[SearchSection.TAGS] = data.tags
            .filter { tag ->
                // A `#holiday` entry finds the tag itself; plain words find it by name.
                if (parsed.tags.isNotEmpty() && parsed.words.isEmpty() && parsed.amount == null) {
                    parsed.tags.all { tag.name.startsWith(it, ignoreCase = true) }
                } else parsed.tags.isEmpty() && qualifies(parsed, listOf(tag.name), null, null)
            }
            .rank { it.name }
            .map { SearchHit(SearchSection.TAGS, it.name, "#${it.name}", colorArgb = it.colorHex.takeIf { c -> c != -1 }) }

        out[SearchSection.LOANS] = data.loans
            .filter { qualifies(parsed, listOf(it.name, it.lenderOrBorrowerName, it.note), it.principalAmount, null) }
            .rank { it.name }
            .map { SearchHit(SearchSection.LOANS, it.id.toString(), it.name, it.lenderOrBorrowerName, it.principalAmount, it.currency) }

        out[SearchSection.BUDGETS] = data.budgets
            .filter { qualifies(parsed, listOf(it.name), it.limitAmount, it.tags) }
            .rank { it.name }
            .map { SearchHit(SearchSection.BUDGETS, it.id.toString(), it.name, "Limit", it.limitAmount, it.currency) }

        out[SearchSection.GOALS] = data.goals
            .filter { qualifies(parsed, listOf(it.name, it.note), it.targetAmount, null) }
            .rank { it.name }
            .map { SearchHit(SearchSection.GOALS, it.id.toString(), it.name, "Target", it.targetAmount, it.currency) }

        out[SearchSection.TEMPLATES] = data.templates
            .filter { qualifies(parsed, listOf(it.name, it.payee, it.note), it.amount, it.tags) }
            .rank { it.name }
            .map { SearchHit(SearchSection.TEMPLATES, it.id.toString(), it.name, it.payee, it.amount, it.currency) }

        out[SearchSection.PLANNED_PAYMENTS] = data.plannedPayments
            .filter { qualifies(parsed, listOf(it.name, it.payee, it.note), it.amount, it.tags) }
            .rank { it.name }
            .map { SearchHit(SearchSection.PLANNED_PAYMENTS, it.id.toString(), it.name, it.payee, it.amount, it.currency) }

        return out.filterValues { it.isNotEmpty() }
    }

    /** Post-filter for database candidates: the `#tags` and amount parts, which SQL only approximates. */
    fun matchesTransaction(parsed: ParsedSearch, tx: Transaction): Boolean {
        if (parsed.tags.isNotEmpty()) {
            val lower = tx.tags.map { it.lowercase() }
            if (!parsed.tags.all { wanted -> lower.any { it.startsWith(wanted) } }) return false
        }
        val spec = parsed.amount
        // A lone number was also searched as text, so a text hit alone is enough there.
        if (spec != null && !parsed.numberOnly && !spec.matches(tx.amount)) return false
        return true
    }

    fun transactionHits(
        transactions: List<Transaction>,
        accountNames: Map<String, String>,
        categories: Map<Long, Category>
    ): List<SearchHit> = transactions.map { tx ->
        val category = categories[tx.categoryId]
        val where = listOfNotNull(category?.name, tx.accountId?.let { accountNames[it] })
        SearchHit(
            section = SearchSection.TRANSACTIONS,
            id = tx.id.toString(),
            title = tx.payee.ifBlank { tx.name },
            subtitle = where.joinToString(" · ").ifBlank { null },
            amount = tx.amount,
            currency = tx.currency,
            type = tx.transactionType,
            timestamp = tx.timestamp,
            iconName = category?.iconName,
            colorArgb = category?.colorHex,
            detail = tx.note.takeIf { it.isNotBlank() }
        )
    }

    /** "#4CAF50" -> an opaque ARGB int; null if it isn't a colour. */
    private fun parseColor(hex: String): Int? = runCatching {
        (hex.removePrefix("#").toLong(16).toInt()) or (0xFF shl 24)
    }.getOrNull()
}
