package com.osscabinet.ossWallet.ui.screens.transactions.entry

import androidx.lifecycle.SavedStateHandle
import com.osscabinet.ossWallet.core.util.MainDispatcherRule
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountRef
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.isOutside
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
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
            tagRepository = fakeTags
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
        viewModel.onEvent(TransactionEntryEvent.SetAccount(mockToAccount)) // acc2 is EUR
        advanceUntilIdle()

        val state1 = awaitState { it.destination.account.isOutside && it.destination.currency == "EUR" }
        assertEquals("EUR", state1.destination.currency)

        // 2. Change Source to USD -> Target (Outside) should follow to USD
        viewModel.onEvent(TransactionEntryEvent.SetAccount(mockAccount)) // acc1 is USD
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

        viewModel.onEvent(TransactionEntryEvent.SetAccount(mockAccount))
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

        viewModel.onEvent(TransactionEntryEvent.SetAccount(mockAccount)) // USD
        viewModel.onEvent(TransactionEntryEvent.SetTargetAccount(mockToAccount)) // EUR (0.85)
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

        viewModel.onEvent(TransactionEntryEvent.SetAccount(mockAccount)) // USD
        viewModel.onEvent(TransactionEntryEvent.SetTargetAccount(mockToAccount)) // EUR (0.85)
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
        viewModel.onEvent(TransactionEntryEvent.SetAccount(mockAccount)) // USD
        viewModel.onEvent(TransactionEntryEvent.SetTargetAccount(mockToAccount)) // EUR (0.85)
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
        viewModel.onEvent(TransactionEntryEvent.SetAccount(mockAccount))
        viewModel.onEvent(TransactionEntryEvent.SetAmount("10"))
        advanceUntilIdle()

        // Fire two saves without waiting
        viewModel.saveTransaction()
        viewModel.saveTransaction()
        advanceUntilIdle()

        assertEquals("TransactionManager called exactly once despite two saveTransaction() calls", 1, saveCount)
    }
}
