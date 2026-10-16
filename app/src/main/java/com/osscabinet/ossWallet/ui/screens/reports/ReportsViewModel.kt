package com.osscabinet.ossWallet.ui.screens.reports

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.card.ToggleRule
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.isTransfer
import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TagRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import com.osscabinet.ossWallet.domain.service.report.ReportCategoryTotal
import com.osscabinet.ossWallet.domain.service.report.ReportCurrencyTotals
import com.osscabinet.ossWallet.domain.service.report.ReportData
import com.osscabinet.ossWallet.domain.service.report.ReportScope
import com.osscabinet.ossWallet.domain.service.report.ReportService
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** A generated report waiting to be shared or saved. */
data class ReportResult(
    val file: File,
    val format: ReportFormat,
    val rowCount: Int,
    val totals: List<ReportCurrencyTotals>
)

/** What the current filters would produce, shown before anything is generated. */
data class ReportPreview(
    val rowCount: Int,
    val totals: List<ReportCurrencyTotals>,
    val topCategories: List<ReportCategoryTotal>,
    /** e.g. "Jan 1 – Oct 16, 2026" */
    val rangeText: String
)

data class ReportsUiState(
    val accounts: List<Account> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val categories: List<Category> = emptyList(),
    /** The same filter model the Records screen uses, so every filter there works for reports too. */
    val filter: TransactionFilter = ReportsViewModel.DEFAULT_FILTER,
    val includeTransfers: Boolean = false,
    val format: ReportFormat = ReportFormat.PDF,
    val preview: ReportPreview? = null,
    val previewLoading: Boolean = true,
    val generating: Boolean = false,
    val result: ReportResult? = null,
    val message: String? = null
) {
    val isFiltered: Boolean get() = filter != ReportsViewModel.DEFAULT_FILTER || includeTransfers
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val tagRepository: TagRepository,
    private val exporter: ReportExporter,
    settingsRepository: SettingsRepository
) : BaseViewModel(settingsRepository) {

    private val _state = MutableStateFlow(ReportsUiState())
    val state: StateFlow<ReportsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.currentProfileId.flatMapLatest { accountRepository.getAllAccounts(it) }
                .collect { list -> _state.update { it.copy(accounts = list) } }
        }
        viewModelScope.launch {
            settingsRepository.currentProfileId.flatMapLatest { tagRepository.getAllTags(it) }
                .collect { list -> _state.update { it.copy(tags = list) } }
        }
        viewModelScope.launch {
            settingsRepository.currentProfileId.flatMapLatest { categoryRepository.getAllCategories(it) }
                .collect { list -> _state.update { it.copy(categories = list) } }
        }
        // Live preview: re-run the query whenever a filter changes, after a short pause so rapid taps coalesce.
        viewModelScope.launch {
            _state.map { it.filter to it.includeTransfers }
                .distinctUntilChanged()
                .onEach { _state.update { s -> s.copy(previewLoading = true) } }
                .debounce(250)
                .mapLatest { runCatching { buildData(_state.value, System.currentTimeMillis()) }.getOrNull() }
                .collect { data ->
                    _state.update { s ->
                        s.copy(
                            previewLoading = false,
                            preview = data?.let {
                                ReportPreview(it.rows.size, it.totals, it.topExpenseCategories, rangeText(s.filter.timelineRange))
                            }
                        )
                    }
                }
        }
    }

    // Any change to the options makes a previously generated file stale.
    private fun edit(transform: (ReportsUiState) -> ReportsUiState) =
        _state.update { transform(it).copy(result = null, message = null) }

    fun setFilter(filter: TransactionFilter) = edit { it.copy(filter = RULES.normalize(filter)) }
    fun resetFilters() = edit { it.copy(filter = DEFAULT_FILTER, includeTransfers = false) }
    fun setFormat(format: ReportFormat) = edit { it.copy(format = format) }
    fun setIncludeTransfers(include: Boolean) = edit { it.copy(includeTransfers = include) }

    /** Backs the note/payee search suggestions in the shared filter fields. */
    fun searchTransactions(query: String): Flow<List<Transaction>> =
        settingsRepository.currentProfileId.flatMapLatest { transactionRepository.searchTransactions(it, query) }

    fun saveTo(destinationUri: String) {
        val file = _state.value.result?.file ?: return
        viewModelScope.launch {
            val ok = exporter.saveTo(file, destinationUri)
            showMessage(if (ok) "Saved." else "Couldn't save the file.")
        }
    }

    fun showMessage(text: String?) = _state.update { it.copy(message = text) }

    fun generate() {
        val snapshot = _state.value
        if (snapshot.generating) return
        _state.update { it.copy(generating = true, result = null, message = null) }
        viewModelScope.launch {
            try {
                val data = buildData(snapshot, System.currentTimeMillis())
                if (data.isEmpty) {
                    _state.update { it.copy(message = "No transactions match these filters.") }
                } else {
                    val file = exporter.export(data, snapshot.format)
                    _state.update {
                        it.copy(result = ReportResult(file, snapshot.format, data.rows.size, data.totals))
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(message = "Couldn't create the report: ${e.message ?: "unknown error"}") }
            } finally {
                _state.update { it.copy(generating = false) }
            }
        }
    }

    private suspend fun buildData(snapshot: ReportsUiState, now: Long): ReportData {
        val profileId = settingsRepository.currentProfileId.first()
        val filter = snapshot.filter
        val transactions = transactionRepository.getFilteredTransactionsSync(profileId, filter)
        return ReportService.build(
            transactions = transactions,
            categories = snapshot.categories.ifEmpty { categoryRepository.getAllCategories(profileId).first() },
            accounts = accountRepository.getAllAccountsSync(profileId),
            scope = scopeFor(snapshot),
            generatedAt = now
        )
    }

    private fun scopeFor(s: ReportsUiState): ReportScope {
        val f = s.filter
        val types = f.transactionTypes
        return ReportScope(
            title = "Transaction report",
            periodLabel = f.timelineRange.toDisplayString(),
            periodRangeText = rangeText(f.timelineRange),
            accountsLabel = s.accounts.filter { it.id in f.accountIds }.joinToString { it.name }.ifEmpty { "All accounts" },
            categoriesLabel = s.categories.filter { it.id in f.categoryIds }.joinToString { it.name }.ifEmpty { "All categories" },
            tagsLabel = f.tags.sorted().joinToString().ifEmpty { "All tags" },
            typesLabel = types.joinToString { it.displayName }.ifEmpty { "Income and expenses" },
            includesTransfers = s.includeTransfers || types.any { it.isTransfer },
            otherFiltersLabel = otherFilters(f)
        )
    }

    /** Filters beyond the main ones, spelled out so a saved report says how it was narrowed. */
    private fun otherFilters(f: TransactionFilter): String = buildList {
        f.queryNote?.takeIf { it.isNotBlank() }?.let { add("note contains \"$it\"") }
        f.queryPayee?.takeIf { it.isNotBlank() }?.let { add("payee contains \"$it\"") }
        if (f.minAmount != null || f.maxAmount != null) {
            add("amount ${f.minAmount?.toPlainString() ?: "any"} to ${f.maxAmount?.toPlainString() ?: "any"}")
        }
        if (f.currencies.isNotEmpty()) add("currency ${f.currencies.sorted().joinToString()}")
        if (f.statuses.isNotEmpty()) add("status ${f.statuses.joinToString { it.displayName }}")
        if (f.paymentMethods.isNotEmpty()) add("paid by ${f.paymentMethods.joinToString { it.displayName }}")
        if (f.tags.size > 1) add(if (f.matchAllTags) "all selected tags" else "any selected tag")
    }.joinToString("; ")

    private fun rangeText(range: TimelineRange): String {
        val (start, end) = TimelineService.getDateRange(range)
        if (start == null && end == null) return "All time"
        val zone = ZoneId.systemDefault()
        val endMs = end ?: System.currentTimeMillis()
        val endText = DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(zone).format(Instant.ofEpochMilli(endMs))
        if (start == null) return "Up to $endText"
        val sameYear = Instant.ofEpochMilli(start).atZone(zone).year == Instant.ofEpochMilli(endMs).atZone(zone).year
        val startText = DateTimeFormatter.ofPattern(if (sameYear) "MMM d" else "MMM d, yyyy").withZone(zone)
            .format(Instant.ofEpochMilli(start))
        return "$startText – $endText"
    }

    companion object {
        val DEFAULT_FILTER = TransactionFilter(timelineRange = TimelineRange.ThisPeriod(ThisPeriodType.MONTH))

        /** Warranty and linked-record filters don't mean anything in a statement-style report. */
        val RULES = CardFilterRules(
            warrantyRule = ToggleRule.Disabled,
            linkedRecordsRule = ToggleRule.Disabled
        )
    }
}
