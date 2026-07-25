package com.osscabinet.ossWallet.ui.screens.import_csv

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.osscabinet.ossWallet.core.util.MainDispatcherRule
import com.osscabinet.ossWallet.domain.model.import_csv.AccountToMap
import com.osscabinet.ossWallet.domain.model.import_csv.ImportPreview
import com.osscabinet.ossWallet.domain.model.import_csv.ImportSummary
import com.osscabinet.ossWallet.test.FakeAccountRepository
import com.osscabinet.ossWallet.test.FakeCategoryRepository
import com.osscabinet.ossWallet.test.FakeCsvImportService
import com.osscabinet.ossWallet.test.FakeImportBatchRepository
import com.osscabinet.ossWallet.test.FakeSettingsRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayInputStream

@OptIn(ExperimentalCoroutinesApi::class)
class CsvImportViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var csvImportService: FakeCsvImportService
    private lateinit var settingsRepository: FakeSettingsRepository
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var importBatchRepository: FakeImportBatchRepository
    private lateinit var viewModel: CsvImportViewModel
    private lateinit var context: Context

    private val samplePreview = ImportPreview(
        totalRows = 3,
        validRows = 3,
        duplicates = emptyList(),
        allCsvAccounts = listOf(
            AccountToMap(
                name = "HDFC Bank",
                suggestedExistingId = "acc-1",
                suggestedExistingName = "HDFC Bank",
                rowCount = 2,
                hasDetectedCurrency = true
            ),
            AccountToMap(
                name = "Cash",
                suggestedExistingId = null,
                suggestedExistingName = null,
                rowCount = 1,
                hasDetectedCurrency = false
            )
        ),
        newCategories = emptyList(),
        parseErrors = emptyList()
    )

    @Before
    fun setup() {
        csvImportService = FakeCsvImportService(previewToReturn = samplePreview)
        settingsRepository = FakeSettingsRepository()
        accountRepository = FakeAccountRepository()
        categoryRepository = FakeCategoryRepository()
        importBatchRepository = FakeImportBatchRepository()

        val contentResolver = mockk<ContentResolver>()
        every { contentResolver.openInputStream(any()) } returns ByteArrayInputStream(ByteArray(0))
        context = mockk(relaxed = true)
        every { context.contentResolver } returns contentResolver

        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns mockk<Uri>()

        viewModel = CsvImportViewModel(
            csvImportService,
            settingsRepository,
            accountRepository,
            categoryRepository,
            importBatchRepository
        )
    }

    @After
    fun tearDown() {
        unmockkStatic(Uri::class)
    }

    private fun runViewModelTest(block: suspend TestScope.() -> Unit) =
        runTest(mainDispatcherRule.testDispatcher) { block() }

    private suspend fun TestScope.loadFileAndGeneratePreview() {
        viewModel.loadFile("content://fake.csv", context)
        advanceUntilIdle()
        viewModel.proceedFromColumnMapping(context)
        advanceUntilIdle()
    }

    @Test
    fun `updateEntitySearchQuery updates the search query state`() = runViewModelTest {
        viewModel.updateEntitySearchQuery("hdfc")

        assertEquals("hdfc", viewModel.entitySearchQuery.value)
    }

    @Test
    fun `selectAllNewAccounts forces every csv account to create-new`() = runViewModelTest {
        loadFileAndGeneratePreview()

        // Sanity check: HDFC Bank starts out matched to an existing account by suggestion.
        assertEquals("acc-1", viewModel.accountMappings.value["HDFC Bank"]?.existingId)

        viewModel.selectAllNewAccounts()

        val mappings = viewModel.accountMappings.value
        assertEquals(2, mappings.size)
        assertNull(mappings["HDFC Bank"]?.existingId)
        assertNull(mappings["Cash"]?.existingId)
    }

    @Test
    fun `selectAllNewAccounts preserves a previously configured mapping's other fields`() = runViewModelTest {
        loadFileAndGeneratePreview()
        viewModel.updateAccountMapping(
            "Cash",
            viewModel.accountMappings.value.getValue("Cash").copy(currencyCode = "EUR")
        )

        viewModel.selectAllNewAccounts()

        val cashMapping = viewModel.accountMappings.value.getValue("Cash")
        assertNull(cashMapping.existingId)
        assertEquals("EUR", cashMapping.currencyCode)
    }

    @Test
    fun `undoImport is a no-op when nothing has been imported yet`() = runViewModelTest {
        viewModel.undoImport()
        advanceUntilIdle()

        assertFalse(viewModel.undoCompleted.value)
        assertTrue(importBatchRepository.undoCalls.isEmpty())
    }

    @Test
    fun `undoImport calls the repository with the completed import's batch id and marks undo completed`() =
        runViewModelTest {
            loadFileAndGeneratePreview()
            csvImportService.executeImportResult = Result.success(
                ImportSummary(imported = 3, skipped = 0, errors = emptyList(), batchId = "batch-123")
            )

            viewModel.executeImport(context)
            advanceUntilIdle()
            assertEquals("batch-123", viewModel.result.value?.batchId)

            importBatchRepository.undoResult = true
            viewModel.undoImport()
            advanceUntilIdle()

            assertEquals(listOf("batch-123"), importBatchRepository.undoCalls)
            assertTrue(viewModel.undoCompleted.value)
        }

    @Test
    fun `undoImport does not mark completed when the repository reports failure`() = runViewModelTest {
        loadFileAndGeneratePreview()
        csvImportService.executeImportResult = Result.success(
            ImportSummary(imported = 1, skipped = 0, errors = emptyList(), batchId = "batch-456")
        )
        viewModel.executeImport(context)
        advanceUntilIdle()

        importBatchRepository.undoResult = false
        viewModel.undoImport()
        advanceUntilIdle()

        assertEquals(listOf("batch-456"), importBatchRepository.undoCalls)
        assertFalse(viewModel.undoCompleted.value)
    }

    @Test
    fun `undoImport is a no-op when the completed import has a blank batch id`() = runViewModelTest {
        loadFileAndGeneratePreview()
        csvImportService.executeImportResult = Result.success(
            ImportSummary(imported = 1, skipped = 0, errors = emptyList(), batchId = "")
        )
        viewModel.executeImport(context)
        advanceUntilIdle()

        viewModel.undoImport()
        advanceUntilIdle()

        assertTrue(importBatchRepository.undoCalls.isEmpty())
        assertFalse(viewModel.undoCompleted.value)
    }
}
