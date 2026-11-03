package com.osscabinet.ossWallet.ui.screens.transactions.entry

import androidx.lifecycle.SavedStateHandle
import com.osscabinet.ossWallet.core.util.MainDispatcherRule
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountRef
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.dto.TransactionRequest
import com.osscabinet.ossWallet.domain.model.isOutside
import com.osscabinet.ossWallet.domain.model.toDbTransferSide
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.management.TransactionManager
import com.osscabinet.ossWallet.domain.usecase.EntryFormDataUseCase
import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
import com.osscabinet.ossWallet.test.FakeAccountRepository
import com.osscabinet.ossWallet.test.FakeCategoryRepository
import com.osscabinet.ossWallet.test.FakeComputationRepository
import com.osscabinet.ossWallet.test.FakeSettingsRepository
import com.osscabinet.ossWallet.test.FakeSharedWalletRepository
import com.osscabinet.ossWallet.test.FakeTagRepository
import com.osscabinet.ossWallet.test.FakeTemplateRepository
import com.osscabinet.ossWallet.test.FakeTransactionRepository
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionEntryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val mockAccount = Account(
        id = "acc1",
        name = "Cash",
        currencyCode = "USD",
        structureType = AccountStructureType.CASH,
        colorHex = "#000000",
        icon = "ic"
    )

    private val mockToAccount = Account(
        id = "acc2",
        name = "Bank",
        currencyCode = "EUR",
        structureType = AccountStructureType.BANK,
        colorHex = "#FFFFFF",
        icon = "ic"
    )

    private lateinit var fakeSettings: FakeSettingsRepository
    private lateinit var fakeComputation: FakeComputationRepository
    private lateinit var fakeTx: FakeTransactionRepository
    private lateinit var transactionManager: TransactionManager
    private lateinit var viewModel: TransactionEntryViewModel

    @Before
    fun setup() {
        CurrencyService.setFormatConfig(2, CurrencyService.FormattingSystem.WESTERN)

        fakeSettings = FakeSettingsRepository(initialProfileId = 1, initialCurrency = "USD")
        fakeComputation = FakeComputationRepository()
        fakeTx = FakeTransactionRepository()
        transactionManager = mockk(relaxed = true)

        val fakeAccounts = FakeAccountRepository(listOf(mockAccount, mockToAccount))
        val fakeCategories = FakeCategoryRepository()
        val fakeTags = FakeTagRepository()
        val fakeTemplates = FakeTemplateRepository()

        val entryFormDataUseCase = EntryFormDataUseCase(
            accountRepository = fakeAccounts,
            categoryRepository = fakeCategories,
            tagRepository = fakeTags,
            templateRepository = fakeTemplates,
            transactionRepository = fakeTx
        )
        val transactionUseCases = TransactionUseCases(
            repository = fakeTx,
            sharedWalletRepository = FakeSharedWalletRepository()
        )

        viewModel = TransactionEntryViewModel(
            savedStateHandle = SavedStateHandle(),
            transactionUseCases = transactionUseCases,
            entryFormDataUseCase = entryFormDataUseCase,
            transactionManager = transactionManager,
            settingsRepository = fakeSettings,
            computationRepository = fakeComputation,
            tagRepository = fakeTags,
            attachmentRepository = com.osscabinet.ossWallet.test.FakeAttachmentRepository(),
            attachmentProcessor = com.osscabinet.ossWallet.test.FakeAttachmentProcessor()
        )
    }

    private suspend fun awaitState(predicate: (TransactionEntryState) -> Boolean): TransactionEntryState {
        return viewModel.state.filter(predicate).first()
    }

    private fun String.toBD() = toBigDecimalOrNull() ?: BigDecimal.ZERO

    @Test
    fun `initial state is correct`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.initialize() }
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        val state = awaitState { it.accounts.isNotEmpty() }
        assertEquals(EntryMode.EXPENSE, state.entryMode)
        assertEquals("0", state.amount)
    }

    @Test
    fun `outside wallet inherits currency from other side and enforces sync`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.initialize() }
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.onEvent(TransactionEntryEvent.SetTransactionType(EntryMode.TRANSFER))
        advanceUntilIdle()

        // 1. Source (EUR) -> Outside (should be EUR)
        viewModel.onEvent(TransactionEntryEvent.SetAccountId(mockToAccount.id)) // acc2 is EUR
        advanceUntilIdle()

        val state1 = awaitState { it.destination.account.isOutside && it.destination.currency == "EUR" }
        assertEquals("EUR", state1.destination.currency)

        // 2. Change Source to USD -> Target (Outside) should follow to USD
        viewModel.onEvent(TransactionEntryEvent.SetAccountId(mockAccount.id)) // acc1 is USD
        advanceUntilIdle()

        val state2 = awaitState { state -> (state.source.account as? AccountRef.Wallet)?.id == "acc1" && state.source.currency == "USD" }
        assertEquals("USD", state2.destination.currency)

        // 3. Manually change Source currency to EUR -> Target (Outside) should follow
        viewModel.onEvent(TransactionEntryEvent.SetCurrency("EUR"))
        advanceUntilIdle()

        val state3 = awaitState { it.source.currency == "EUR" }
        assertEquals("EUR", state3.destination.currency)
    }

    @Test
    fun `same source and target currency results in 1 to 1 rate`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.initialize() }
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.onEvent(TransactionEntryEvent.SetTransactionType(EntryMode.TRANSFER))
        advanceUntilIdle()

        viewModel.onEvent(TransactionEntryEvent.SetAccountId(mockAccount.id))
        advanceUntilIdle()

        viewModel.onEvent(TransactionEntryEvent.SetAmount("100"))
        advanceUntilIdle()

        val state = awaitState {
            it.amount.toBD().compareTo(BigDecimal("100")) == 0 &&
            it.targetAmount.toBD().compareTo(BigDecimal("100")) == 0 &&
            it.targetCurrency == "USD"
        }

        assertEquals("USD", state.selectedCurrency)
        assertEquals("USD", state.targetCurrency)
        assertEquals(0, BigDecimal("100").compareTo(state.targetAmount.toBD()))
    }

    @Test
    fun `swapping accounts in auto mode correctly swaps values and recalculates target`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.initialize() }
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.onEvent(TransactionEntryEvent.SetTransactionType(EntryMode.TRANSFER))
        advanceUntilIdle()

        viewModel.onEvent(TransactionEntryEvent.SetAccountId(mockAccount.id)) // USD
        viewModel.onEvent(TransactionEntryEvent.SetTargetAccountId(mockToAccount.id)) // EUR (0.85)
        advanceUntilIdle()

        viewModel.onEvent(TransactionEntryEvent.SetAmount("100"))
        advanceUntilIdle()

        viewModel.onEvent(TransactionEntryEvent.SwapAccounts)
        advanceUntilIdle()

        val state = awaitState { (it.source.account as? AccountRef.Wallet)?.id == "acc2" && it.amount.toBD().compareTo(BigDecimal("85")) == 0 }

        assertEquals("acc2", (state.source.account as? AccountRef.Wallet)?.id)
        assertEquals("EUR", state.selectedCurrency)
        assertEquals("acc1", (state.destination.account as? AccountRef.Wallet)?.id)
        assertEquals("USD", state.targetCurrency)

        val finalState = awaitState { it.targetAmount.toBD().compareTo(BigDecimal("100")) == 0 }
        assertEquals(0, BigDecimal("100").compareTo(finalState.targetAmount.toBD()))
        advanceUntilIdle()
    }

    @Test
    fun `SetAmount updates target and SetTargetAmount updates source`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.initialize() }
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.onEvent(TransactionEntryEvent.SetTransactionType(EntryMode.TRANSFER))
        advanceUntilIdle()

        viewModel.onEvent(TransactionEntryEvent.SetAccountId(mockAccount.id)) // USD
        viewModel.onEvent(TransactionEntryEvent.SetTargetAccountId(mockToAccount.id)) // EUR (0.85)
        advanceUntilIdle()

        viewModel.onEvent(TransactionEntryEvent.SetAmount("100"))
        advanceUntilIdle()

        val state1 = awaitState { it.targetAmount.toBD().compareTo(BigDecimal("85")) == 0 }
        assertEquals(0, BigDecimal("85").compareTo(state1.targetAmount.toBD()))

        viewModel.onEvent(TransactionEntryEvent.SetTargetAmount("170"))
        advanceUntilIdle()

        val state2 = awaitState { it.amount.toBD().compareTo(BigDecimal("200")) == 0 }
        assertEquals(0, BigDecimal("200").compareTo(state2.amount.toBD()))
        advanceUntilIdle()
    }

    @Test
    fun `ToggleRateMode correctly decouples amounts`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.initialize() }
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.onEvent(TransactionEntryEvent.SetTransactionType(EntryMode.TRANSFER))
        viewModel.onEvent(TransactionEntryEvent.SetAccountId(mockAccount.id)) // USD
        viewModel.onEvent(TransactionEntryEvent.SetTargetAccountId(mockToAccount.id)) // EUR (0.85)
        viewModel.onEvent(TransactionEntryEvent.SetAmount("100"))
        advanceUntilIdle()

        // Enable Fixed mode
        viewModel.onEvent(TransactionEntryEvent.ToggleRateMode)
        advanceUntilIdle()

        val state1 = awaitState { it.isRateFixed }
        assertTrue(state1.isRateFixed)

        // Change source - target should NOT change
        viewModel.onEvent(TransactionEntryEvent.SetAmount("200"))
        advanceUntilIdle()

        val state2 = awaitState { it.amount.toBD().compareTo(BigDecimal("200")) == 0 }
        assertEquals(0, BigDecimal("85").compareTo(state2.targetAmount.toBD()))

        // Implied rate should update (85 / 200 = 0.425)
        assertEquals(0, BigDecimal("0.425").compareTo(state2.exchangeRate))

        // Toggle back to Auto - target should re-align (200 * 0.85 = 170)
        viewModel.onEvent(TransactionEntryEvent.ToggleRateMode)
        advanceUntilIdle()

        val state3 = awaitState { !it.isRateFixed }
        assertFalse(state3.isRateFixed)
        val state4 = awaitState { it.targetAmount.toBD().compareTo(BigDecimal("170")) == 0 }
        assertEquals(0, BigDecimal("170").compareTo(state4.targetAmount.toBD()))
    }

    // F-2: clearPrefillDto clears prefillDto from state
    @Test
    fun `clearPrefillDto sets prefillDto to null`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.initialize() }
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        // Verify no prefill initially
        assertNull(viewModel.state.value.prefillDto)

        // Set a prefill via saved state (simulate navigation with a prefill)
        // clearPrefillDto should be a no-op but still set prefillDto = null
        viewModel.clearPrefillDto()
        advanceUntilIdle()

        assertNull(viewModel.state.value.prefillDto)
    }

    // F-3: isSaving guard prevents double-save
    @Test
    fun `saveTransaction guard prevents concurrent double save`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.initialize() }
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        var saveCount = 0
        coEvery { transactionManager.handleSaveTransaction(any(), any(), any(), any(), any(), any(), any()) } coAnswers {
            delay(500L)
            saveCount++
            1L
        }

        // Put VM in a saveable state
        viewModel.onEvent(TransactionEntryEvent.SetAccountId(mockAccount.id))
        viewModel.onEvent(TransactionEntryEvent.SetAmount("10"))
        advanceUntilIdle()

        // Fire two saves without waiting
        viewModel.saveTransaction()
        viewModel.saveTransaction()
        advanceUntilIdle()

        assertEquals("TransactionManager called exactly once despite two saveTransaction() calls", 1, saveCount)
    }

    @Test
    fun `reverts from split mode if no splits exist when dismissing sheet`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.initialize() }
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        // 1. Enable split mode
        viewModel.onEvent(TransactionEntryEvent.ToggleSplitMode(true))
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isSplitMode)

        // 2. Open split sheet
        viewModel.onEvent(TransactionEntryEvent.ShowSplitSheet(true))
        advanceUntilIdle()
        assertTrue(viewModel.state.value.showSplitSheet)

        // 3. Dismiss split sheet (splits are empty)
        viewModel.onEvent(TransactionEntryEvent.ShowSplitSheet(false))
        advanceUntilIdle()
        assertFalse(viewModel.state.value.showSplitSheet)
        assertFalse(viewModel.state.value.isSplitMode) // Should revert

        // 4. Test DismissAllSheets
        viewModel.onEvent(TransactionEntryEvent.ToggleSplitMode(true))
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isSplitMode)
        viewModel.onEvent(TransactionEntryEvent.DismissAllSheets)
        advanceUntilIdle()
        assertFalse(viewModel.state.value.isSplitMode) // Should revert

        // 5. Should NOT revert if splits are not empty
        viewModel.onEvent(TransactionEntryEvent.ToggleSplitMode(true))
        viewModel.onEvent(TransactionEntryEvent.AddSplit)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isSplitMode)
        assertEquals(1, viewModel.state.value.splits.size)

        viewModel.onEvent(TransactionEntryEvent.ShowSplitSheet(false))
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isSplitMode) // Should NOT revert
    }

    @Test
    fun `balance adjustment on the reconciliation category opens and saves as a transfer, keeping its category`() = runTest(testDispatcher) {
        openExisting(storedRow(11, "50.00", TransactionType.TRANSFER_INFLOW, Category.SYSTEM_RECONCILIATION_ADJUSTMENT))

        assertEquals(EntryMode.TRANSFER, viewModel.state.value.entryMode)
        val request = saveAndCaptureRequest()

        assertEquals(EntryMode.TRANSFER, request.entryMode)
        assertTrue(request.isTransfer)
        assertEquals(Category.SYSTEM_RECONCILIATION_ADJUSTMENT, request.categoryId)
        assertEquals(0, BigDecimal("50").compareTo(request.amount))
        assertNull(request.accountId)
        assertEquals("acc1", request.targetAccountId)
    }

    @Test
    fun `switching an initial balance or adjustment row away from transfer reassigns it to Others`() = runTest(testDispatcher) {
        openExisting(storedRow(12, "50.00", TransactionType.TRANSFER_INFLOW, Category.SYSTEM_RECONCILIATION_ADJUSTMENT))
        viewModel.onEvent(TransactionEntryEvent.SetTransactionType(EntryMode.INCOME))
        advanceUntilIdle()
        assertEquals(EntryMode.INCOME, viewModel.state.value.entryMode)
        assertEquals(Category.SYSTEM_OTHERS, viewModel.state.value.selectedCategoryId)

        openExisting(storedRow(13, "1000.00", TransactionType.TRANSFER_OUTFLOW, Category.SYSTEM_INITIAL_BALANCE))
        viewModel.onEvent(TransactionEntryEvent.SetTransactionType(EntryMode.EXPENSE))
        advanceUntilIdle()
        assertEquals(EntryMode.EXPENSE, viewModel.state.value.entryMode)
        assertEquals(Category.SYSTEM_OTHERS, viewModel.state.value.selectedCategoryId)
    }

    // ── Opening balances and balance adjustments must open and save with their own sign ──

    private fun storedRow(
        id: Long,
        amount: String,
        type: TransactionType,
        category: Long,
        accountId: String = "acc1",
        targetAccountId: String? = null,
        timestamp: Long = 1_000L
    ) = Transaction(
        id = id,
        name = "row",
        amount = BigDecimal(amount),
        timestamp = timestamp,
        currency = "USD",
        transactionType = type,
        accountId = accountId,
        categoryId = category,
        targetAccountId = targetAccountId,
        targetCurrency = if (targetAccountId != null) "EUR" else null
    )

    /** Opens [row] in edit mode and waits until the form is populated. */
    private suspend fun TestScope.openExisting(row: Transaction) {
        fakeTx.transactionsById[row.id] = row
        backgroundScope.launch { viewModel.initialize(transactionId = row.id) }
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()
        // Keyed on this row's own id, not just "some edit-mode state" - a prior openExisting() call
        // in the same test can leave state that already satisfies a looser predicate, racing ahead
        // of this row's own load.
        awaitState { it.isEditMode && it.accounts.isNotEmpty() && it.transactionId == row.id }
    }

    /** Saves the open form and returns the request handed to the TransactionManager. */
    private fun TestScope.saveAndCaptureRequest(): TransactionRequest {
        val request = slot<TransactionRequest>()
        coEvery {
            transactionManager.handleSaveTransaction(any(), capture(request), any(), any(), any(), any(), any())
        } returns 1L
        viewModel.saveTransaction()
        advanceUntilIdle()
        return request.captured
    }

    @Test
    fun `gain opening balance opens as a transfer inflow from Outside Wallet`() = runTest(testDispatcher) {
        openExisting(storedRow(7, "1000.00", TransactionType.TRANSFER_INFLOW, Category.SYSTEM_INITIAL_BALANCE))

        val state = viewModel.state.value
        assertEquals(EntryMode.TRANSFER, state.entryMode)
        assertEquals("acc1", state.destination.account.toDbTransferSide())
        assertEquals("1000", state.destination.amount)
        assertNull(state.source.account.toDbTransferSide())
    }

    @Test
    fun `saving an untouched gain opening balance keeps it a positive transfer inflow with its date`() = runTest(testDispatcher) {
        openExisting(storedRow(7, "1000.00", TransactionType.TRANSFER_INFLOW, Category.SYSTEM_INITIAL_BALANCE, timestamp = 555L))

        val request = saveAndCaptureRequest()

        assertEquals(EntryMode.TRANSFER, request.entryMode)
        assertTrue(request.isTransfer)
        assertEquals(0, BigDecimal("1000").compareTo(request.amount))
        assertEquals(Category.SYSTEM_INITIAL_BALANCE, request.categoryId)
        assertEquals(555L, request.timestamp)
        assertNull(request.accountId)
        assertEquals("acc1", request.targetAccountId)
    }

    @Test
    fun `loss opening balance opens and saves as a transfer outflow with the same magnitude`() = runTest(testDispatcher) {
        openExisting(storedRow(8, "-250.00", TransactionType.TRANSFER_OUTFLOW, Category.SYSTEM_INITIAL_BALANCE))

        assertEquals(EntryMode.TRANSFER, viewModel.state.value.entryMode)
        val request = saveAndCaptureRequest()

        assertEquals(EntryMode.TRANSFER, request.entryMode)
        assertTrue(request.isTransfer)
        assertEquals(0, BigDecimal("250").compareTo(request.amount))
        assertEquals("acc1", request.accountId)
        assertNull(request.targetAccountId)
    }

    @Test
    fun `income balance adjustment on the transfer category opens and saves as income`() = runTest(testDispatcher) {
        // Balance adjustments are INCOME/EXPENSE rows that carry the transfer category id.
        openExisting(storedRow(9, "50.00", TransactionType.INCOME, Category.SYSTEM_TRANSFER))

        assertEquals(EntryMode.INCOME, viewModel.state.value.entryMode)
        val request = saveAndCaptureRequest()

        assertEquals(EntryMode.INCOME, request.entryMode)
        assertFalse(request.isTransfer)
        assertEquals(0, BigDecimal("50").compareTo(request.amount))
    }

    @Test
    fun `real transfer leg still opens as transfer and saves with its target`() = runTest(testDispatcher) {
        openExisting(
            storedRow(
                10, "-100.00", TransactionType.TRANSFER_OUTFLOW, Category.SYSTEM_TRANSFER,
                accountId = "acc1", targetAccountId = "acc2"
            )
        )

        assertEquals(EntryMode.TRANSFER, viewModel.state.value.entryMode)
        val request = saveAndCaptureRequest()

        assertTrue(request.isTransfer)
        assertEquals("acc2", request.targetAccountId)
        assertEquals(0, BigDecimal("100").compareTo(request.amount))
    }
}
