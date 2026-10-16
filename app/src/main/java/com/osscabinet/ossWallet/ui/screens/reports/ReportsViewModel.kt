package com.osscabinet.ossWallet.ui.screens.reports

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TagRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import com.osscabinet.ossWallet.domain.service.report.ReportCurrencyTotals
import com.osscabinet.ossWallet.domain.service.report.ReportPeriod
import com.osscabinet.ossWallet.domain.service.report.ReportScope
import com.osscabinet.ossWallet.domain.service.report.ReportService
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.ZoneId

/** A generated report waiting to be shared or saved. */
data class ReportResult(
    val file: File,
    val format: ReportFormat,
    val rowCount: Int,
    val totals: List<ReportCurrencyTotals>
)

data class ReportsUiState(
    val accounts: List<Account> = emptyList(),
    val tags: List<Tag> = emptyList(),
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

@OptIn(ExperimentalCoroutinesApi::class)
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
    }

    // Any change to the options makes a previously generated file stale.
    private fun edit(transform: (ReportsUiState) -> ReportsUiState) =
        _state.update { transform(it).copy(result = null, message = null) }

    fun setPeriod(period: ReportPeriod) = edit { it.copy(period = period) }
    fun setFormat(format: ReportFormat) = edit { it.copy(format = format) }
    fun setIncludeTransfers(include: Boolean) = edit { it.copy(includeTransfers = include) }
    fun clearAccounts() = edit { it.copy(selectedAccountIds = emptySet()) }
    fun clearTags() = edit { it.copy(selectedTags = emptySet()) }
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
                val profileId = settingsRepository.currentProfileId.first()
                val now = System.currentTimeMillis()
                val range = snapshot.period.range(now, ZoneId.systemDefault())
                val filter = TransactionFilter(
                    timelineRange = TimelineRange.CustomRange(start = range.first, end = range.last),
                    accountIds = snapshot.selectedAccountIds,
                    tags = snapshot.selectedTags,
                    matchAllTags = false
                )
                val transactions = transactionRepository.getFilteredTransactionsSync(profileId, filter)
                val data = ReportService.build(
                    transactions = transactions,
                    categories = categoryRepository.getAllCategories(profileId).first(),
                    accounts = accountRepository.getAllAccountsSync(profileId),
                    scope = ReportScope(
                        title = "Transaction report",
                        periodLabel = snapshot.period.label,
                        accountsLabel = snapshot.accounts.filter { it.id in snapshot.selectedAccountIds }
                            .joinToString { it.name }.ifEmpty { "All accounts" },
                        tagsLabel = snapshot.selectedTags.sorted().joinToString().ifEmpty { "All tags" },
                        includesTransfers = snapshot.includeTransfers
                    ),
                    generatedAt = now
                )
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
