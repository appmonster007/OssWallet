package com.osscabinet.ossWallet.appfunctions

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.dto.TransactionRequest
import com.osscabinet.ossWallet.domain.model.query.AggregationDimension
import com.osscabinet.ossWallet.domain.model.query.StandardMetrics
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.AssistantAccessRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.FilterRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TagRepository
import com.osscabinet.ossWallet.domain.service.aggregation.TransactionAggregationService
import com.osscabinet.ossWallet.domain.service.management.TransactionManager
import com.osscabinet.ossWallet.domain.service.query.DatabaseQueryService
import com.osscabinet.ossWallet.domain.service.search.FilterSummary
import com.osscabinet.ossWallet.domain.service.search.SearchQueryParser
import com.osscabinet.ossWallet.domain.service.search.TransactionSearcher
import com.osscabinet.ossWallet.domain.service.security.AppLockGate
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * What an AI assistant can do in OssWallet. Each call checks first that the user has allowed assistants and that
 * the app isn't locked. Nothing here loads whole tables: totals and breakdowns are computed in the database and
 * lists are capped, so a 500,000-record history costs the same as a small one, and only a small summary ever
 * leaves a call.
 */
@Singleton
class AssistantFunctions @Inject constructor(
    private val lock: AppLockGate,
    private val access: AssistantAccessRepository,
    private val settings: SettingsRepository,
    private val accounts: AccountRepository,
    private val categories: CategoryRepository,
    private val tags: TagRepository,
    private val filters: FilterRepository,
    private val queryService: DatabaseQueryService,
    private val searcher: TransactionSearcher,
    private val transactionManager: TransactionManager,
    private val chartRenderer: AssistantChartRenderer
) {
    /** The clock and zone, replaceable so behaviour that depends on "now" can be tested. */
    internal var clock: () -> Long = { System.currentTimeMillis() }
    internal var zone: ZoneId = ZoneId.systemDefault()

    /** Refuses unless the user has switched assistants on and the app is unlocked; returns the current profile. */
    private suspend fun gate(): Int {
        if (!access.isEnabled()) {
            throw AssistantException(AssistantException.Kind.DISABLED, "AI assistant access is turned off in OssWallet's Settings.")
        }
        if (lock.isLockedNow()) {
            throw AssistantException(AssistantException.Kind.DENIED, "OssWallet is locked. Ask the user to unlock the app, then try again.")
        }
        return settings.currentProfileId.first()
    }

    // ── Lookups ──────────────────────────────────────────────────────────────

    suspend fun listAccounts(): AccountList {
        val profile = gate()
        return AccountList(activeAccounts(profile).map { AccountInfo(it.name, it.currencyCode) })
    }

    suspend fun listCategories(): CategoryList {
        val profile = gate()
        val all = usableCategories(profile)
        val byId = all.associateBy { it.id }
        return CategoryList(all.sortedBy { it.name.lowercase() }.map { CategoryInfo(it.name, it.parentId?.let { id -> byId[id]?.name }) })
    }

    suspend fun listSavedFilters(): SavedFilterList {
        val profile = gate()
        return SavedFilterList(filters.getAllFilters(profile).first().map { SavedFilterInfo(it.name, FilterSummary.describe(it.filter)) })
    }

    private suspend fun activeAccounts(profile: Int) = accounts.getAllAccountsSync(profile).filter { !it.isArchived }

    /** Spending and income categories; the system ones for transfers and opening balances aren't meant for records. */
    private suspend fun usableCategories(profile: Int) = categories.getAllCategories(profile).first().filter { !Category.isTransfer(it.id) }

    private fun resolveAccount(list: List<Account>, wanted: String?): Account {
        if (wanted.isNullOrBlank()) {
            return list.singleOrNull()
                ?: throw AssistantException.invalid("accountName is needed. The accounts are: ${AssistantSupport.candidates(list.map { it.name })}")
        }
        return when (val m = AssistantSupport.match(list.map { it.name }, wanted)) {
            is AssistantSupport.Match.Found -> list.first { it.name == m.name }
            is AssistantSupport.Match.Ambiguous -> throw AssistantException.invalid("\"$wanted\" could be ${AssistantSupport.candidates(m.options)}")
            AssistantSupport.Match.None -> throw AssistantException.notFound("No account called \"$wanted\". The accounts are: ${AssistantSupport.candidates(list.map { it.name })}")
        }
    }

    private suspend fun resolveCategory(profile: Int, wanted: String?): Category {
        val all = usableCategories(profile)
        if (wanted.isNullOrBlank()) return all.firstOrNull { it.id == Category.SYSTEM_UNKNOWN } ?: all.first()
        return when (val m = AssistantSupport.match(all.map { it.name }, wanted)) {
            is AssistantSupport.Match.Found -> all.first { it.name == m.name }
            is AssistantSupport.Match.Ambiguous -> throw AssistantException.invalid("\"$wanted\" could be ${AssistantSupport.candidates(m.options)}")
            AssistantSupport.Match.None -> throw AssistantException.notFound("No category called \"$wanted\". Use listCategories to see them.")
        }
    }

    private suspend fun resolveSavedFilter(profile: Int, wanted: String): Filter {
        val all = filters.getAllFilters(profile).first()
        return when (val m = AssistantSupport.match(all.map { it.name }, wanted)) {
            is AssistantSupport.Match.Found -> all.first { it.name == m.name }
            is AssistantSupport.Match.Ambiguous -> throw AssistantException.invalid("\"$wanted\" could be ${AssistantSupport.candidates(m.options)}")
            AssistantSupport.Match.None -> throw AssistantException.notFound("No saved filter called \"$wanted\". Use listSavedFilters to see them.")
        }
    }

    // ── Create ───────────────────────────────────────────────────────────────

    suspend fun createTransaction(p: CreateTransactionParams): CreatedTransaction {
        val profile = gate()
        val mode = AssistantSupport.parseType(p.type)
        if (mode == EntryMode.TRANSFER) throw AssistantException.invalid("only expenses and income can be created")
        val amount = AssistantSupport.parseAmount(p.amount)
        val account = resolveAccount(activeAccounts(profile), p.accountName)
        val category = resolveCategory(profile, p.categoryName)
        val now = clock()
        val timestamp = AssistantSupport.resolveTime(p.dateTime, now, zone)
        val note = AssistantSupport.cleanNote(p.note)
        val payee = p.payee?.trim().orEmpty().also {
            if (it.length > 100) throw AssistantException.invalid("payee is longer than 100 characters")
        }
        val tagNames = ensureTags(profile, AssistantSupport.cleanTags(p.tags))

        val type = if (mode == EntryMode.INCOME) TransactionType.INCOME else TransactionType.EXPENSE
        findRecentDuplicate(profile, account.id, amount, type, payee, note, timestamp, atNow = p.dateTime == null)?.let {
            return CreatedTransaction(it.id, "Already recorded: ${describe(mode, amount, account, category, timestamp)}", alreadyExisted = true)
        }

        val request = TransactionRequest(
            name = payee.ifBlank { category.name },
            amount = amount,
            entryMode = mode,
            accountId = account.id,
            currency = account.currencyCode,
            categoryId = category.id,
            tags = tagNames.toSet(),
            timestamp = timestamp,
            payee = payee,
            note = note
        )
        val id = transactionManager.handleSaveTransaction(existingTransaction = null, request = request, profileId = profile)
        return CreatedTransaction(id, "Recorded: ${describe(mode, amount, account, category, timestamp)}", alreadyExisted = false)
    }

    private fun describe(mode: EntryMode, amount: java.math.BigDecimal, account: Account, category: Category, timestamp: Long) =
        "${mode.displayName.lowercase()} of ${AssistantSupport.money(amount, account.currencyCode)} ${account.currencyCode} " +
            "in ${account.name} (${category.name}) on ${LocalDate.ofInstant(Instant.ofEpochMilli(timestamp), zone)}"

    /** Reuses existing tags (matching ignoring case) and creates the rest, so a typo can't fork a tag. */
    private suspend fun ensureTags(profile: Int, wanted: List<String>): List<String> {
        if (wanted.isEmpty()) return emptyList()
        val existing = tags.getAllTags(profile).first().associateBy { it.name.lowercase() }
        return wanted.map { name ->
            existing[name.lowercase()]?.name ?: name.also { tags.insertTag(Tag(name = it, profileId = profile)) }
        }
    }

    /**
     * Assistants retry. If the same record (account, amount, direction, payee, note) was created moments ago, return it
     * instead of adding another. Without a given time, "moments" is the last two minutes; with one, the same minute.
     */
    private suspend fun findRecentDuplicate(
        profile: Int, accountId: String, amount: java.math.BigDecimal, type: TransactionType,
        payee: String, note: String, timestamp: Long, atNow: Boolean
    ): Transaction? {
        val window = if (atNow) (timestamp - 120_000)..(timestamp + 1_000) else (timestamp - 60_000)..(timestamp + 60_000)
        val filter = TransactionFilter(
            timelineRange = TimelineRange.CustomRange(window.first, window.last),
            accountIds = setOf(accountId)
        )
        return queryService.observeFilteredTransactions(profile, filter, limit = 20).first().firstOrNull {
            it.transactionType == type && it.amount.abs().compareTo(amount) == 0 &&
                it.payee.equals(payee, ignoreCase = true) && it.note == note
        }
    }

    // ── Search ───────────────────────────────────────────────────────────────

    suspend fun searchTransactions(p: SearchTransactionsParams): TransactionSearchResult {
        val profile = gate()
        val limit = AssistantSupport.clampLimit(p.limit)
        val parsed = SearchQueryParser.parse(p.query.orEmpty())
        val period = parsePeriod(p.period)
        val saved = p.savedFilterName?.takeIf { it.isNotBlank() }?.let { resolveSavedFilter(profile, it) }
        val account = p.accountName?.takeIf { it.isNotBlank() }?.let { resolveAccount(activeAccounts(profile), it) }
        val type = p.type?.takeIf { it.isNotBlank() }?.let { AssistantSupport.parseType(it) }

        // Searching isn't limited to what has happened yet, unlike statistics: scheduled entries can be looked up too.
        var filter = saved?.filter ?: TransactionFilter(timelineRange = TimelineRange.AllTime(excludeFuture = false))
        if (period != null) filter = filter.copy(timelineRange = rangeOf(period, excludeFuture = false))
        if (account != null) filter = filter.copy(accountIds = setOf(account.id))
        if (type != null) filter = filter.copy(transactionTypes = setOf(if (type == EntryMode.INCOME) TransactionType.INCOME else TransactionType.EXPENSE))

        val data = if (parsed.isEmpty) {
            val list = queryService.observeFilteredTransactions(profile, filter, limit).first()
            val total = runCatching {
                TransactionAggregationService.getSummaryByCurrency(queryService, profile, filter).getOrThrow().values.sumOf { it.transactionCount }
            }.getOrDefault(list.size)
            list to maxOf(total, list.size)
        } else {
            val matched = searcher.search(profile, parsed, filter)
            matched.take(limit) to matched.size
        }

        val accountNames = accounts.getAllAccountsSync(profile).associate { it.id to it.name }
        val categoryNames = categories.getAllCategories(profile).first().associate { it.id to it.name }
        return TransactionSearchResult(data.second, data.first.map { toItem(it, accountNames, categoryNames) })
    }

    private fun toItem(tx: Transaction, accountNames: Map<String, String>, categoryNames: Map<Long, String>) = TransactionItem(
        id = tx.id,
        date = AssistantSupport.isoLocal(tx.timestamp, zone),
        title = tx.payee.ifBlank { tx.name },
        category = categoryNames[tx.categoryId],
        account = tx.accountId?.let { accountNames[it] },
        amount = AssistantSupport.money(tx.amount, tx.currency),
        currency = tx.currency,
        type = when (tx.transactionType) {
            TransactionType.EXPENSE -> "expense"
            TransactionType.INCOME -> "income"
            else -> "transfer"
        },
        note = tx.note.takeIf { it.isNotBlank() },
        tags = tx.tags.sorted()
    )

    // ── Statistics ───────────────────────────────────────────────────────────

    suspend fun statistics(p: StatisticsParams): StatisticsResult {
        val profile = gate()
        val now = clock()
        val period = parsePeriod(p.period)
        val saved = p.savedFilterName?.takeIf { it.isNotBlank() }?.let { resolveSavedFilter(profile, it) }
        val account = p.accountName?.takeIf { it.isNotBlank() }?.let { resolveAccount(activeAccounts(profile), it) }
        val groupDimension = parseGroupBy(p.groupBy)

        // A named period wins; with only a saved filter its own dates stand; with neither, this month.
        val effective = period ?: if (saved == null) AssistantPeriod.THIS_MONTH else null
        var filter = saved?.filter ?: TransactionFilter()
        if (account != null) filter = filter.copy(accountIds = setOf(account.id))
        val range: LongRange?
        val previousRange: LongRange?
        if (effective != null) {
            range = effective.range(now, zone)
            previousRange = effective.previousRange(now, zone)
            filter = filter.copy(timelineRange = rangeOf(effective, excludeFuture = true))
        } else {
            val (start, end) = TimelineService.getDateRange(filter.timelineRange)
            range = start?.let { it..(end ?: now) }
            previousRange = null
        }

        // Statistics are about income and spending; transfers between accounts are neither, and leaving them out here
        // also keeps the transaction counts honest.
        val moneyTypes = setOf(TransactionType.INCOME, TransactionType.EXPENSE)
        filter = filter.copy(transactionTypes = filter.transactionTypes.intersect(moneyTypes).ifEmpty { moneyTypes })

        val totals = TransactionAggregationService.getSummaryByCurrency(queryService, profile, filter, excludeInternalTransfers = true).getOrThrow()
        val prior = previousRange?.let {
            TransactionAggregationService.getSummaryByCurrency(
                queryService, profile,
                filter.copy(timelineRange = TimelineRange.CustomRange(it.first, it.last, excludeFuture = true)),
                excludeInternalTransfers = true
            ).getOrThrow()
        }
        val slices = breakdownSlices(profile, filter, groupDimension)
        val byDay = TransactionAggregationService.getSummaryByCurrencyByDay(queryService, profile, filter, excludeInternalTransfers = true).getOrThrow()

        val summaries = AssistantStatistics.summaries(totals, prior)
        val breakdown = AssistantStatistics.breakdown(slices)
        val trend = AssistantStatistics.trend(byDay, AssistantStatistics.granularityFor(range))

        val whenText = effective?.label ?: FilterSummary.parts(filter).first().lowercase()
        val scope = when {
            saved != null -> " (saved filter \"${saved.name}\")"
            account != null -> " (account ${account.name})"
            else -> ""
        }
        return StatisticsResult(
            description = "Income and spending for $whenText$scope" + if (summaries.isEmpty()) ": no transactions found" else "",
            from = range?.let { LocalDate.ofInstant(Instant.ofEpochMilli(it.first), zone).toString() }.orEmpty(),
            to = range?.let { LocalDate.ofInstant(Instant.ofEpochMilli(minOf(it.last, now)), zone).toString() }.orEmpty(),
            summaries = summaries,
            breakdown = breakdown,
            trend = trend,
            comparedWith = previousRange?.let { "previous period (${LocalDate.ofInstant(Instant.ofEpochMilli(it.first), zone)} to ${LocalDate.ofInstant(Instant.ofEpochMilli(it.last), zone)})" },
            chart = if (p.includeChart) chartRenderer.render(trend, effective?.label ?: "selected filter") else null
        )
    }

    /** Spending slices for the requested grouping, using the app's existing aggregates where there is one. */
    private suspend fun breakdownSlices(profile: Int, filter: TransactionFilter, dimension: AggregationDimension): List<AssistantStatistics.Slice> {
        return when (dimension) {
            AggregationDimension.CATEGORY -> {
                val names = categories.getAllCategories(profile).first().associate { it.id to it.name }
                val byCategory = TransactionAggregationService.getSummaryByCategoryAndCurrency(queryService, profile, filter, excludeInternalTransfers = true).getOrThrow()
                AssistantStatistics.slices(byCategory.mapKeys { it.key.toString() }) { names[it.toLongOrNull()] ?: "Uncategorised" }
            }
            AggregationDimension.ACCOUNT -> {
                val names = accounts.getAllAccountsSync(profile).associate { it.id to it.name }
                val byAccount = TransactionAggregationService.getSummaryByAccountAndCurrency(queryService, profile, filter, excludeInternalTransfers = true).getOrThrow()
                AssistantStatistics.slices(byAccount) { names[it] ?: it.ifBlank { "Outside wallet" } }
            }
            // Tags and payment methods have no ready-made summary, so ask the same query service directly.
            else -> queryService.aggregate(
                profileId = profile,
                dimensions = listOf(AggregationDimension.CURRENCY, dimension),
                metrics = listOf(StandardMetrics.EXPENSE_SUM),
                filter = filter,
                excludeInternalTransfers = true
            ).getOrThrow().map { row ->
                val raw = row.dimensions[dimension].orEmpty()
                AssistantStatistics.Slice(
                    currency = row.dimensions[AggregationDimension.CURRENCY].orEmpty(),
                    label = if (dimension == AggregationDimension.PAYMENT_METHOD) raw.lowercase().replace('_', ' ') else raw.ifBlank { "Untagged" },
                    spent = (row.metrics["expense"] ?: java.math.BigDecimal.ZERO).abs()
                )
            }
        }
    }

    private fun parsePeriod(text: String?): AssistantPeriod? {
        if (text.isNullOrBlank()) return null
        return AssistantPeriod.parse(text) ?: throw AssistantException.invalid("period must be one of: ${AssistantPeriod.choices}")
    }

    private fun parseGroupBy(text: String?): AggregationDimension = when (text?.trim()?.lowercase()?.replace('_', ' ')) {
        null, "", "category", "categories" -> AggregationDimension.CATEGORY
        "account", "accounts" -> AggregationDimension.ACCOUNT
        "tag", "tags" -> AggregationDimension.TAGS
        "payment method", "payment methods", "method" -> AggregationDimension.PAYMENT_METHOD
        else -> throw AssistantException.invalid("groupBy must be category, account, tag or payment method")
    }

    private fun rangeOf(period: AssistantPeriod, excludeFuture: Boolean): TimelineRange {
        val range = period.range(clock(), zone) ?: return TimelineRange.AllTime(excludeFuture = excludeFuture)
        return TimelineRange.CustomRange(range.first, range.last, excludeFuture = excludeFuture)
    }
}
