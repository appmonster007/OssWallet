package com.osscabinet.ossWallet.ui.screens.search

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.BudgetRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.FilterRepository
import com.osscabinet.ossWallet.domain.repository.GoalRepository
import com.osscabinet.ossWallet.domain.repository.LoanRepository
import com.osscabinet.ossWallet.domain.repository.PlannedPaymentRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TagRepository
import com.osscabinet.ossWallet.domain.repository.TemplateRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import com.osscabinet.ossWallet.domain.service.filter.TransactionFilterService
import com.osscabinet.ossWallet.domain.service.search.GlobalSearchResults
import com.osscabinet.ossWallet.domain.service.search.GlobalSearchService
import com.osscabinet.ossWallet.domain.service.search.SearchData
import com.osscabinet.ossWallet.domain.service.search.SearchQueryParser
import com.osscabinet.ossWallet.domain.service.search.SearchSection
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** How far back a transaction search reaches. */
enum class SearchPeriod(val label: String) {
    ANY("Any time"), THIS_MONTH("This month"), LAST_30_DAYS("Last 30 days"), THIS_YEAR("This year");

    fun toRange(): TimelineRange = when (this) {
        // Search finds scheduled, future-dated entries too, unlike the Records default.
        ANY -> TimelineRange.AllTime(excludeFuture = false)
        THIS_MONTH -> TimelineRange.ThisPeriod(ThisPeriodType.MONTH)
        LAST_30_DAYS -> TimelineRange.LastPeriod(LastPeriodType.LAST_30_DAYS)
        THIS_YEAR -> TimelineRange.ThisPeriod(ThisPeriodType.YEAR)
    }
}

enum class SearchKind(val label: String, val types: Set<TransactionType>) {
    ALL("All", emptySet()),
    EXPENSES("Expenses", setOf(TransactionType.EXPENSE)),
    INCOME("Income", setOf(TransactionType.INCOME))
}

/** The one-tap chips under the search box. */
data class SearchChips(
    val period: SearchPeriod = SearchPeriod.ANY,
    val kind: SearchKind = SearchKind.ALL,
    val accountIds: Set<String> = emptySet()
) {
    /** Chips only make sense for transactions, so any chip hides the other record types. */
    val active: Boolean get() = period != SearchPeriod.ANY || kind != SearchKind.ALL || accountIds.isNotEmpty()
}

data class GlobalSearchUiState(
    val query: String = "",
    val chips: SearchChips = SearchChips(),
    val accounts: List<Account> = emptyList(),
    /** Null until there's something to search for. */
    val results: GlobalSearchResults? = null,
    val searching: Boolean = false,
    val message: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class GlobalSearchViewModel @Inject constructor(
    private val transactions: TransactionRepository,
    private val accounts: AccountRepository,
    private val categories: CategoryRepository,
    private val tags: TagRepository,
    private val loans: LoanRepository,
    private val budgets: BudgetRepository,
    private val goals: GoalRepository,
    private val templates: TemplateRepository,
    private val plannedPayments: PlannedPaymentRepository,
    private val filters: FilterRepository,
    settingsRepository: SettingsRepository
) : BaseViewModel(settingsRepository) {

    private val _state = MutableStateFlow(GlobalSearchUiState())
    val state: StateFlow<GlobalSearchUiState> = _state.asStateFlow()

    private var cachedData: SearchData? = null

    init {
        viewModelScope.launch {
            val profileId = settingsRepository.currentProfileId.first()
            _state.update { it.copy(accounts = accounts.getAllAccountsSync(profileId).filter { a -> !a.isArchived }) }
        }
        viewModelScope.launch {
            combine(_state.map { it.query }.distinctUntilChanged(), _state.map { it.chips }.distinctUntilChanged()) { q, c -> q to c }
                .debounce(200)
                .mapLatest { (query, chips) -> run(query, chips) }
                .collect { results -> _state.update { it.copy(results = results, searching = false) } }
        }
    }

    fun setQuery(query: String) = _state.update { it.copy(query = query, searching = query.isNotBlank(), message = null) }
    fun setPeriod(period: SearchPeriod) = _state.update { it.copy(chips = it.chips.copy(period = period), searching = true) }
    fun setKind(kind: SearchKind) = _state.update { it.copy(chips = it.chips.copy(kind = kind), searching = true) }
    fun toggleAccount(id: String) = _state.update {
        val ids = if (id in it.chips.accountIds) it.chips.accountIds - id else it.chips.accountIds + id
        it.copy(chips = it.chips.copy(accountIds = ids), searching = true)
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    private suspend fun loadData(profileId: Int): SearchData = cachedData ?: SearchData(
        accounts = accounts.getAllAccountsSync(profileId),
        categories = categories.getAllCategoriesSync(),
        tags = tags.getAllTags(profileId).first(),
        loans = loans.getAllLoans(profileId).first(),
        budgets = budgets.getActiveBudgetsSync(profileId),
        goals = goals.getAllGoalsSync(profileId),
        templates = templates.getAllTemplatesSync(profileId),
        plannedPayments = plannedPayments.getAllSync(profileId)
    ).also { cachedData = it }

    private suspend fun run(query: String, chips: SearchChips): GlobalSearchResults? {
        val parsed = SearchQueryParser.parse(query)
        if (parsed.isEmpty) return null
        val profileId = settingsRepository.currentProfileId.first()
        val data = loadData(profileId)

        val chipFilter = TransactionFilter(
            timelineRange = chips.period.toRange(),
            transactionTypes = chips.kind.types,
            accountIds = chips.accountIds
        )
        val matched = transactions.searchForGlobal(profileId, parsed, CANDIDATE_LIMIT)
            .filter { GlobalSearchService.matchesTransaction(parsed, it) }
            .let { if (chips.active) TransactionFilterService.filterTransactions(it, chipFilter) else it }

        val accountNames = data.accounts.associate { it.id to it.name }
        val txHits = GlobalSearchService.transactionHits(
            matched.take(TRANSACTION_LIMIT), accountNames, data.categories.associateBy { it.id }
        )

        val hits = buildMap {
            if (txHits.isNotEmpty()) put(SearchSection.TRANSACTIONS, txHits)
            if (!chips.active) putAll(GlobalSearchService.searchRecords(parsed, data))
        }
        return GlobalSearchResults(parsed, hits, matched.size)
    }

    /** Saves what's on screen (text, #tags, amount, chips) as a reusable filter on Records. */
    fun saveAsFilter(name: String) {
        val s = _state.value
        val parsed = SearchQueryParser.parse(s.query)
        if (parsed.isEmpty || name.isBlank()) return
        viewModelScope.launch {
            // Free text becomes the note search; the rest map one-to-one.
            val filter = TransactionFilter(
                timelineRange = s.chips.period.toRange(),
                transactionTypes = s.chips.kind.types,
                accountIds = s.chips.accountIds,
                tags = parsed.tags.toSet(),
                matchAllTags = true,
                minAmount = parsed.amount?.min,
                maxAmount = parsed.amount?.max,
                queryNote = parsed.words.takeIf { it.isNotEmpty() && !parsed.numberOnly }?.joinToString(" ")
            )
            filters.insertFilter(Filter(name = name.trim(), filter = filter, profileId = settingsRepository.currentProfileId.first()))
            _state.update { it.copy(message = "Saved filter “${name.trim()}”") }
        }
    }

    /** What the screen would show in Records, for "open all in Records". */
    fun currentRecordsFilter(): TransactionFilter? {
        val s = _state.value
        val parsed = SearchQueryParser.parse(s.query)
        if (parsed.isEmpty) return null
        return TransactionFilter(
            timelineRange = s.chips.period.toRange(),
            transactionTypes = s.chips.kind.types,
            accountIds = s.chips.accountIds,
            tags = parsed.tags.toSet(),
            minAmount = parsed.amount?.min,
            maxAmount = parsed.amount?.max,
            queryNote = parsed.words.takeIf { it.isNotEmpty() && !parsed.numberOnly }?.joinToString(" ")
        )
    }

    private companion object {
        const val CANDIDATE_LIMIT = 1000
        const val TRANSACTION_LIMIT = 50
    }
}
