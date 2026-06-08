package com.osscabinet.ossWallet.ui.screens.import_csv

import android.content.Context
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.import_csv.CsvImportSession
import com.osscabinet.ossWallet.domain.model.import_csv.DuplicatePolicy
import com.osscabinet.ossWallet.domain.model.import_csv.EntityMapping
import com.osscabinet.ossWallet.domain.model.import_csv.ImportPreview
import com.osscabinet.ossWallet.domain.model.import_csv.ImportSummary
import com.osscabinet.ossWallet.domain.model.import_csv.OssWalletField
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.service.CsvImportService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CsvImportViewModel @Inject constructor(
    private val csvImportService: CsvImportService,
    private val settingsRepository: SettingsRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    sealed class Step {
        object ColumnMapping : Step()
        object EntityMapping : Step()
        object Preview : Step()
    }

    private val _step = MutableStateFlow<Step>(Step.ColumnMapping)
    val step: StateFlow<Step> = _step

    private val _session = MutableStateFlow<CsvImportSession?>(null)
    val session: StateFlow<CsvImportSession?> = _session

    private val _preview = MutableStateFlow<ImportPreview?>(null)
    val preview: StateFlow<ImportPreview?> = _preview

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _result = MutableStateFlow<ImportSummary?>(null)
    val result: StateFlow<ImportSummary?> = _result

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    val accounts: StateFlow<List<Account>> = settingsRepository.currentProfileId
        .flatMapLatest { profileId -> accountRepository.getAllAccounts(profileId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = categoryRepository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var pendingUri: String? = null
    private var entityMappings: List<EntityMapping> = emptyList()
    private var selectedDuplicates: Set<Int> = emptySet()
    private var duplicatePolicy: DuplicatePolicy = DuplicatePolicy.SKIP

    fun loadFile(uriString: String, context: Context) {
        pendingUri = uriString
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val stream = context.contentResolver.openInputStream(uriString.toUri())!!
                _session.value = csvImportService.parseHeaders(stream)
                _step.value = Step.ColumnMapping
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load file"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateMapping(field: OssWalletField, colIndex: Int?) {
        _session.update { it?.copy(mapping = it.mapping + (field to colIndex)) }
    }

    fun proceedFromColumnMapping(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val stream = context.contentResolver.openInputStream(pendingUri!!.toUri())!!
                val preview = csvImportService.generatePreview(session.value!!, stream)
                _preview.value = preview
                if (preview.newAccounts.isNotEmpty() || preview.newCategories.isNotEmpty()) {
                    _step.value = Step.EntityMapping
                } else {
                    _step.value = Step.Preview
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to generate preview"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setEntityMappings(mappings: List<EntityMapping>) {
        entityMappings = mappings
    }

    fun proceedFromEntityMapping() {
        _step.value = Step.Preview
    }

    fun setDuplicatePolicy(policy: DuplicatePolicy) {
        duplicatePolicy = policy
    }

    fun toggleDuplicateRow(rowIndex: Int) {
        selectedDuplicates = if (rowIndex in selectedDuplicates) {
            selectedDuplicates - rowIndex
        } else {
            selectedDuplicates + rowIndex
        }
    }

    fun executeImport(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val stream = context.contentResolver.openInputStream(pendingUri!!.toUri())!!
                val result = csvImportService.executeImport(
                    session.value!!, stream, duplicatePolicy, entityMappings, selectedDuplicates
                )
                if (result.isSuccess) {
                    _result.value = result.getOrNull()
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Import failed"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Import failed"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
