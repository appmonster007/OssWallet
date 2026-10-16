package com.osscabinet.ossWallet.ui.screens.reports

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TagRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import com.osscabinet.ossWallet.domain.service.report.ReportCategoryTotal
import com.osscabinet.ossWallet.domain.service.report.ReportCurrencyTotals
import com.osscabinet.ossWallet.domain.service.report.ReportData
import com.osscabinet.ossWallet.domain.service.report.ReportPeriod
import com.osscabinet.ossWallet.domain.service.report.ReportScope
import com.osscabinet.ossWallet.domain.service.report.ReportService
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
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

/** Which kinds of transactions the report lists. */
enum class ReportKind(val label: String, val types: Set<TransactionType>) {
    ALL("All", emptySet()),
    EXPENSES("Expenses", setOf(TransactionType.EXPENSE)),
    INCOME("Income", setOf(TransactionType.INCOME))
}

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
    val selectedCategoryIds: Set<Long> = emptySet(),
    val kind: ReportKind = ReportKind.ALL,
    val preview: ReportPreview? = null,
    val previewLoading: Boolean = true,
    val period: ReportPeriod = ReportPeriod.ThisMonth,
    /** Empty means "all accounts". */
    val selectedAccountIds: Set<String> = emptySet(),
    /** Empty means "all tags"; otherwise a transaction matches if it has any of them. */
    val selectedTags: Set<String> = emptySet(),
    val includeTransfers: Boolean = false,
    val format: ReportFormat = ReportFormat.PDF,
    val generating: Boolean = false,
    val result: ReportResult? = null,
    val message: String? = null
)

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
                .collect { list ->
                    _state.update { s ->
                        val live = list.filter { !it.isArchived || it.id in s.selectedAccountIds }
                        s.copy(accounts = live, selectedAccountIds = s.selectedAccountIds.filter { id -> list.any { it.id == id } }.toSet())
                    }
                }
        }
        viewModelScope.launch {
            settingsRepository.currentProfileId.flatMapLatest { tagRepository.getAllTags(it) }
                .collect { list -> _state.update { it.copy(tags = list) } }
        }
        viewModelScope.launch {
            settingsRepository.currentProfileId.flatMapLatest { categoryRepository.getAllCategories(it) }
                .collect { list ->
                    // Transfers and opening balances aren't spending categories; they're governed by "Include transfers".
                    val usable = list.filter { !Category.isTransfer(it.id) }.sortedBy { it.name.lowercase() }
                    _state.update { s ->
                        s.copy(categories = usable, selectedCategoryIds = s.selectedCategoryIds.filter { id -> usable.any { it.id == id } }.toSet())
                    }
                }
        }
        // Live preview: re-run the query whenever a filter changes, after a short pause so rapid chip taps coalesce.
        viewModelScope.launch {
            _state.map { Criteria(it.period, it.selectedAccountIds, it.selectedCategoryIds, it.selectedTags, it.kind, it.includeTransfers) }
                .distinctUntilChanged()
                .onEach { _state.update { s -> s.copy(previewLoading = true) } }
                .debounce(250)
                .mapLatest { runCatching { buildData(_state.value, System.currentTimeMillis()) }.getOrNull() }
                .collect { data ->
                    _state.update { s ->
                        s.copy(
                            previewLoading = false,
                            preview = data?.let { ReportPreview(it.rows.size, it.totals, it.topExpenseCategories, rangeText(s.period)) }
                        )
                    }
                }
        }
    }

    private data class Criteria(
        val period: ReportPeriod,
        val accounts: Set<String>,
        val categories: Set<Long>,
        val tags: Set<String>,
        val kind: ReportKind,
        val transfers: Boolean
    )

    private fun rangeText(period: ReportPeriod): String {
        val zone = ZoneId.systemDefault()
        val range = period.range(System.currentTimeMillis(), zone)
        val sameYear = Instant.ofEpochMilli(range.first).atZone(zone).year == Instant.ofEpochMilli(range.last).atZone(zone).year
        val start = DateTimeFormatter.ofPattern(if (sameYear) "MMM d" else "MMM d, yyyy").withZone(zone)
        val end = DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(zone)
        return "${start.format(Instant.ofEpochMilli(range.first))} – ${end.format(Instant.ofEpochMilli(range.last))}"
    }

    private suspend fun buildData(snapshot: ReportsUiState, now: Long): ReportData {
        val profileId = settingsRepository.currentProfileId.first()
        val range = snapshot.period.range(now, ZoneId.systemDefault())
        val filter = TransactionFilter(
            timelineRange = TimelineRange.CustomRange(start = range.first, end = range.last),
            accountIds = snapshot.selectedAccountIds,
            categoryIds = snapshot.selectedCategoryIds,
            transactionTypes = snapshot.kind.types,
            tags = snapshot.selectedTags,
            matchAllTags = false
        )
        val transactions = transactionRepository.getFilteredTransactionsSync(profileId, filter)
        return ReportService.build(
            transactions = transactions,
            categories = categoryRepository.getAllCategories(profileId).first(),
            accounts = accountRepository.getAllAccountsSync(profileId),
            scope = ReportScope(
                title = "Transaction report",
                periodLabel = snapshot.period.label,
                periodRangeText = rangeText(snapshot.period),
                accountsLabel = snapshot.accounts.filter { it.id in snapshot.selectedAccountIds }
                    .joinToString { it.name }.ifEmpty { "All accounts" },
                tagsLabel = snapshot.selectedTags.sorted().joinToString().ifEmpty { "All tags" },
                includesTransfers = snapshot.includeTransfers,
                categoriesLabel = snapshot.categories.filter { it.id in snapshot.selectedCategoryIds }
                    .joinToString { it.name }.ifEmpty { "All categories" },
                typesLabel = when (snapshot.kind) {
                    ReportKind.ALL -> "Income and expenses"
                    ReportKind.EXPENSES -> "Expenses only"
                    ReportKind.INCOME -> "Income only"
                }
            ),
            generatedAt = now
        )
    }

    // Any change to the options makes a previously generated file stale.
    private fun edit(transform: (ReportsUiState) -> ReportsUiState) =
        _state.update { transform(it).copy(result = null, message = null) }

    fun setPeriod(period: ReportPeriod) = edit { it.copy(period = period) }
    fun setFormat(format: ReportFormat) = edit { it.copy(format = format) }
    fun setIncludeTransfers(include: Boolean) = edit { it.copy(includeTransfers = include) }
    fun clearAccounts() = edit { it.copy(selectedAccountIds = emptySet()) }
    fun clearTags() = edit { it.copy(selectedTags = emptySet()) }
    fun clearCategories() = edit { it.copy(selectedCategoryIds = emptySet()) }
    fun setKind(kind: ReportKind) = edit { it.copy(kind = kind) }
    fun toggleCategory(id: Long) = edit {
        it.copy(selectedCategoryIds = if (id in it.selectedCategoryIds) it.selectedCategoryIds - id else it.selectedCategoryIds + id)
    }

    fun resetFilters() = edit {
        it.copy(
            period = ReportPeriod.ThisMonth, selectedAccountIds = emptySet(), selectedCategoryIds = emptySet(),
            selectedTags = emptySet(), kind = ReportKind.ALL, includeTransfers = false
        )
    }
    fun toggleAccount(id: String) = edit {
        it.copy(selectedAccountIds = if (id in it.selectedAccountIds) it.selectedAccountIds - id else it.selectedAccountIds + id)
    }

    fun toggleTag(name: String) = edit {
        it.copy(selectedTags = if (name in it.selectedTags) it.selectedTags - name else it.selectedTags + name)
    }

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
}
